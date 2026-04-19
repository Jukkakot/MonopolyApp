package fi.monopoly.presentation.session.trade;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.application.command.*;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerDecision;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.computer.StrongBotConfig;
import fi.monopoly.components.popup.ButtonAction;
import fi.monopoly.components.trade.BotTradeProfile;
import fi.monopoly.components.trade.StrongTradePlanner;
import fi.monopoly.components.trade.TradeDecision;
import fi.monopoly.components.trade.TradeOfferEvaluator;
import fi.monopoly.components.trade.TradeUiBuilder;
import fi.monopoly.domain.session.TradeOfferState;
import fi.monopoly.domain.session.TradeSelectionState;
import fi.monopoly.domain.session.TradeState;
import fi.monopoly.domain.session.TradeStatus;
import fi.monopoly.presentation.legacy.session.trade.LegacyTradeGateway;
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
public final class TradeController {
    private static final StrongBotConfig STRONG_CONFIG = StrongBotConfig.defaults();

    private final MonopolyRuntime runtime;
    private final String sessionId;
    private final SessionApplicationService sessionApplicationService;
    private final TradeViewAdapter tradeViewAdapter;
    private final LegacyTradeGateway legacyTradeGateway;
    private final BooleanSupplier canOpenTrade;
    private final Supplier<Player> currentPlayerSupplier;
    private final Supplier<List<Player>> playersSupplier;
    private final TradeOfferEvaluator tradeOfferEvaluator = new TradeOfferEvaluator();
    private final TradeUiBuilder tradeUiBuilder = new TradeUiBuilder(tradeOfferEvaluator);
    private final StrongTradePlanner strongTradePlanner = new StrongTradePlanner(STRONG_CONFIG);
    private Player lastProactiveTradePlayer;

    public TradeController(
            MonopolyRuntime runtime,
            String sessionId,
            SessionApplicationService sessionApplicationService,
            TradeViewAdapter tradeViewAdapter,
            LegacyTradeGateway legacyTradeGateway,
            BooleanSupplier canOpenTrade,
            Supplier<Player> currentPlayerSupplier,
            Supplier<List<Player>> playersSupplier
    ) {
        this.runtime = runtime;
        this.sessionId = sessionId;
        this.sessionApplicationService = sessionApplicationService;
        this.tradeViewAdapter = tradeViewAdapter;
        this.legacyTradeGateway = legacyTradeGateway;
        this.canOpenTrade = canOpenTrade;
        this.currentPlayerSupplier = currentPlayerSupplier;
        this.playersSupplier = playersSupplier;
    }

    public void sync() {
        tradeViewAdapter.sync();
    }

    public void openTradeMenu() {
        if (!canOpenTrade.getAsBoolean() || sessionApplicationService.hasActiveTrade()) {
            return;
        }
        Player proposer = currentPlayerSupplier.get();
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
                        player -> () -> openTrade(proposer, player)
                )
        );
    }

    public ComputerDecision tryInitiateComputerTrade(Player proposer) {
        if (!canOpenTrade.getAsBoolean() || sessionApplicationService.hasActiveTrade() || proposer == null || !proposer.isComputerControlled()) {
            return null;
        }
        if (currentPlayerSupplier.get() != proposer || lastProactiveTradePlayer == proposer) {
            return null;
        }
        lastProactiveTradePlayer = proposer;
        if (proposer.getComputerProfile() != ComputerPlayerProfile.STRONG) {
            return null;
        }
        StrongTradePlanner.TradePlan plan = strongTradePlanner.plan(proposer, playersSupplier.get());
        if (plan == null) {
            return null;
        }
        if (!openTrade(plan.offer().proposer(), plan.offer().recipient())) {
            return null;
        }
        TradeState tradeState = sessionApplicationService.currentState().tradeState();
        if (tradeState == null) {
            return null;
        }
        replaceOffer(tradeState, legacyTradeGateway.toState(plan.offer()), true);
        handleResult(sessionApplicationService.handle(new SubmitTradeOfferCommand(sessionId, playerId(plan.offer().proposer()), tradeState.tradeId())));
        handleComputerTradeTurn(plan.offer().recipient());
        return plan.decision();
    }

    public boolean handleComputerTradeTurn(Player actor) {
        TradeState tradeState = sessionApplicationService.currentState().tradeState();
        if (tradeState == null || actor == null || !actor.isComputerControlled()) {
            return false;
        }
        String actorId = playerId(actor);
        if (tradeState.status() == TradeStatus.EDITING && actorId.equals(tradeState.editingPlayerId())) {
            return handleResult(sessionApplicationService.handle(new SubmitTradeOfferCommand(sessionId, actorId, tradeState.tradeId()))).accepted();
        }
        if ((tradeState.status() != TradeStatus.SUBMITTED && tradeState.status() != TradeStatus.COUNTERED)
                || !actorId.equals(tradeState.decisionRequiredFromPlayerId())) {
            return false;
        }
        BotTradeProfile profile = resolveTradeProfile(actor);
        StrongBotConfig strongConfig = resolveStrongTradeConfig(actor);
        TradeDecision decision = tradeOfferEvaluator.evaluateForRecipient(
                legacyTradeGateway.toLegacyOffer(tradeState.currentOffer()),
                profile,
                strongConfig
        );
        log.info("Bot trade decision: player={}, accept={}, score={}, reason={}",
                actor.getName(),
                decision.accept(),
                Math.round(decision.score() * 10.0) / 10.0,
                decision.reason());
        if (decision.accept()) {
            return handleResult(sessionApplicationService.handle(new AcceptTradeCommand(sessionId, actorId, tradeState.tradeId()))).accepted();
        }
        TradeOfferState counterOffer = legacyTradeGateway.proposeCounterOffer(tradeState.currentOffer(), profile, strongConfig);
        if (counterOffer == null) {
            return handleResult(sessionApplicationService.handle(new DeclineTradeCommand(sessionId, actorId, tradeState.tradeId()))).accepted();
        }
        replaceOffer(tradeState, counterOffer, false);
        return handleResult(sessionApplicationService.handle(new CounterTradeCommand(sessionId, actorId, tradeState.tradeId()))).accepted();
    }

    private boolean openTrade(Player proposer, Player recipient) {
        CommandResult result = sessionApplicationService.handle(new OpenTradeCommand(
                sessionId,
                playerId(proposer),
                playerId(recipient)
        ));
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

    private BotTradeProfile resolveTradeProfile(Player player) {
        return switch (player.getComputerProfile()) {
            case SMOKE_TEST -> BotTradeProfile.CAUTIOUS;
            case STRONG -> BotTradeProfile.BALANCED;
            case HUMAN -> BotTradeProfile.AGGRESSIVE;
        };
    }

    private StrongBotConfig resolveStrongTradeConfig(Player player) {
        return player.getComputerProfile() == ComputerPlayerProfile.STRONG ? STRONG_CONFIG : null;
    }

    private String playerId(Player player) {
        return player == null ? null : "player-" + player.getId();
    }
}
