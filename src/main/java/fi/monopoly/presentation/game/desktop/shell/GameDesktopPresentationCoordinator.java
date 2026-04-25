package fi.monopoly.presentation.game.desktop.shell;

import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.client.session.desktop.LocalSessionActions;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.host.bot.BotTurnScheduler;
import fi.monopoly.host.bot.GameBotTurnControlCoordinator;
import fi.monopoly.presentation.game.desktop.assembly.GamePresentationFactory;
import fi.monopoly.presentation.game.desktop.runtime.GameRuntimeAssemblyFactory;
import fi.monopoly.presentation.game.desktop.session.SessionViewFacade;
import fi.monopoly.presentation.game.desktop.ui.GameFrameCoordinator;
import fi.monopoly.presentation.game.desktop.ui.GamePrimaryTurnControls;
import fi.monopoly.presentation.game.desktop.ui.GameUiSessionControls;
import fi.monopoly.presentation.game.session.GameSessionQueries;
import fi.monopoly.presentation.game.session.GameSessionStateCoordinator;
import fi.monopoly.presentation.game.turn.GameTurnFlowCoordinator;
import fi.monopoly.presentation.session.debt.DebtController;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;

import static fi.monopoly.text.UiTexts.text;

/**
 * Presentation/runtime coordinator for the desktop-local shell.
 *
 * <p>This owns frame hooks, UI session controls, projected primary-turn control logic, and
 * presentation-facing session hooks. Session restoration and bridge ownership stay in
 * {@link GameDesktopSessionCoordinator}.</p>
 */
@Slf4j
public final class GameDesktopPresentationCoordinator {
    private final fi.monopoly.client.desktop.MonopolyRuntime runtime;
    private final String sessionId;
    private final List<Locale> supportedLocales;
    private final LocalSessionActions localSessionActions;
    private final GameSessionStateCoordinator sessionStateCoordinator;
    private final GameBotTurnControlCoordinator botTurnControlCoordinator;

    public GameDesktopPresentationCoordinator(
            fi.monopoly.client.desktop.MonopolyRuntime runtime,
            String sessionId,
            List<Locale> supportedLocales,
            LocalSessionActions localSessionActions,
            GameSessionStateCoordinator sessionStateCoordinator,
            GameBotTurnControlCoordinator botTurnControlCoordinator
    ) {
        this.runtime = runtime;
        this.sessionId = sessionId;
        this.supportedLocales = supportedLocales;
        this.localSessionActions = localSessionActions;
        this.sessionStateCoordinator = sessionStateCoordinator;
        this.botTurnControlCoordinator = botTurnControlCoordinator;
    }

    public GameRuntimeAssemblyFactory.Hooks createRuntimeAssemblyHooks(GameDesktopShellDependencies dependencies) {
        return new GameRuntimeAssemblyFactory.Hooks() {
            @Override
            public void refreshLabels() {
                dependencies.refreshLabels();
            }

            @Override
            public MonopolyEventListener eventListener() {
                return dependencies.eventListener();
            }

            @Override
            public void rollDice() {
                dependencies.rollDice();
            }

            @Override
            public void setupDefaultGameState(Board board, Players players) {
                dependencies.setupDefaultGameState(board, players);
            }

            @Override
            public void hidePrimaryTurnControls() {
                dependencies.hidePrimaryTurnControls();
            }

            @Override
            public void showRollDiceControl() {
                dependencies.showRollDiceControl();
            }

            @Override
            public void onDebtStateChanged() {
                GameDesktopPresentationCoordinator.this.onDebtStateChanged(dependencies);
            }

            @Override
            public void declareWinner(Player winner) {
                GameDesktopPresentationCoordinator.this.declareWinner(dependencies, winner);
            }

            @Override
            public void debugResetTurnState() {
                GameDesktopPresentationCoordinator.this.debugResetTurnState(dependencies);
            }

            @Override
            public void restoreNormalTurnControls() {
                GameDesktopPresentationCoordinator.this.restoreNormalTurnControls(dependencies);
            }

            @Override
            public void retryPendingDebtPaymentAction() {
                dependencies.retryPendingDebtPaymentAction();
            }

            @Override
            public void handlePaymentRequest(PaymentRequest request, CallbackAction onResolved) {
                dependencies.handlePaymentRequest(request, null, onResolved);
            }

            @Override
            public boolean debtActive() {
                return dependencies.debtState() != null;
            }

            @Override
            public boolean gameOver() {
                return dependencies.sessionState().gameOver();
            }

            @Override
            public int goMoneyAmount() {
                return dependencies.goMoneyAmount();
            }
        };
    }

