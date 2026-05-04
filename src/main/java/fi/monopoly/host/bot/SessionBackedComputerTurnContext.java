package fi.monopoly.host.bot;

import fi.monopoly.application.command.*;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerDecision;
import fi.monopoly.components.computer.ComputerTurnContext;
import fi.monopoly.components.computer.GameView;
import fi.monopoly.components.computer.PlayerView;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.types.SpotType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
public final class SessionBackedComputerTurnContext implements ComputerTurnContext {
    private final Player player;
    private final SessionCommandPort sessionCommandPort;
    private final HostBotInteractionAdapter interactionAdapter;
    private final Runnable syncPresentationState;
    private BotTurnScheduler.DelayKind delayKind = BotTurnScheduler.DelayKind.RESOLVE_POPUP;


    public BotTurnScheduler.DelayKind delayKind() {
        return delayKind;
    }

    @Override
    public GameView gameView() {
        return interactionAdapter.currentGameView(player);
    }

    @Override
    public PlayerView currentPlayerView() {
        return interactionAdapter.currentPlayerView(player);
    }

    @Override
    public SessionState sessionState() {
        return sessionCommandPort.currentState();
    }

    @Override
    public boolean submit(SessionCommand command) {
        var result = sessionCommandPort.handle(command);
        boolean accepted = result.accepted();
        if (!accepted) {
            SessionState state = sessionCommandPort.currentState();
            if (!result.rejections().isEmpty()) {
                log.debug("Rejected bot command {} for player {}: {}",
                        command.getClass().getSimpleName(),
                        player.getName(),
                        result.rejections().get(0).message());
            }
            log.debug("Bot command seam state: phase={}, pendingDecision={}, auctionState={}, tradeState={}, debtState={}, rollAvailable={}, endTurnAvailable={}",
                    state.turn().phase(),
                    state.pendingDecision() != null,
                    state.auctionState() != null,
                    state.tradeState() != null,
                    state.activeDebt() != null,
                    state.turn().canRoll(),
                    state.turn().canEndTurn());
            return false;
        }
        delayKind = delayKindForCommand(command);
        syncPresentationState.run();
        return true;
    }

    @Override
    public boolean resolveActivePopup() {
        boolean resolved = interactionAdapter.resolveVisiblePopupFor(player);
        if (resolved) {
            delayKind = BotTurnScheduler.DelayKind.RESOLVE_POPUP;
        }
        return resolved;
    }

    @Override
    public boolean acceptActivePopup() {
        boolean accepted = interactionAdapter.acceptActivePopup();
        if (accepted) {
            delayKind = BotTurnScheduler.DelayKind.ACCEPT_POPUP;
        }
        return accepted;
    }

    @Override
    public boolean declineActivePopup() {
        boolean declined = interactionAdapter.declineActivePopup();
        if (declined) {
            delayKind = BotTurnScheduler.DelayKind.DECLINE_POPUP;
        }
        return declined;
    }

    @Override
    public ComputerDecision initiateTrade() {
        ComputerDecision decision = interactionAdapter.tryInitiateComputerTrade(player);
        if (decision != null) {
            delayKind = BotTurnScheduler.DelayKind.TRADE;
        }
        return decision;
    }

    private BotTurnScheduler.DelayKind delayKindForCommand(SessionCommand command) {
        if (command instanceof BuyPropertyCommand) {
            return BotTurnScheduler.DelayKind.ACCEPT_POPUP;
        }
        if (command instanceof DeclinePropertyCommand) {
            return BotTurnScheduler.DelayKind.DECLINE_POPUP;
        }
        if (command instanceof PayDebtCommand) {
            return BotTurnScheduler.DelayKind.RETRY_DEBT_PAYMENT;
        }
        if (command instanceof MortgagePropertyForDebtCommand) {
            return BotTurnScheduler.DelayKind.MORTGAGE_PROPERTY;
        }
        if (command instanceof SellBuildingForDebtCommand || command instanceof SellBuildingRoundsAcrossSetForDebtCommand) {
            return BotTurnScheduler.DelayKind.SELL_BUILDING;
        }
        if (command instanceof DeclareBankruptcyCommand) {
            return BotTurnScheduler.DelayKind.DECLARE_BANKRUPTCY;
        }
        if (command instanceof BuyBuildingRoundCommand) {
            return BotTurnScheduler.DelayKind.BUILD_ROUND;
        }
        if (command instanceof ToggleMortgageCommand toggleMortgageCommand) {
            Property property = PropertyFactory.getProperty(SpotType.valueOf(toggleMortgageCommand.propertyId()));
            return property != null && property.isMortgaged()
                    ? BotTurnScheduler.DelayKind.UNMORTGAGE_PROPERTY
                    : BotTurnScheduler.DelayKind.MORTGAGE_PROPERTY;
        }
        if (command instanceof RollDiceCommand) {
            return BotTurnScheduler.DelayKind.ROLL_DICE;
        }
        if (command instanceof EndTurnCommand) {
            return BotTurnScheduler.DelayKind.END_TURN;
        }
        return BotTurnScheduler.DelayKind.RESOLVE_POPUP;
    }
}
