package fi.monopoly.presentation.game.desktop;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.application.session.SessionApplicationService;
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
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.presentation.game.bot.BotTurnScheduler;
import fi.monopoly.presentation.game.bot.GameBotTurnControlCoordinator;
import fi.monopoly.presentation.game.bot.GameBotTurnDriver;
import fi.monopoly.presentation.game.session.GameSessionQueries;
import fi.monopoly.presentation.game.session.GameSessionState;
import fi.monopoly.presentation.game.session.GameSessionStateCoordinator;
import fi.monopoly.presentation.game.turn.GameTurnFlowCoordinator;
import fi.monopoly.presentation.game.desktop.ui.GameFrameCoordinator;
import fi.monopoly.presentation.game.desktop.ui.GamePrimaryTurnControls;
import fi.monopoly.presentation.session.debt.DebtController;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;

import static fi.monopoly.text.UiTexts.text;

/**
 * Owns the desktop shell adapters that still translate between {@code Game} and the extracted
 * runtime/presentation factories.
 *
 * <p>The goal is to keep {@code Game} as a thin Processing host while the remaining anonymous hook
 * implementations and projected turn-control logic move into one explicit desktop-shell
 * coordinator.</p>
 */
@Slf4j
public final class GameDesktopShellCoordinator {
    private final MonopolyRuntime runtime;
    private final String sessionId;
    private final List<Locale> supportedLocales;
    private final LocalSessionActions localSessionActions;
    private final GameSessionStateCoordinator sessionStateCoordinator;
    private final GameBotTurnControlCoordinator botTurnControlCoordinator;