    public GamePresentationFactory.Hooks createPresentationHooks(GameDesktopShellDependencies dependencies) {
        return new GamePresentationFactory.Hooks() {
            @Override
            public boolean gameOver() {
                return dependencies.sessionState().gameOver();
            }

            @Override
            public boolean debtActive() {
                return dependencies.debtState() != null;
            }

            @Override
            public boolean popupVisible() {
                return dependencies.popupService().isAnyVisible();
            }

            @Override
            public boolean paused() {
                return dependencies.sessionState().paused();
            }

            @Override
            public int goMoneyAmount() {
                return dependencies.goMoneyAmount();
            }

            @Override
            public int nowMillis() {
                return runtime.millis();
            }

            @Override
            public String sessionId() {
                return sessionId;
            }

            @Override
            public Player currentTurnPlayer() {
                return dependencies.currentTurnPlayer();
            }

            @Override
            public fi.monopoly.components.computer.GameView createGameViewFor(Player player) {
                return dependencies.createGameViewFor(player);
            }

            @Override
            public fi.monopoly.components.computer.PlayerView createPlayerViewFor(Player player) {
                return dependencies.createPlayerViewFor(player);
            }

            @Override
            public void updateLogTurnContext() {
                dependencies.updateLogTurnContext();
            }

            @Override
            public void hidePrimaryTurnControls() {
                dependencies.hidePrimaryTurnControls();
            }

            @Override
            public void showRollDiceControl() {
                dependencies.showRollDiceControl();
            }

            @Override
            public void showEndTurnControl() {
                dependencies.showEndTurnControl();
            }

            @Override
            public void syncTransientPresentationState() {
                dependencies.syncTransientPresentationState();
            }

            @Override
            public void scheduleNextComputerAction(BotTurnScheduler.DelayKind delayKind) {
                dependencies.scheduleNextComputerAction(delayKind, runtime.millis());
            }

            @Override
            public void handlePaymentRequest(
                    PaymentRequest request,
                    fi.monopoly.domain.session.TurnContinuationState continuationState,
                    CallbackAction onResolved
            ) {
                dependencies.handlePaymentRequest(request, continuationState, onResolved);
            }

            @Override
            public boolean projectedRollDiceActionAvailable() {
                return isProjectedRollDiceActionAvailable(dependencies);
            }

            @Override
            public boolean projectedEndTurnActionAvailable() {
                return isProjectedEndTurnActionAvailable(dependencies);
            }

            @Override
            public boolean restoreBotTurnControlsIfNeeded() {
                return GameDesktopPresentationCoordinator.this.restoreBotTurnControlsIfNeeded(dependencies);
            }
        };
    }

    public GameUiSessionControls createUiSessionControls(GameDesktopShellDependencies dependencies) {
        return new GameUiSessionControls() {
            @Override
            public List<Locale> supportedLocales() {
                return supportedLocales;
            }

            @Override
            public Locale currentLocale() {
                return fi.monopoly.text.UiTexts.getLocale();
            }

            @Override
            public void switchLanguage(Locale locale) {
                GameDesktopPresentationCoordinator.this.switchLanguage(locale);
            }

            @Override
            public void togglePause() {
                GameDesktopPresentationCoordinator.this.togglePause(dependencies);
            }

            @Override
            public void cycleBotSpeedMode() {
                GameDesktopPresentationCoordinator.this.cycleBotSpeedMode(dependencies);
            }

            @Override
            public void saveSession() {
                localSessionActions.saveSession().run();
            }

            @Override
            public void loadSession() {
                localSessionActions.loadSession().run();
            }
        };
    }

