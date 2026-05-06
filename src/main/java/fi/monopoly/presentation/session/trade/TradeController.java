package fi.monopoly.presentation.session.trade;

import fi.monopoly.application.command.*;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerDecision;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.computer.StrongBotConfig;
import fi.monopoly.components.trade.*;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.TradeOfferState;
import fi.monopoly.domain.session.TradeSelectionState;
import fi.monopoly.domain.session.TradeState;
import fi.monopoly.domain.session.TradeStatus;
import fi.monopoly.presentation.legacy.session.trade.LegacyTradeGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
/**
 * Desktop-side trade interaction coordinator for the authoritative session model.
 *
 * <p>The controller translates menu opens, popup actions, and bot trade responses into
 * session commands while reusing legacy trade evaluation helpers until that logic is
 * migrated behind pure application/domain ports.</p>
 */
@RequiredArgsConstructor
public final class TradeController {
    private static final StrongBotConfig STRONG_CONFIG = StrongBotConfig.defaults();

    private final MonopolyRuntime runtime;
    private final String sessionId;
    private final SessionCommandPort sessionApplicationService;
    private final TradeViewAdapter tradeViewAdapter;
    private final LegacyTradeGateway legacyTradeGateway;
    private final BooleanSupplier canOpenTrade;
    private final Supplier<List<Player>> playersSupplier;
    private final TradeOfferEvaluator tradeOfferEvaluator = new TradeOfferEvaluator();
    private final TradeUiBuilder tradeUiBuilder = new TradeUiBuilder(tradeOfferEvaluator);
    private final StrongTradePlanner strongTradePlanner = new StrongTradePlanner(STRONG_CONFIG);
    private String lastProactiveTradePlayerId;


    public void sync() {
        tradeViewAdapter.sync();
    }

    public void openTradeMenu() {
        if (!canOpenTrade.getAsBoolean() || sessionApplicationService.currentState().tradeState() != null) {
            return;
        }
        Player proposer = currentTurnPlayer();
        if (proposer == null) {
            return;
        }
        List<Player> tradePartners = playersSupplier.get().stream()
                .filter(player -> player != proposer)
                .sorted(Comparator.comparingInt(Player::getTurnNumber))
                .toList();
        if (tradePartners.isEmpty()) {
            runtime.popupService().show(text("trade.noPartners"));
            return;
        }
        runtime.popupService().showTrade(
                text("trade.choosePartner", proposer.getName()),
                tradeUiBuilder.buildPartnerSelectionView(
                        proposer,
                        tradePartners,
                        player -> () -> openTrade(playerId(proposer), playerId(player))
                )
        );
    }

    public ComputerDecision tryInitiateComputerTrade(String proposerId, ComputerPlayerProfile proposerProfile) {
        if (!canOpenTrade.getAsBoolean() || sessionApplicationService.currentState().tradeState() != null
                || proposerId == null || !proposerProfile.isComputerControlled()) {
            return null;
        }
        Player currentPlayer = currentTurnPlayer();
        if (currentPlayer == null || !proposerId.equals(playerId(currentPlayer))
                || proposerId.equals(lastProactiveTradePlayerId)) {
            return null;
        }
        lastProactiveTradePlayerId = proposerId;
        if (proposerProfile != ComputerPlayerProfile.STRONG) {
            return null;
        }
        Player proposerPlayer = findPlayerByDomainId(proposerId);
        if (proposerPlayer == null) return null;
        StrongTradePlanner.TradePlan plan = strongTradePlanner.plan(proposerPlayer, playersSupplier.get());
        if (plan == null) {
            return null;
        }
        String planProposerId = playerId(plan.offer().proposer());
        String planRecipientId = playerId(plan.offer().recipient());
        if (!openTrade(planProposerId, planRecipientId)) {
            return null;
        }
        TradeState tradeState = sessionApplicationService.currentState().tradeState();
        if (tradeState == null) {
            return null;
        }
        replaceOffer(tradeState, legacyTradeGateway.toState(plan.offer()), true);
        handleResult(sessionApplicationService.handle(new SubmitTradeOfferCommand(sessionId, planProposerId, tradeState.tradeId())));
        Player recipient = plan.offer().recipient();
        handleComputerTradeTurn(planRecipientId, recipient.getComputerProfile());
        return plan.decision();
    }

