package fi.monopoly.presentation.game.desktop.assembly;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.computer.GameView;
import fi.monopoly.components.computer.PlayerView;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.turn.TurnEngine;
import fi.monopoly.host.bot.BotTurnScheduler;
import fi.monopoly.host.bot.GameBotTurnDriver;
import fi.monopoly.host.bot.GameBotTurnHooksAdapter;
import fi.monopoly.host.bot.HostBotInteractionAdapter;
import fi.monopoly.presentation.game.session.GameSessionQueries;
import fi.monopoly.presentation.game.turn.GameTurnFlowCoordinator;
import fi.monopoly.presentation.game.turn.GameTurnFlowHooksAdapter;
import fi.monopoly.presentation.game.desktop.runtime.DebugController;
import fi.monopoly.presentation.game.desktop.ui.GameControlLayout;
import fi.monopoly.presentation.game.desktop.ui.GamePrimaryTurnControls;
import fi.monopoly.presentation.game.desktop.ui.GamePresentationSupport;
import fi.monopoly.presentation.game.desktop.ui.GameUiController;
import fi.monopoly.presentation.game.desktop.ui.GameUiHooksAdapter;
import fi.monopoly.presentation.game.desktop.ui.GameUiSessionControls;
import fi.monopoly.presentation.session.auction.AuctionViewAdapter;
import fi.monopoly.presentation.session.debt.DebtActionDispatcher;
import fi.monopoly.presentation.session.debt.DebtController;
import fi.monopoly.presentation.session.purchase.PendingDecisionPopupAdapter;
import fi.monopoly.presentation.session.trade.TradeController;
import fi.monopoly.presentation.session.trade.TradeViewAdapter;
import fi.monopoly.utils.DebugPerformanceStats;

import java.util.List;
import java.util.Locale;

/**
 * Assembles desktop presentation coordinators around the current game runtime.
 */
public final class GamePresentationFactory {
    public GamePresentationBundle create(
            MonopolyRuntime runtime,
            Buttons buttons,
            Dependencies dependencies,
            GameUiSessionControls uiSessionControls,
            Hooks hooks
    ) {
        GameControlLayout gameControlLayout = new GameControlLayout(
                runtime,
                buttons.endRoundButton(),
                buttons.retryDebtButton(),
                buttons.declareBankruptcyButton(),
                buttons.debugGodModeButton(),
                buttons.pauseButton(),
                buttons.tradeButton(),
                buttons.saveButton(),
                buttons.loadButton(),
                buttons.botSpeedButton(),
                buttons.languageButton(),
                dependencies.dices()
        );
        GamePrimaryTurnControls gamePrimaryTurnControls = new GamePrimaryTurnControls(
                dependencies.dices(),
                buttons.endRoundButton(),
                hooks::gameOver,
                hooks::currentTurnPlayer
        );
        GameSessionQueries gameSessionQueries = new GameSessionQueries(dependencies.players(), dependencies.board());
        GameTurnFlowCoordinator gameTurnFlowCoordinator = new GameTurnFlowCoordinator(
                runtime,
                dependencies.players(),
                dependencies.dices(),
                dependencies.board(),
                dependencies.animations(),
                dependencies.turnEngine(),
                dependencies.propertyPurchaseFlow(),
                hooks::goMoneyAmount,
                new GameTurnFlowHooksAdapter(
                        hooks::updateLogTurnContext,
                        hooks::hidePrimaryTurnControls,
                        hooks::showRollDiceControl,
                        hooks::showEndTurnControl,
                        hooks::gameOver,
                        hooks::debtActive,
                        hooks::handlePaymentRequest
                )
        );
        dependencies.sessionApplicationService().configureTurnContinuationFlow(
                continuationState -> gameTurnFlowCoordinator.resumeContinuation(continuationState)
        );
        GameUiController gameUiController = new GameUiController(
                buttons.endRoundButton(),
                buttons.retryDebtButton(),
                buttons.declareBankruptcyButton(),
                buttons.debugGodModeButton(),
                buttons.pauseButton(),
                buttons.tradeButton(),
                buttons.saveButton(),
                buttons.loadButton(),
                buttons.botSpeedButton(),
                buttons.languageButton(),
                uiSessionControls,
                new GameUiHooksAdapter(
                        dependencies.board(),
                        dependencies.dices(),
                        dependencies.debtController(),
                        dependencies.tradeController(),
                        dependencies.debugController(),
                        gameTurnFlowCoordinator,
                        dependencies.debtActionDispatcher()::payDebt,
                        dependencies.debtActionDispatcher()::declareBankruptcy,
                        dependencies.animations()::finishAllAnimations,
                        hooks::gameOver,
                        hooks::popupVisible,
                        buttons.endRoundButton()::isVisible
                )
        );
        GamePresentationSupport gamePresentationSupport = new GamePresentationSupport(
                buttons.retryDebtButton(),
                buttons.declareBankruptcyButton(),
                gameUiController,
                dependencies.auctionViewAdapter(),
                dependencies.pendingDecisionPopupAdapter(),
                dependencies.tradeViewAdapter()
        );
        HostBotInteractionAdapter interactionAdapter = new DesktopHostBotInteractionAdapter(
                new DesktopHostBotInteractionAdapter.PopupHooks() {
                    @Override
                    public boolean popupVisible() {
                        return runtime.popupService().isAnyVisible();
                    }

                    @Override
                    public boolean resolveVisiblePopupFor(Player player) {
                        return runtime.popupService().resolveForComputer(player.getComputerProfile());
                    }

                    @Override
                    public boolean acceptActivePopupFor(Player player) {
                        return runtime.popupService().triggerPrimaryComputerAction();
                    }

                    @Override
                    public boolean declineActivePopupFor(Player player) {
                        return runtime.popupService().triggerSecondaryComputerAction();
                    }
                },
                dependencies.tradeController(),
                hooks::createGameViewFor,
                hooks::createPlayerViewFor
        );
        GameBotTurnDriver.Hooks gameBotTurnHooks = new GameBotTurnHooksAdapter(
                dependencies.sessionApplicationService(),
                dependencies.sessionApplicationService()::handleComputerAuctionAction,
                gameSessionQueries,
                interactionAdapter,
                dependencies.debugPerformanceStats(),
                hooks::currentTurnPlayer,
                hooks::updateLogTurnContext,
                hooks::syncTransientPresentationState,
                hooks::gameOver,
                dependencies.animations()::isRunning,
                hooks::paused,
                hooks::nowMillis,
                hooks::scheduleNextComputerAction,
                hooks::sessionId,
                hooks::projectedRollDiceActionAvailable,
                hooks::projectedEndTurnActionAvailable,
                hooks::restoreBotTurnControlsIfNeeded
        );
        return new GamePresentationBundle(
                gameControlLayout,
                gamePrimaryTurnControls,
                gameSessionQueries,
                gameTurnFlowCoordinator,
                gameUiController,
                gamePresentationSupport,
                gameBotTurnHooks
        );
    }