    public GameBotTurnControlCoordinator.Hooks createBotTurnControlHooks(GameDesktopShellDependencies dependencies) {
        return new GameBotTurnControlCoordinator.Hooks() {
            @Override
            public boolean gameOver() {
                return dependencies.sessionState().gameOver();
            }

            @Override
            public boolean popupVisible() {
                return dependencies.popupService().isAnyVisible();
            }

            @Override
            public boolean debtActive() {
                return dependencies.debtState() != null;
            }

            @Override
            public boolean animationsRunning() {
                return dependencies.animations().isRunning();
            }

            @Override
            public boolean activeAuctionOpen() {
                return dependencies.sessionApplicationService().hasActiveAuction();
            }

            @Override
            public boolean activeTradeOpen() {
                return dependencies.sessionApplicationService().hasActiveTrade();
            }

            @Override
            public boolean auctionOverrideActive() {
                return dependencies.sessionApplicationService().hasAuctionOverride();
            }

            @Override
            public boolean tradeOverrideActive() {
                return dependencies.sessionApplicationService().hasTradeOverride();
            }

            @Override
            public boolean pendingDecisionOverrideActive() {
                return dependencies.sessionApplicationService().hasPendingDecisionOverride();
            }

            @Override
            public fi.monopoly.types.DiceState currentDiceState() {
                return dependencies.dices().getValue() != null ? dependencies.dices().getValue().diceState() : null;
            }

            @Override
            public boolean rollDiceActionAlreadyAvailable() {
                return dependencies.gamePrimaryTurnControls().isRollDiceActionAvailable(
                        dependencies.popupService().isAnyVisible(),
                        dependencies.debtState() != null,
                        dependencies.currentTurnPlayer()
                );
            }

            @Override
            public boolean endTurnActionAlreadyAvailable() {
                return dependencies.gamePrimaryTurnControls().isEndTurnActionAvailable(
                        dependencies.popupService().isAnyVisible(),
                        dependencies.debtState() != null,
                        dependencies.currentTurnPlayer()
                );
            }

            @Override
            public void showRollDiceControl() {
                dependencies.showRollDiceControl();
            }

            @Override
            public void showEndTurnControl() {
                dependencies.showEndTurnControl();
            }
        };
    }

    public GameFrameCoordinator.FrameHooks createFrameHooks(GameDesktopShellDependencies dependencies) {
        return new GameFrameCoordinator.FrameHooks() {
            @Override
            public fi.monopoly.presentation.game.session.GameSessionState sessionState() {
                return dependencies.sessionState();
            }

            @Override
            public Board board() {
                return dependencies.board();
            }

            @Override
            public Players players() {
                return dependencies.players();
            }

            @Override
            public Dices dices() {
                return dependencies.dices();
            }

            @Override
            public Animations animations() {
                return dependencies.animations();
            }

            @Override
            public Player turnPlayer() {
                return dependencies.currentTurnPlayer();
            }

            @Override
            public List<String> recentPopupMessages() {
                return dependencies.popupService().recentPopupMessages();
            }

            @Override
            public DebtState debtState() {
                return dependencies.debtState();
            }

            @Override
            public Player debtDebtor() {
                return dependencies.debtState() != null ? dependencies.debtState().paymentRequest().debtor() : null;
            }

            @Override
            public boolean popupVisible() {
                return dependencies.popupService().isAnyVisible();
            }

            @Override
            public boolean debtSidebarMode() {
                return dependencies.debtState() != null;
            }

            @Override
            public boolean endRoundVisible() {
                return dependencies.endRoundVisible();
            }

            @Override
            public boolean rollDiceVisible() {
                return dependencies.rollDiceVisible();
            }

            @Override
            public void focusPlayer(Player player) {
                dependencies.focusPlayer(player);
            }

            @Override
            public void restoreBotTurnControlsIfNeeded() {
                GameDesktopPresentationCoordinator.this.restoreBotTurnControlsIfNeeded(dependencies);
            }
        };
    }

    public void onDebtStateChanged(GameDesktopShellDependencies dependencies) {
        sessionStateCoordinator.onDebtStateChanged(
                () -> dependencies.sessionApplicationService().clearActiveDebtOverride(),
                dependencies::updateDebtButtons,
                () -> restoreBotTurnControlsIfNeeded(dependencies)
        );
    }

    public void togglePause(GameDesktopShellDependencies dependencies) {
        if (!sessionStateCoordinator.togglePause(dependencies.sessionState(), dependencies::refreshLabels)) {
            return;
        }
        log.info("Game paused={}", dependencies.sessionState().paused());
    }

    public void cycleBotSpeedMode(GameDesktopShellDependencies dependencies) {
        BotTurnScheduler.SpeedMode nextMode = sessionStateCoordinator.cycleBotSpeedMode(
                dependencies.sessionState(),
                dependencies.botTurnScheduler()::markReadyNow,
                runtime.millis(),
                dependencies::refreshLabels
        );
        log.info("Bot speed mode={}", nextMode);
    }

    public void switchLanguage(Locale locale) {
        fi.monopoly.text.UiTexts.setLocale(locale);
    }

    public void debugResetTurnState(GameDesktopShellDependencies dependencies) {
        log.debug("Debug action: reset turn state");
        sessionStateCoordinator.debugResetTurnState(new GameSessionStateCoordinator.DebugResetHooks() {
            @Override
            public void finishAllAnimations() {
                dependencies.animations().finishAllAnimations();
            }

            @Override
            public void resetTransientTurnState() {
                dependencies.gameTurnFlowCoordinator().resetTransientTurnState();
            }

            @Override
            public void clearDebtState() {
                dependencies.debtController().clearDebtState();
            }

            @Override
            public void updateDebtButtons() {
                dependencies.updateDebtButtons();
            }

            @Override
            public void hideAllPopups() {
                dependencies.popupService().hideAll();
            }

            @Override
            public void showRollDiceControl() {
                dependencies.showRollDiceControl();
            }

            @Override
            public void showDebugResetMessage() {
                dependencies.popupService().show(text("game.debug.reset"));
            }
        });
    }

