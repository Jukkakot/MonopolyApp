package fi.monopoly.presentation.game.ui;

import fi.monopoly.client.desktop.DesktopClientSettings;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.payment.BankTarget;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.SessionStatus;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import fi.monopoly.host.bot.BotTurnScheduler;
import fi.monopoly.presentation.game.session.GameSessionState;
import fi.monopoly.presentation.game.session.GameSessionStateCoordinator;
import fi.monopoly.presentation.game.desktop.ui.GameControlLayout;
import fi.monopoly.presentation.game.desktop.ui.GameFrameCoordinator;
import fi.monopoly.presentation.game.desktop.ui.GamePrimaryTurnControls;
import fi.monopoly.presentation.game.desktop.ui.GamePresentationSupport;
import fi.monopoly.presentation.game.desktop.ui.GameSidebarPresenter;
import fi.monopoly.presentation.game.desktop.ui.GameSidebarStateFactory;
import fi.monopoly.presentation.game.desktop.ui.GameUiController;
import fi.monopoly.presentation.game.desktop.ui.GameUiSessionControls;
import fi.monopoly.support.TestDesktopRuntimeFactory;
import fi.monopoly.utils.DebugPerformanceStats;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class GameFrameCoordinatorTest {

    @AfterEach
    void tearDown() {
        DesktopClientSettings.setDebugMode(false);
        fi.monopoly.text.UiTexts.setLocale(Locale.ENGLISH);
    }

    @Test
    void createSidebarStateExpiresPersistenceNoticeAndUsesVisibleRollControlPhase() {
        TestContext context = createContext(1700, 996);
        GameSessionState sessionState = new GameSessionState();
        sessionState.setPersistenceNotice("Saved", -1);
        SessionState authState = SessionState.builder()
                .sessionId("test")
                .version(1L)
                .status(SessionStatus.IN_PROGRESS)
                .turn(new TurnState("p1", TurnPhase.WAITING_FOR_ROLL, true, false))
                .seats(List.of())
                .players(List.of())
                .properties(List.of())
                .build();

        GameSidebarPresenter.SidebarState sidebarState = context.coordinator.createSidebarState(
                new TestFrameHooks(context, sessionState, null, authState)
        );

        assertNull(sessionState.persistenceNotice());
        assertEquals(fi.monopoly.text.UiTexts.text("sidebar.phase.roll"), sidebarState.currentTurnPhase());
        assertSame(context.players.getTurn(), sidebarState.turnPlayer());
    }

    @Test
    void sidebarContentTopStaysAboveHistoryPanelWhenWindowIsShort() {
        TestContext context = createContext(1700, 560);
        GameSessionState sessionState = new GameSessionState();

        context.coordinator.updateFrameLayoutMetrics();
        float contentTop = context.coordinator.getSidebarContentTop(
                new TestFrameHooks(context, sessionState, null, null)
        );
        float historyY = context.coordinator.getSidebarHistoryPanelY();

        assertTrue(contentTop <= historyY - 16f);
        assertTrue(context.coordinator.getSidebarHistoryHeight() <= 192f);
        assertTrue(context.coordinator.getSidebarHistoryHeight() >= 112f);
    }

    @Test
    void createSidebarStateUsesDebtPhaseWhenDebtIsActive() {
        TestContext context = createContext(1700, 996);
        GameSessionState sessionState = new GameSessionState();
        Player debtor = context.players.getTurn();
        DebtState debtState = new DebtState(
                new PaymentRequest(debtor, BankTarget.INSTANCE, 200, "Tax"),
                () -> {
                },
                true
        );
        SessionState authState = SessionState.builder()
                .sessionId("test")
                .version(1L)
                .status(SessionStatus.IN_PROGRESS)
                .turn(new TurnState("p1", TurnPhase.RESOLVING_DEBT, false, false))
                .seats(List.of())
                .players(List.of())
                .properties(List.of())
                .build();

        GameSidebarPresenter.SidebarState sidebarState = context.coordinator.createSidebarState(
                new TestFrameHooks(context, sessionState, debtState, authState)
        );

        assertEquals(fi.monopoly.text.UiTexts.text("sidebar.phase.debt"), sidebarState.currentTurnPhase());
        assertSame(debtState, sidebarState.debtState());
    }

    private static TestContext createContext(int width, int height) {
        MonopolyRuntime runtime = initHeadlessRuntime(width, height);
        MonopolyButton endRoundButton = new MonopolyButton(runtime, "endRound");
        MonopolyButton retryDebtButton = new MonopolyButton(runtime, "retryDebt");
        MonopolyButton declareBankruptcyButton = new MonopolyButton(runtime, "declareBankruptcy");
        MonopolyButton debugGodModeButton = new MonopolyButton(runtime, "debugGodMode");
        MonopolyButton pauseButton = new MonopolyButton(runtime, "pause");
        MonopolyButton tradeButton = new MonopolyButton(runtime, "trade");
        MonopolyButton saveButton = new MonopolyButton(runtime, "save");
        MonopolyButton loadButton = new MonopolyButton(runtime, "load");
        MonopolyButton botSpeedButton = new MonopolyButton(runtime, "botSpeed");
        MonopolyButton languageButton = new MonopolyButton(runtime, "language");
        Dices dices = new Dices(runtime);
        GameControlLayout controlLayout = new GameControlLayout(
                runtime,
                endRoundButton,
                retryDebtButton,
                declareBankruptcyButton,
                debugGodModeButton,
                pauseButton,
                tradeButton,
                saveButton,
                loadButton,
                botSpeedButton,
                languageButton,
                dices
        );
        GameUiController uiController = new GameUiController(
                endRoundButton,
                retryDebtButton,
                declareBankruptcyButton,
                debugGodModeButton,
                pauseButton,
                tradeButton,
                saveButton,
                loadButton,
                botSpeedButton,
                languageButton,
                new NoOpUiSessionControls(),
                new NoOpUiHooks()
        );
        GamePresentationSupport presentationSupport = new GamePresentationSupport(
                retryDebtButton,
                declareBankruptcyButton,
                uiController,
                null,
                null,
                null
        );
        Players players = new Players(runtime);
        players.addPlayer(new Player("Tester", Color.PINK, 1500, 1));
        GamePrimaryTurnControls primaryTurnControls = new GamePrimaryTurnControls(
                dices,
                endRoundButton,
                () -> false,
                players::getTurn
        );
        GameFrameCoordinator coordinator = new GameFrameCoordinator(
                runtime,
                controlLayout,
                new GameSidebarPresenter(runtime),
                presentationSupport,
                primaryTurnControls,
                new GameSidebarStateFactory(),
                new GameSessionStateCoordinator(),
                new BotTurnScheduler(() -> false),
                new DebugPerformanceStats(),
                List.of(
                        endRoundButton,
                        retryDebtButton,
                        declareBankruptcyButton,
                        debugGodModeButton,
                        pauseButton,
                        tradeButton,
                        saveButton,
                        loadButton,
                        botSpeedButton,
                        languageButton
                )
        );
        return new TestContext(runtime, coordinator, players, dices, new Animations());
    }

    private static MonopolyRuntime initHeadlessRuntime(int width, int height) {
        return TestDesktopRuntimeFactory.create(width, height).runtime();
    }

    private record TestContext(
            MonopolyRuntime runtime,
            GameFrameCoordinator coordinator,
            Players players,
            Dices dices,
            Animations animations
    ) {
    }

    private record TestFrameHooks(
            TestContext context,
            GameSessionState sessionState,
            DebtState debtState,
            SessionState authoritativeState
    ) implements GameFrameCoordinator.FrameHooks {

        @Override
        public GameSessionState sessionState() {
            return sessionState;
        }

        @Override
        public SessionState authoritativeSessionState() {
            return authoritativeState;
        }

        @Override
        public fi.monopoly.components.board.Board board() {
            return null;
        }

        @Override
        public Players players() {
            return context.players();
        }

        @Override
        public Dices dices() {
            return context.dices();
        }

        @Override
        public Animations animations() {
            return context.animations();
        }

        @Override
        public Player turnPlayer() {
            return context.players().getTurn();
        }

        @Override
        public List<String> recentPopupMessages() {
            return List.of("message");
        }

        @Override
        public DebtState debtState() {
            return debtState;
        }

        @Override
        public Player debtDebtor() {
            return debtState != null ? debtState.paymentRequest().debtor() : null;
        }

        @Override
        public boolean popupVisible() {
            return false;
        }

        @Override
        public boolean debtSidebarMode() {
            return debtState != null;
        }

        @Override
        public boolean endRoundVisible() {
            return false;
        }

        @Override
        public boolean rollDiceVisible() {
            return false;
        }

        @Override
        public void focusPlayer(Player player) {
        }

        @Override
        public void restoreBotTurnControlsIfNeeded() {
        }
    }

    private static final class NoOpUiHooks implements GameUiController.Hooks {
        @Override
        public boolean gameOver() {
            return false;
        }

        @Override
        public boolean popupVisible() {
            return false;
        }

        @Override
        public boolean debtActive() {
            return false;
        }

        @Override
        public boolean canEndTurn() {
            return false;
        }

        @Override
        public void openTradeMenu() {
        }

        @Override
        public void payDebt() {
        }

        @Override
        public void declareBankruptcy() {
        }

        @Override
        public void endRound() {
        }

        @Override
        public void openGodModeMenu() {
        }

        @Override
        public void finishAllAnimations() {
        }

        @Override
        public void toggleSkipAnimations() {
        }

        @Override
        public fi.monopoly.components.spots.Spot hoveredSpot() {
            return null;
        }

        @Override
        public boolean debugFlyToHoveredSpot(fi.monopoly.components.spots.Spot hoveredSpot) {
            return false;
        }

    }

    private static final class NoOpUiSessionControls implements GameUiSessionControls {
        @Override
        public List<Locale> supportedLocales() {
            return List.of(Locale.ENGLISH);
        }

        @Override
        public Locale currentLocale() {
            return Locale.ENGLISH;
        }

        @Override
        public void switchLanguage(Locale locale) {
        }

        @Override
        public void togglePause() {
        }

        @Override
        public void cycleBotSpeedMode() {
        }

        @Override
        public void saveSession() {
        }

        @Override
        public void loadSession() {
        }
    }
}