    public boolean handleComputerTradeTurn(String actorId, ComputerPlayerProfile actorProfile) {
        TradeState tradeState = sessionApplicationService.currentState().tradeState();
        if (tradeState == null || actorId == null || !actorProfile.isComputerControlled()) {
            return false;
        }
        if (tradeState.status() == TradeStatus.EDITING && actorId.equals(tradeState.editingPlayerId())) {
            return handleResult(sessionApplicationService.handle(new SubmitTradeOfferCommand(sessionId, actorId, tradeState.tradeId()))).accepted();
        }
        if ((tradeState.status() != TradeStatus.SUBMITTED && tradeState.status() != TradeStatus.COUNTERED)
                || !actorId.equals(tradeState.decisionRequiredFromPlayerId())) {
            return false;
        }
        BotTradeProfile tradeProfile = resolveTradeProfile(actorProfile);
        StrongBotConfig strongConfig = resolveStrongTradeConfig(actorProfile);
        TradeDecision decision = tradeOfferEvaluator.evaluateForRecipient(
                legacyTradeGateway.toLegacyOffer(tradeState.currentOffer()),
                tradeProfile,
                strongConfig
        );
        log.info("Bot trade decision: player={}, accept={}, score={}, reason={}",
                actorId,
                decision.accept(),
                Math.round(decision.score() * 10.0) / 10.0,
                decision.reason());
        if (decision.accept()) {
            return handleResult(sessionApplicationService.handle(new AcceptTradeCommand(sessionId, actorId, tradeState.tradeId()))).accepted();
        }
        TradeOfferState counterOffer = legacyTradeGateway.proposeCounterOffer(tradeState.currentOffer(), tradeProfile, strongConfig);
        if (counterOffer == null) {
            return handleResult(sessionApplicationService.handle(new DeclineTradeCommand(sessionId, actorId, tradeState.tradeId()))).accepted();
        }
        replaceOffer(tradeState, counterOffer, false);
        return handleResult(sessionApplicationService.handle(new CounterTradeCommand(sessionId, actorId, tradeState.tradeId()))).accepted();
    }

    private boolean openTrade(String proposerId, String recipientId) {
        CommandResult result = sessionApplicationService.handle(new OpenTradeCommand(
                sessionId, proposerId, recipientId));
        handleResult(result);
        return result.accepted();
    }

    private void replaceOffer(TradeState tradeState, TradeOfferState targetState, boolean nextEditingOfferedSide) {
        TradeOfferState currentState = sessionApplicationService.currentState().tradeState().currentOffer();
        if (!currentState.proposerPlayerId().equals(targetState.proposerPlayerId())) {
            handleResult(sessionApplicationService.handle(new EditTradeOfferCommand(
                    sessionId,
                    tradeState.editingPlayerId(),
                    tradeState.tradeId(),
                    new fi.monopoly.domain.session.TradeEditPatch(true, true, null, List.of(), List.of(), null)
            )));
        }
        currentState = sessionApplicationService.currentState().tradeState().currentOffer();
        applySelectionReplace(tradeState, true, currentState.offeredToRecipient(), targetState.offeredToRecipient());
        applySelectionReplace(tradeState, false, currentState.requestedFromRecipient(), targetState.requestedFromRecipient());
        if (sessionApplicationService.currentState().tradeState() != null
                && sessionApplicationService.currentState().tradeState().editingOfferedSide() != nextEditingOfferedSide) {
            handleResult(sessionApplicationService.handle(new EditTradeOfferCommand(
                    sessionId,
                    tradeState.editingPlayerId(),
                    tradeState.tradeId(),
                    new fi.monopoly.domain.session.TradeEditPatch(null, nextEditingOfferedSide, null, List.of(), List.of(), null)
            )));
        }
    }

    private void applySelectionReplace(
            TradeState tradeState,
            boolean offeredSide,
            TradeSelectionState currentSelection,
            TradeSelectionState targetSelection
    ) {
        handleResult(sessionApplicationService.handle(new EditTradeOfferCommand(
                sessionId,
                tradeState.editingPlayerId(),
                tradeState.tradeId(),
                new fi.monopoly.domain.session.TradeEditPatch(
                        null,
                        offeredSide,
                        targetSelection.moneyAmount(),
                        targetSelection.propertyIds().stream().filter(id -> !currentSelection.propertyIds().contains(id)).toList(),
                        currentSelection.propertyIds().stream().filter(id -> !targetSelection.propertyIds().contains(id)).toList(),
                        currentSelection.jailCardCount() != targetSelection.jailCardCount()
                )
        )));
    }

    private CommandResult handleResult(CommandResult result) {
        tradeViewAdapter.sync();
        if (!result.accepted() && !result.rejections().isEmpty()) {
            runtime.popupService().show(result.rejections().get(0).message());
        }
        return result;
    }

    private Player currentTurnPlayer() {
        SessionState state = sessionApplicationService.currentState();
        if (state == null || state.turn() == null) return null;
        return findPlayerByDomainId(state.turn().activePlayerId());
    }

    private Player findPlayerByDomainId(String domainId) {
        if (domainId == null) return null;
        return playersSupplier.get().stream()
                .filter(p -> domainId.equals("player-" + p.getId()))
                .findFirst().orElse(null);
    }

    private BotTradeProfile resolveTradeProfile(ComputerPlayerProfile profile) {
        return switch (profile) {
            case SMOKE_TEST -> BotTradeProfile.CAUTIOUS;
            case STRONG -> BotTradeProfile.BALANCED;
            case HUMAN -> BotTradeProfile.AGGRESSIVE;
        };
    }

    private StrongBotConfig resolveStrongTradeConfig(ComputerPlayerProfile profile) {
        return profile == ComputerPlayerProfile.STRONG ? STRONG_CONFIG : null;
    }

    private String playerId(Player player) {
        return player == null ? null : "player-" + player.getId();
    }
}
