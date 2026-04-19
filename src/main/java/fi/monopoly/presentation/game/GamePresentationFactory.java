package fi.monopoly.presentation.game;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.turn.TurnEngine;
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
                hooks.supportedLocales(),
                new GameUiHooksAdapter(
                        dependencies.board(),
                        dependencies.dices(),
                        dependencies.debtController(),
                        dependencies.tradeController(),
                        dependencies.debugController(),
                        gameTurnFlowCoordinator,
                        hooks::togglePause,
                        hooks::cycleBotSpeedMode,
                        dependencies.debtActionDispatcher()::payDebt,
                        dependencies.debtActionDispatcher()::declareBankruptcy,
                        dependencies.animations()::finishAllAnimations,
                        hooks::gameOver,
                        hooks::popupVisible,
                        buttons.endRoundButton()::isVisible,
                        hooks::switchLanguage,
                        hooks.saveSessionAction(),
                        hooks.loadSessionAction()
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
        GameBotTurnDriver.Hooks gameBotTurnHooks = new GameBotTurnHooksAdapter(
                runtime,
                dependencies.sessionApplicationService(),
                gameSessionQueries,
                dependencies.tradeController(),
                dependencies.debugPerformanceStats(),
                hooks::currentTurnPlayer,
                hooks::createCurrentGameView,
                hooks::createCurrentPlayerView,
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
            fi.monopoly.components.DebugController debugController,
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

        List<Locale> supportedLocales();

        String sessionId();

        Player currentTurnPlayer();

        fi.monopoly.components.computer.GameView createCurrentGameView();

        fi.monopoly.components.computer.PlayerView createCurrentPlayerView();

        void updateLogTurnContext();

        void hidePrimaryTurnControls();

        void showRollDiceControl();

        void showEndTurnControl();

        void syncTransientPresentationState();

        void togglePause();

        void cycleBotSpeedMode();

        void switchLanguage(Locale locale);

        void scheduleNextComputerAction(BotTurnScheduler.DelayKind delayKind);

        void handlePaymentRequest(fi.monopoly.components.payment.PaymentRequest request,
                                  fi.monopoly.domain.session.TurnContinuationState continuationState,
                                  CallbackAction onResolved);

        boolean projectedRollDiceActionAvailable();

        boolean projectedEndTurnActionAvailable();

        boolean restoreBotTurnControlsIfNeeded();

        Runnable saveSessionAction();

        Runnable loadSessionAction();
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
