package fi.monopoly.presentation.game.shell;

import fi.monopoly.application.session.SessionPresentationStatePort;
import fi.monopoly.client.desktop.DesktopClientSettings;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.client.session.desktop.LocalSessionActions;
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
import fi.monopoly.host.bot.BotTurnScheduler;
import fi.monopoly.host.bot.GameBotTurnControlCoordinator;
import fi.monopoly.presentation.game.desktop.session.GameSessionBridgeFactory;
import fi.monopoly.presentation.game.desktop.shell.GameDesktopPresentationCoordinator;
import fi.monopoly.presentation.game.desktop.shell.GameDesktopShellDependencies;
import fi.monopoly.presentation.game.desktop.shell.GameDesktopSessionCoordinator;
import fi.monopoly.presentation.game.desktop.ui.GamePrimaryTurnControls;
import fi.monopoly.presentation.game.session.GameSessionQueries;
import fi.monopoly.presentation.game.session.GameSessionState;
import fi.monopoly.presentation.game.session.GameSessionStateCoordinator;
import fi.monopoly.presentation.game.turn.GameTurnFlowCoordinator;
import fi.monopoly.presentation.session.debt.DebtController;
import fi.monopoly.support.TestDesktopRuntimeFactory;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameDesktopShellCoordinatorTest {

    @AfterEach
    void tearDown() {
        DesktopClientSettings.setDebugMode(false);
        fi.monopoly.text.UiTexts.setLocale(Locale.ENGLISH);
    }

    @Test
    void sessionBridgeHooksBlockTradeWhenPopupDebtOrGameOverActive() {
        TestDependencies dependencies = createDependencies();
        GameDesktopSessionCoordinator coordinator = createSessionCoordinator(dependencies.runtime);
        GameSessionBridgeFactory.Hooks hooks = coordinator.createSessionBridgeHooks(dependencies.shellDependencies);

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
        GameDesktopPresentationCoordinator coordinator = createPresentationCoordinator(dependencies.runtime);

        coordinator.togglePause(dependencies.shellDependencies);

        assertTrue(dependencies.sessionState.paused());
        assertEquals(1, dependencies.refreshLabelsCalls);
    }

    @Test
    void declareWinnerUpdatesShellStateAndRunsUiHooks() {
        TestDependencies dependencies = createDependencies();
        GameDesktopPresentationCoordinator coordinator = createPresentationCoordinator(dependencies.runtime);
        Player winner = dependencies.currentTurnPlayer;

        coordinator.declareWinner(dependencies.shellDependencies, "player-1", winner.getName());

        assertTrue(dependencies.sessionState.gameOver());
        assertEquals("player-1", dependencies.sessionState.winnerPlayerId());
        assertEquals(winner.getName(), dependencies.sessionState.winnerName());
        assertTrue(dependencies.hideControlsCalled.get());
        assertTrue(dependencies.updateDebtButtonsCalled.get());
        assertTrue(dependencies.refreshLabelsCalled.get());
        assertTrue(dependencies.runtime.popupService().isAnyVisible());
    }

    private static GameDesktopSessionCoordinator createSessionCoordinator(MonopolyRuntime runtime) {
        return new GameDesktopSessionCoordinator(runtime, new GameSessionStateCoordinator());
    }

    private static GameDesktopPresentationCoordinator createPresentationCoordinator(MonopolyRuntime runtime) {
        return new GameDesktopPresentationCoordinator(
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
                () -> currentTurnPlayer.isComputerControlled()
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
                winnerPlayerId -> {
                }
        );
        GameTurnFlowCoordinator gameTurnFlowCoordinator = new GameTurnFlowCoordinator(
                runtime,
                players,
                dices,
                null,
                animations,
                new TurnEngine(),
                (playerId, propertyId, displayName, price, message, continuationState) -> {
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
        return TestDesktopRuntimeFactory.create(width, height).runtime();
    }

    private static final class TestDependencies {
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
        private final GameDesktopShellDependencies shellDependencies;
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
            this.shellDependencies = new GameDesktopShellDependencies(
                    new GameDesktopShellDependencies.StateAccess(
                            () -> sessionState,
                            () -> players,
                            () -> currentTurnPlayer != null,
                            () -> currentTurnPlayer != null && currentTurnPlayer.isComputerControlled(),
                            () -> null,
                            () -> dices,
                            () -> animations,
                            () -> debtController,
                            () -> debtState,
                            () -> gameTurnFlowCoordinator,
                            () -> primaryTurnControls,
                            () -> null,
                            () -> null,
                            () -> null,
                            runtime::popupService,
                            () -> new BotTurnScheduler(() -> false)
                    ),
                    new GameDesktopShellDependencies.ProjectionAccess(
                            player -> null,
                            player -> null
                    ),
                    new GameDesktopShellDependencies.ActionAccess(
                            this::refreshLabels,
                            () -> {
                            },
                            (board, playerList) -> {
                            },
                            this::hidePrimaryTurnControls,
                            () -> {
                            },
                            () -> {
                            },
                            this::updateDebtButtons,
                            () -> {
                            },
                            () -> {
                            },
                            () -> {
                            },
                            (request, continuationState, onResolved) -> {
                            },
                            switchTurns -> {
                            },
                            (delayKind, now) -> {
                            },
                            continuationState -> {
                            },
                            playerId -> {
                            }
                    ),
                    new GameDesktopShellDependencies.VisibilityAccess(
                            () -> 200,
                            () -> false,
                            () -> false,
                            () -> event -> false,
                            () -> false,
                            () -> false
                    )
            );
        }

        private void refreshLabels() {
            refreshLabelsCalls++;
            refreshLabelsCalled.set(true);
        }

        private void hidePrimaryTurnControls() {
            hideControlsCalled.set(true);
        }

        private void updateDebtButtons() {
            updateDebtButtonsCalled.set(true);
        }
    }
}