    public GameDesktopShellCoordinator(
            MonopolyRuntime runtime,
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

    public GameSessionBridgeFactory.Hooks createSessionBridgeHooks(Dependencies dependencies) {
        return new GameSessionBridgeFactory.Hooks() {
            @Override
            public boolean paused() {
                return dependencies.sessionState().paused();
            }

            @Override
            public boolean gameOver() {
                return dependencies.sessionState().gameOver();
            }

            @Override
            public Player winner() {
                return dependencies.sessionState().winner();
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
            public void endTurn() {
                dependencies.endRound(true);
            }

            @Override
            public Player playerById(String playerId) {
                return dependencies.playerById(playerId);
            }

            @Override
            public boolean computerTurn() {
                Player turnPlayer = dependencies.currentTurnPlayer();
                return turnPlayer != null && turnPlayer.isComputerControlled();
            }

            @Override
            public boolean canOpenTrade() {
                return !dependencies.sessionState().gameOver()
                        && !dependencies.popupService().isAnyVisible()
                        && dependencies.debtState() == null;
            }

            @Override
            public Player currentTurnPlayer() {
                return dependencies.currentTurnPlayer();
            }

            @Override
            public List<Player> players() {
                return dependencies.players() != null ? dependencies.players().getPlayers() : List.of();
            }
        };
    }

    public GameRuntimeAssemblyFactory.Hooks createRuntimeAssemblyHooks(Dependencies dependencies) {
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
                GameDesktopShellCoordinator.this.onDebtStateChanged(dependencies);
            }

            @Override
            public void declareWinner(Player winner) {
                GameDesktopShellCoordinator.this.declareWinner(dependencies, winner);
            }

            @Override
            public void debugResetTurnState() {
                GameDesktopShellCoordinator.this.debugResetTurnState(dependencies);
            }

            @Override
            public void restoreNormalTurnControls() {
                GameDesktopShellCoordinator.this.restoreNormalTurnControls(dependencies);
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

    public GamePresentationFactory.Hooks createPresentationHooks(Dependencies dependencies) {
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
                return runtime.app().millis();
            }

            @Override
            public List<Locale> supportedLocales() {
                return supportedLocales;
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
            public fi.monopoly.components.computer.GameView createCurrentGameView() {
                return dependencies.createCurrentGameView();
            }

            @Override
            public fi.monopoly.components.computer.PlayerView createCurrentPlayerView() {
                return dependencies.createCurrentPlayerView();
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
            public void togglePause() {
                GameDesktopShellCoordinator.this.togglePause(dependencies);
            }

            @Override
            public void cycleBotSpeedMode() {
                GameDesktopShellCoordinator.this.cycleBotSpeedMode(dependencies);
            }

            @Override
            public void switchLanguage(Locale locale) {
                GameDesktopShellCoordinator.this.switchLanguage(locale);
            }

            @Override
            public void scheduleNextComputerAction(BotTurnScheduler.DelayKind delayKind) {
                dependencies.scheduleNextComputerAction(delayKind, runtime.app().millis());
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
                return GameDesktopShellCoordinator.this.restoreBotTurnControlsIfNeeded(dependencies);
            }

            @Override
            public Runnable saveSessionAction() {
                return localSessionActions.saveSession();
            }

            @Override
            public Runnable loadSessionAction() {
                return localSessionActions.loadSession();
            }
        };
    }

    public RestoredSessionReattachmentCoordinator.Hooks createRestoredSessionReattachmentHooks(Dependencies dependencies) {
        return new RestoredSessionReattachmentCoordinator.Hooks() {
            @Override
            public Player playerById(String playerId) {
                return dependencies.playerById(playerId);
            }

            @Override
            public boolean gameOver() {
                return dependencies.sessionState().gameOver();
            }

            @Override
            public void refreshLabels() {
                dependencies.refreshLabels();
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
            public void hidePrimaryTurnControls() {
                dependencies.hidePrimaryTurnControls();
            }

            @Override
            public void updateDebtButtons() {
                dependencies.updateDebtButtons();
            }

            @Override
            public void syncTransientPresentationState() {
                dependencies.syncTransientPresentationState();
            }

            @Override
            public void resumeContinuation(fi.monopoly.domain.session.TurnContinuationState continuationState) {
                dependencies.resumeContinuation(continuationState);
            }
        };
    }

    public GameBotTurnControlCoordinator.Hooks createBotTurnControlHooks(Dependencies dependencies) {
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

    public GameFrameCoordinator.FrameHooks createFrameHooks(
            Dependencies dependencies,
            GameBotTurnDriver.Hooks botTurnHooks
    ) {
        return new GameFrameCoordinator.FrameHooks() {
            @Override
            public GameSessionState sessionState() {
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
                GameDesktopShellCoordinator.this.restoreBotTurnControlsIfNeeded(dependencies);
            }

            @Override
            public GameBotTurnDriver.Hooks botTurnHooks() {
                return botTurnHooks;
            }
        };
    }

    public void applyRestoredSessionState(Dependencies dependencies, SessionState restoredSessionState) {
        sessionStateCoordinator.restoreSessionState(
                dependencies.sessionState(),
                restoredSessionState,
                dependencies.sessionApplicationService(),
                dependencies::playerById
        );
    }

    public void initializeSessionPresentation(Dependencies dependencies, SessionState restoredSessionState) {
        sessionStateCoordinator.initializePresentation(
                restoredSessionState,
                dependencies.sessionApplicationService(),
                dependencies.debtController(),
                createRestoredSessionReattachmentHooks(dependencies)
        );
    }

    public void showPersistenceNotice(GameSessionState sessionState, String notice) {
        sessionStateCoordinator.showPersistenceNotice(sessionState, notice, runtime.app().millis());
    }

    public void onDebtStateChanged(Dependencies dependencies) {
        sessionStateCoordinator.onDebtStateChanged(
                dependencies.sessionApplicationService(),
                dependencies::updateDebtButtons,
                () -> restoreBotTurnControlsIfNeeded(dependencies)
        );
    }

    public void togglePause(Dependencies dependencies) {
        if (!sessionStateCoordinator.togglePause(dependencies.sessionState(), dependencies::refreshLabels)) {
            return;
        }
        log.info("Game paused={}", dependencies.sessionState().paused());
    }

    public void cycleBotSpeedMode(Dependencies dependencies) {
        BotTurnScheduler.SpeedMode nextMode = sessionStateCoordinator.cycleBotSpeedMode(
                dependencies.sessionState(),
                dependencies.botTurnScheduler()::markReadyNow,
                runtime.app().millis(),
                dependencies::refreshLabels
        );
        log.info("Bot speed mode={}", nextMode);
    }

    public void switchLanguage(Locale locale) {
        fi.monopoly.text.UiTexts.setLocale(locale);
    }

    public void debugResetTurnState(Dependencies dependencies) {
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

    public void restoreNormalTurnControls(Dependencies dependencies) {
        log.trace("Restoring normal turn controls");
        sessionStateCoordinator.restoreNormalTurnControls(
                dependencies.debtController()::clearDebtState,
                dependencies::showRollDiceControl
        );
    }

    public void declareWinner(Dependencies dependencies, Player winningPlayer) {
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

    public boolean isRollDiceActionAvailable(Dependencies dependencies, Player currentPlayer) {
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

    public boolean isEndTurnActionAvailable(Dependencies dependencies, Player currentPlayer) {
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

    public boolean isProjectedRollDiceActionAvailable(Dependencies dependencies) {
        return isRollDiceActionAvailable(dependencies, dependencies.currentTurnPlayer());
    }

    public boolean isProjectedEndTurnActionAvailable(Dependencies dependencies) {
        return isEndTurnActionAvailable(dependencies, dependencies.currentTurnPlayer());
    }

    public boolean restoreBotTurnControlsIfNeeded(Dependencies dependencies) {
        return botTurnControlCoordinator.restoreControlsIfNeeded(
                createBotTurnControlHooks(dependencies),
                dependencies.currentTurnPlayer()
        );
    }

    public SessionViewFacade createSessionViewFacade(Dependencies dependencies) {
        return new SessionViewFacade(
                runtime,
                dependencies.players(),
                dependencies.board(),
                dependencies::debtState,
                dependencies::retryDebtVisible,
                dependencies::declareBankruptcyVisible,
                player -> isRollDiceActionAvailable(dependencies, player),
                player -> isEndTurnActionAvailable(dependencies, player),
                () -> dependencies.gameSessionQueries().countUnownedProperties(),
                player -> dependencies.gameSessionQueries().calculateBoardDangerScore(player)
        );
    }

    public interface Dependencies {
        GameSessionState sessionState();

        Players players();

        Player currentTurnPlayer();

        Player playerById(String playerId);

        Board board();

        Dices dices();

        Animations animations();

        DebtController debtController();

        DebtState debtState();

        GameTurnFlowCoordinator gameTurnFlowCoordinator();

        GamePrimaryTurnControls gamePrimaryTurnControls();

        GameSessionQueries gameSessionQueries();

        SessionApplicationService sessionApplicationService();

        PopupService popupService();

        BotTurnScheduler botTurnScheduler();

        fi.monopoly.components.computer.GameView createCurrentGameView();

        fi.monopoly.components.computer.PlayerView createCurrentPlayerView();

        void refreshLabels();

        void rollDice();

        void setupDefaultGameState(Board board, Players players);

        void hidePrimaryTurnControls();

        void showRollDiceControl();

        void showEndTurnControl();

        void updateDebtButtons();

        void syncTransientPresentationState();

        void updateLogTurnContext();

        void retryPendingDebtPaymentAction();

        void handlePaymentRequest(
                PaymentRequest request,
                fi.monopoly.domain.session.TurnContinuationState continuationState,
                CallbackAction onResolved
        );

        void endRound(boolean switchTurns);

        void scheduleNextComputerAction(BotTurnScheduler.DelayKind delayKind, int now);

        void resumeContinuation(fi.monopoly.domain.session.TurnContinuationState continuationState);

        void focusPlayer(Player player);

        int goMoneyAmount();

        boolean retryDebtVisible();

        boolean declareBankruptcyVisible();

        boolean endRoundVisible();

        boolean rollDiceVisible();

        MonopolyEventListener eventListener();
    }
}
