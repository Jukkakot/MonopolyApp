package fi.monopoly.presentation.game.desktop.assembly;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.application.session.purchase.PropertyPurchaseFlow;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.Players;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.turn.TurnEngine;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.host.bot.BotTurnScheduler;
import fi.monopoly.host.bot.GameBotTurnDriver;
import fi.monopoly.presentation.game.session.GameSessionQueries;
import fi.monopoly.presentation.game.turn.GameTurnFlowCoordinator;
import fi.monopoly.presentation.game.desktop.runtime.DebugController;
import fi.monopoly.presentation.game.desktop.runtime.GameRuntimeAssemblyFactory;
import fi.monopoly.presentation.game.desktop.session.GameSessionBridgeFactory;
import fi.monopoly.presentation.game.desktop.ui.GameControlLayout;
import fi.monopoly.presentation.game.desktop.ui.GameButtonLayoutFactory;
import fi.monopoly.presentation.game.desktop.ui.GamePrimaryTurnControls;
import fi.monopoly.presentation.game.desktop.ui.GamePresentationSupport;
import fi.monopoly.presentation.game.desktop.ui.GameUiController;
import fi.monopoly.presentation.session.auction.AuctionViewAdapter;
import fi.monopoly.presentation.session.debt.DebtActionDispatcher;
import fi.monopoly.presentation.session.debt.DebtController;
import fi.monopoly.presentation.session.purchase.PendingDecisionPopupAdapter;
import fi.monopoly.presentation.session.trade.TradeController;
import fi.monopoly.presentation.session.trade.TradeViewAdapter;
import fi.monopoly.utils.DebugPerformanceStats;

/**
 * Builds the full desktop gameplay assembly used by the local Processing client.
 *
 * <p>This lifts the large multi-stage wiring sequence out of {@code Game}: runtime bootstrap,
 * authoritative session bridge creation, legacy game session registration, and presentation
 * coordinators are assembled here into one bundle. That keeps the desktop shell thinner and makes
 * the remaining client composition root explicit while the app moves toward a backend-driven
 * architecture.</p>
 */
public final class GameDesktopAssemblyFactory {
    private final GameRuntimeAssemblyFactory runtimeAssemblyFactory;
    private final GamePresentationFactory gamePresentationFactory;

    public GameDesktopAssemblyFactory() {
        this(new GameRuntimeAssemblyFactory(), new GamePresentationFactory());
    }

    GameDesktopAssemblyFactory(
            GameRuntimeAssemblyFactory runtimeAssemblyFactory,
            GamePresentationFactory gamePresentationFactory
    ) {
        this.runtimeAssemblyFactory = runtimeAssemblyFactory;
        this.gamePresentationFactory = gamePresentationFactory;
    }

    public GameDesktopAssembly create(
            MonopolyRuntime runtime,
            SessionState restoredSessionState,
            String sessionId,
            GameButtonLayoutFactory.Buttons buttons,
            TurnEngine turnEngine,
            GameRuntimeAssemblyFactory.Hooks runtimeHooks,
            GameSessionBridgeFactory.Hooks sessionBridgeHooks,
            GamePresentationFactory.Hooks presentationHooks,
            DebugPerformanceStats debugPerformanceStats
    ) {
        GameRuntimeAssemblyFactory.GameRuntimeAssembly runtimeAssembly =
                runtimeAssemblyFactory.create(runtime, restoredSessionState, runtimeHooks);

        GameSessionBridgeFactory.GameSessionBridge sessionBridge = new GameSessionBridgeFactory(runtime).create(
                sessionId,
                runtimeAssembly.players(),
                runtimeAssembly.dices(),
                runtimeAssembly.debtController(),
                sessionBridgeHooks
        );

        runtimeAssemblyFactory.registerGameSession(
                runtime,
                runtimeAssembly,
                sessionBridge.debtActionDispatcher(),
                runtimeHooks
        );

        GamePresentationFactory.GamePresentationBundle presentationBundle = gamePresentationFactory.create(
                runtime,
                new GamePresentationFactory.Buttons(
                        buttons.endRoundButton(),
                        buttons.retryDebtButton(),
                        buttons.declareBankruptcyButton(),
                        buttons.debugGodModeButton(),
                        buttons.pauseButton(),
                        buttons.tradeButton(),
                        buttons.saveButton(),
                        buttons.loadButton(),
                        buttons.botSpeedButton(),
                        buttons.languageButton()
                ),
                new GamePresentationFactory.Dependencies(
                        runtimeAssembly.players(),
                        runtimeAssembly.dices(),
                        runtimeAssembly.board(),
                        runtimeAssembly.animations(),
                        turnEngine,
                        sessionBridge.sessionApplicationService(),
                        sessionBridge.pendingDecisionPopupAdapter(),
                        sessionBridge.pendingDecisionPopupAdapter(),
                        sessionBridge.debtActionDispatcher(),
                        runtimeAssembly.debtController(),
                        runtimeAssembly.debugController(),
                        sessionBridge.auctionViewAdapter(),
                        sessionBridge.tradeViewAdapter(),
                        sessionBridge.tradeController(),
                        debugPerformanceStats
                ),
                presentationHooks
        );

        return new GameDesktopAssembly(
                runtimeAssembly.board(),
                runtimeAssembly.players(),
                runtimeAssembly.dices(),
                runtimeAssembly.animations(),
                runtimeAssembly.debtController(),
                runtimeAssembly.debugController(),
                sessionBridge.sessionApplicationService(),
                sessionBridge.pendingDecisionPopupAdapter(),
                sessionBridge.pendingDecisionPopupAdapter(),
                sessionBridge.debtActionDispatcher(),
                sessionBridge.auctionViewAdapter(),
                sessionBridge.tradeViewAdapter(),
                sessionBridge.tradeController(),
                presentationBundle.gameControlLayout(),
                presentationBundle.gamePrimaryTurnControls(),
                presentationBundle.gameSessionQueries(),
                presentationBundle.gameTurnFlowCoordinator(),
                presentationBundle.gameUiController(),
                presentationBundle.gamePresentationSupport(),
                presentationBundle.gameBotTurnHooks()
        );
    }

    public record GameDesktopAssembly(
            Board board,
            Players players,
            Dices dices,
            Animations animations,
            DebtController debtController,
            DebugController debugController,
            SessionApplicationService sessionApplicationService,
            PendingDecisionPopupAdapter pendingDecisionPopupAdapter,
            PropertyPurchaseFlow propertyPurchaseFlow,
            DebtActionDispatcher debtActionDispatcher,
            AuctionViewAdapter auctionViewAdapter,
            TradeViewAdapter tradeViewAdapter,
            TradeController tradeController,
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