    public record Buttons(
            MonopolyButton endRoundButton,
            MonopolyButton retryDebtButton,
            MonopolyButton declareBankruptcyButton,
            MonopolyButton debugGodModeButton,
            MonopolyButton pauseButton,
            MonopolyButton tradeButton,
            MonopolyButton saveButton,
            MonopolyButton loadButton,
            MonopolyButton botSpeedButton,
            MonopolyButton languageButton
    ) {
    }

    public record Dependencies(
            Players players,
            Dices dices,
            Board board,
            Animations animations,
            TurnEngine turnEngine,
            SessionApplicationService sessionApplicationService,
            PendingDecisionPopupAdapter pendingDecisionPopupAdapter,
            fi.monopoly.application.session.purchase.PropertyPurchaseFlow propertyPurchaseFlow,
            DebtActionDispatcher debtActionDispatcher,
            DebtController debtController,
            DebugController debugController,
            AuctionViewAdapter auctionViewAdapter,
            TradeViewAdapter tradeViewAdapter,
            TradeController tradeController,
            DebugPerformanceStats debugPerformanceStats
    ) {
    }

    public interface Hooks {
        boolean gameOver();

        boolean debtActive();

        boolean popupVisible();

        boolean paused();

        int goMoneyAmount();

        int nowMillis();

        String sessionId();

        Player currentTurnPlayer();

        fi.monopoly.components.computer.GameView createGameViewFor(Player player);

        fi.monopoly.components.computer.PlayerView createPlayerViewFor(Player player);

        void updateLogTurnContext();

        void hidePrimaryTurnControls();

        void showRollDiceControl();

        void showEndTurnControl();

        void syncTransientPresentationState();

        void scheduleNextComputerAction(BotTurnScheduler.DelayKind delayKind);

        void handlePaymentRequest(fi.monopoly.components.payment.PaymentRequest request,
                                  fi.monopoly.domain.session.TurnContinuationState continuationState,
                                  CallbackAction onResolved);

        boolean projectedRollDiceActionAvailable();

        boolean projectedEndTurnActionAvailable();

        boolean restoreBotTurnControlsIfNeeded();

    }

    public record GamePresentationBundle(
            GameControlLayout gameControlLayout,
            GamePrimaryTurnControls gamePrimaryTurnControls,
            GameSessionQueries gameSessionQueries,
            GameTurnFlowCoordinator gameTurnFlowCoordinator,
            GameUiController gameUiController,
            GamePresentationSupport gamePresentationSupport,
            GameBotTurnDriver.Hooks gameBotTurnHooks
    ) {
    }
}
