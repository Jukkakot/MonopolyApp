package fi.monopoly.presentation.game;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.components.turn.TurnEngine;
import fi.monopoly.presentation.session.debt.DebtController;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class GameDesktopShellCoordinatorTest {

    @AfterEach
    void tearDown() {
        MonopolyApp.DEBUG_MODE = false;
        fi.monopoly.text.UiTexts.setLocale(Locale.ENGLISH);
    }

    @Test
    void sessionBridgeHooksBlockTradeWhenPopupDebtOrGameOverActive() {
        TestDependencies dependencies = createDependencies();
        GameDesktopShellCoordinator coordinator = createCoordinator(dependencies.runtime);
        GameSessionBridgeFactory.Hooks hooks = coordinator.createSessionBridgeHooks(dependencies);

        assertTrue(hooks.canOpenTrade());

        dependencies.runtime.popupService().show("Popup");
        assertFalse(hooks.canOpenTrade());

        dependencies.runtime.popupService().hideAll();
        dependencies.debtState = new DebtState(null, () -> {
        }, false);
        assertFalse(hooks.canOpenTrade());

        dependencies.debtState = null;
        dependencies.sessionState.setGameOver(true);
        assertFalse(hooks.canOpenTrade());
    }

    @Test
    void togglePauseFlipsStateThroughCoordinator() {
        TestDependencies dependencies = createDependencies();
        GameDesktopShellCoordinator coordinator = createCoordinator(dependencies.runtime);

        coordinator.togglePause(dependencies);

        assertTrue(dependencies.sessionState.paused());
        assertEquals(1, dependencies.refreshLabelsCalls);
    }

    @Test
    void declareWinnerUpdatesShellStateAndRunsUiHooks() {
        TestDependencies dependencies = createDependencies();
        GameDesktopShellCoordinator coordinator = createCoordinator(dependencies.runtime);
        Player winner = dependencies.currentTurnPlayer;

        coordinator.declareWinner(dependencies, winner);

        assertTrue(dependencies.sessionState.gameOver());
        assertSame(winner, dependencies.sessionState.winner());
        assertTrue(dependencies.hideControlsCalled.get());
        assertTrue(dependencies.updateDebtButtonsCalled.get());
        assertTrue(dependencies.refreshLabelsCalled.get());
        assertTrue(dependencies.runtime.popupService().isAnyVisible());
    }

    private static GameDesktopShellCoordinator createCoordinator(MonopolyRuntime runtime) {
        return new GameDesktopShellCoordinator(
                runtime,
                "local-session",
                List.of(Locale.ENGLISH),
                LocalSessionActions.NO_OP_ACTIONS,
                new GameSessionStateCoordinator(),
                new GameBotTurnControlCoordinator()
        );
    }

    private static TestDependencies createDependencies() {
        MonopolyRuntime runtime = initHeadlessRuntime(1200, 800);
        Players players = new Players(runtime);
        Player currentTurnPlayer = new Player("Bot", Color.PINK, 1500, 1, ComputerPlayerProfile.STRONG);
        players.addPlayer(currentTurnPlayer);
        Dices dices = new Dices(runtime);
        Animations animations = new Animations();
        GamePrimaryTurnControls primaryTurnControls = new GamePrimaryTurnControls(
                dices,
                new MonopolyButton(runtime, "endRound"),
                () -> false,
                players::getTurn
        );
        DebtController debtController = new DebtController(
                runtime,
                players,
                () -> {
                },
                () -> {
                },
                () -> {
                },
                winner -> {
                }
        );
        GameTurnFlowCoordinator gameTurnFlowCoordinator = new GameTurnFlowCoordinator(
                runtime,
                players,
                dices,
                null,
                animations,
                new TurnEngine(),
                (player, property, message, continuationState) -> {
                },
                () -> 200,
                new GameTurnFlowCoordinator.Hooks() {
                    @Override
                    public void updateLogTurnContext() {
                    }

                    @Override
                    public void hidePrimaryTurnControls() {
                    }

                    @Override
                    public void showRollDiceControl() {
                    }

                    @Override
                    public void showEndTurnControl() {
                    }

                    @Override
                    public boolean gameOver() {
                        return false;
                    }

                    @Override
                    public boolean debtActive() {
                        return false;
                    }

                    @Override
                    public void handlePaymentRequest(
                            PaymentRequest request,
                            fi.monopoly.domain.session.TurnContinuationState continuationState,
                            CallbackAction onResolved
                    ) {
                    }
                }
        );
        return new TestDependencies(
                runtime,
                players,
                currentTurnPlayer,
                dices,
                primaryTurnControls,
                animations,
                debtController,
                gameTurnFlowCoordinator
        );
    }

    private static MonopolyRuntime initHeadlessRuntime(int width, int height) {
        MonopolyApp app = new MonopolyApp();
        app.width = width;
        app.height = height;

        PGraphicsJava2D graphics = new PGraphicsJava2D();
        graphics.setParent(app);
        graphics.setPrimary(true);
        graphics.setSize(app.width, app.height);
        app.g = graphics;

        ControlP5 controlP5 = new ControlP5(app);
        PFont font = app.createFont("Arial", 20);
        return MonopolyRuntime.initialize(app, controlP5, font, font, font);
    }

    private static final class TestDependencies implements GameDesktopShellCoordinator.Dependencies {
        private final MonopolyRuntime runtime;
        private final Players players;
        private final Player currentTurnPlayer;
        private final Dices dices;
        private final GamePrimaryTurnControls primaryTurnControls;
        private final Animations animations;
        private final DebtController debtController;
        private final GameTurnFlowCoordinator gameTurnFlowCoordinator;
        private final GameSessionState sessionState = new GameSessionState();
        private final AtomicBoolean hideControlsCalled = new AtomicBoolean();
        private final AtomicBoolean updateDebtButtonsCalled = new AtomicBoolean();
        private final AtomicBoolean refreshLabelsCalled = new AtomicBoolean();
        private DebtState debtState;
        private int refreshLabelsCalls;

        private TestDependencies(
                MonopolyRuntime runtime,
                Players players,
                Player currentTurnPlayer,
                Dices dices,
                GamePrimaryTurnControls primaryTurnControls,
                Animations animations,
                DebtController debtController,
                GameTurnFlowCoordinator gameTurnFlowCoordinator
        ) {
            this.runtime = runtime;
            this.players = players;
            this.currentTurnPlayer = currentTurnPlayer;
            this.dices = dices;
            this.primaryTurnControls = primaryTurnControls;
            this.animations = animations;
            this.debtController = debtController;
            this.gameTurnFlowCoordinator = gameTurnFlowCoordinator;
        }

        @Override
        public GameSessionState sessionState() {
            return sessionState;
        }

        @Override
        public Players players() {
            return players;
        }

        @Override
        public Player currentTurnPlayer() {
            return currentTurnPlayer;
        }

        @Override
        public Player playerById(String playerId) {
            return currentTurnPlayer;
        }

        @Override
        public Board board() {
            return null;
        }

        @Override
        public Dices dices() {
            return dices;
        }

        @Override
        public Animations animations() {
            return animations;
        }

        @Override
        public fi.monopoly.presentation.session.debt.DebtController debtController() {
            return debtController;
        }

        @Override
        public DebtState debtState() {
            return debtState;
        }

        @Override
        public GameTurnFlowCoordinator gameTurnFlowCoordinator() {
            return gameTurnFlowCoordinator;
        }

        @Override
        public GamePrimaryTurnControls gamePrimaryTurnControls() {
            return primaryTurnControls;
        }

        @Override
        public GameSessionQueries gameSessionQueries() {
            return null;
        }

        @Override
        public SessionApplicationService sessionApplicationService() {
            return null;
        }

        @Override
        public PopupService popupService() {
            return runtime.popupService();
        }

        @Override
        public BotTurnScheduler botTurnScheduler() {
            return new BotTurnScheduler();
        }

        @Override
        public fi.monopoly.components.computer.GameView createCurrentGameView() {
            return null;
        }

        @Override
        public fi.monopoly.components.computer.PlayerView createCurrentPlayerView() {
            return null;
        }

        @Override
        public void refreshLabels() {
            refreshLabelsCalls++;
            refreshLabelsCalled.set(true);
        }

        @Override
        public void rollDice() {
        }

        @Override
        public void setupDefaultGameState(Board board, Players players) {
        }

        @Override
        public void hidePrimaryTurnControls() {
            hideControlsCalled.set(true);
        }

        @Override
        public void showRollDiceControl() {
        }

        @Override
        public void showEndTurnControl() {
        }

        @Override
        public void updateDebtButtons() {
            updateDebtButtonsCalled.set(true);
        }

        @Override
        public void syncTransientPresentationState() {
        }

        @Override
        public void updateLogTurnContext() {
        }

        @Override
        public void retryPendingDebtPaymentAction() {
        }

        @Override
        public void handlePaymentRequest(
                PaymentRequest request,
                fi.monopoly.domain.session.TurnContinuationState continuationState,
                CallbackAction onResolved
        ) {
        }

        @Override
        public void endRound(boolean switchTurns) {
        }

        @Override
        public void scheduleNextComputerAction(BotTurnScheduler.DelayKind delayKind, int now) {
        }

        @Override
        public void resumeContinuation(fi.monopoly.domain.session.TurnContinuationState continuationState) {
        }

        @Override
        public void focusPlayer(Player player) {
        }

        @Override
        public int goMoneyAmount() {
            return 200;
        }

        @Override
        public boolean retryDebtVisible() {
            return false;
        }

        @Override
        public boolean declareBankruptcyVisible() {
            return false;
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
        public fi.monopoly.components.event.MonopolyEventListener eventListener() {
            return event -> false;
        }
    }
}