    public void restoreNormalTurnControls(GameDesktopShellDependencies dependencies) {
        log.trace("Restoring normal turn controls");
        sessionStateCoordinator.restoreNormalTurnControls(
                dependencies.debtController()::clearDebtState,
                dependencies::showRollDiceControl
        );
    }

    public void declareWinner(GameDesktopShellDependencies dependencies, Player winningPlayer) {
        sessionStateCoordinator.declareWinner(dependencies.sessionState(), winningPlayer, new GameSessionStateCoordinator.WinnerHooks() {
            @Override
            public void resetTransientTurnState() {
                dependencies.gameTurnFlowCoordinator().resetTransientTurnState();
            }

            @Override
            public void clearDebtState() {
                dependencies.debtController().clearDebtState();
            }

            @Override
            public void updateDebtButtons() {
                dependencies.updateDebtButtons();
            }

            @Override
            public void hidePrimaryTurnControls() {
                dependencies.hidePrimaryTurnControls();
            }

            @Override
            public void refreshLabels() {
                dependencies.refreshLabels();
            }

            @Override
            public void focusWinner(Player winner) {
                if (winner.getSpot() != null) {
                    winner.setCoords(winner.getSpot().getTokenCoords(winner));
                    dependencies.focusPlayer(winner);
                }
            }

            @Override
            public void updateLogTurnContext() {
                dependencies.updateLogTurnContext();
            }

            @Override
            public void showVictoryPopup(Player winner) {
                String winnerName = winner != null ? winner.getName() : text("game.bankruptcy.noWinner");
                log.info("Game over. winner={}", winnerName);
                dependencies.popupService().show(text("game.victory.popup", winnerName), () -> {
                });
            }
        });
    }

    public boolean isRollDiceActionAvailable(GameDesktopShellDependencies dependencies, Player currentPlayer) {
        if (dependencies.gamePrimaryTurnControls().isRollDiceActionAvailable(
                dependencies.popupService().isAnyVisible(),
                dependencies.debtState() != null,
                currentPlayer
        )) {
            return true;
        }
        return botTurnControlCoordinator.projectedAction(createBotTurnControlHooks(dependencies), currentPlayer)
                == GameBotTurnControlCoordinator.BotPrimaryAction.ROLL_DICE;
    }

    public boolean isEndTurnActionAvailable(GameDesktopShellDependencies dependencies, Player currentPlayer) {
        if (dependencies.gamePrimaryTurnControls().isEndTurnActionAvailable(
                dependencies.popupService().isAnyVisible(),
                dependencies.debtState() != null,
                currentPlayer
        )) {
            return true;
        }
        return botTurnControlCoordinator.projectedAction(createBotTurnControlHooks(dependencies), currentPlayer)
                == GameBotTurnControlCoordinator.BotPrimaryAction.END_TURN;
    }

    public boolean isProjectedRollDiceActionAvailable(GameDesktopShellDependencies dependencies) {
        return isRollDiceActionAvailable(dependencies, dependencies.currentTurnPlayer());
    }

    public boolean isProjectedEndTurnActionAvailable(GameDesktopShellDependencies dependencies) {
        return isEndTurnActionAvailable(dependencies, dependencies.currentTurnPlayer());
    }

    public boolean restoreBotTurnControlsIfNeeded(GameDesktopShellDependencies dependencies) {
        return botTurnControlCoordinator.restoreControlsIfNeeded(
                createBotTurnControlHooks(dependencies),
                dependencies.currentTurnPlayer()
        );
    }

    public SessionViewFacade createSessionViewFacade(GameDesktopShellDependencies dependencies) {
        return new SessionViewFacade(
                dependencies.popupService(),
                dependencies::players,
                dependencies::board,
                dependencies::debtState,
                dependencies::retryDebtVisible,
                dependencies::declareBankruptcyVisible,
                player -> isRollDiceActionAvailable(dependencies, player),
                player -> isEndTurnActionAvailable(dependencies, player),
                () -> dependencies.gameSessionQueries().countUnownedProperties(),
                player -> dependencies.gameSessionQueries().calculateBoardDangerScore(player)
        );
    }
}
