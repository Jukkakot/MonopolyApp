package fi.monopoly.presentation.game;

import fi.monopoly.MonopolyApp;
import fi.monopoly.components.DebugController;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.dices.DiceValue;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.payment.DebtController;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.presentation.session.trade.TradeController;
import fi.monopoly.text.UiTexts;
import fi.monopoly.types.DiceState;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public final class GameUiHooksAdapter implements GameUiController.Hooks {
    private final Board board;
    private final Dices dices;
    private final DebtController debtController;
    private final TradeController tradeController;
    private final DebugController debugController;
    private final GameTurnFlowCoordinator gameTurnFlowCoordinator;
    private final Runnable togglePauseAction;
    private final Runnable cycleBotSpeedModeAction;
    private final Runnable payDebtAction;
    private final Runnable declareBankruptcyAction;
    private final Runnable finishAllAnimationsAction;
    private final BooleanSupplier gameOverSupplier;
    private final BooleanSupplier popupVisibleSupplier;
    private final BooleanSupplier canEndTurnSupplier;
    private final Consumer<Locale> languageSwitcher;
    private final Runnable saveSessionAction;
    private final Runnable loadSessionAction;

    public GameUiHooksAdapter(
            Board board,
            Dices dices,
            DebtController debtController,
            TradeController tradeController,
            DebugController debugController,
            GameTurnFlowCoordinator gameTurnFlowCoordinator,
            Runnable togglePauseAction,
            Runnable cycleBotSpeedModeAction,
            Runnable payDebtAction,
            Runnable declareBankruptcyAction,
            Runnable finishAllAnimationsAction,
            BooleanSupplier gameOverSupplier,
            BooleanSupplier popupVisibleSupplier,
            BooleanSupplier canEndTurnSupplier,
            Consumer<Locale> switchLanguageAction,
            Runnable saveSessionAction,
            Runnable loadSessionAction
    ) {
        this.board = board;
        this.dices = dices;
        this.debtController = debtController;
        this.tradeController = tradeController;
        this.debugController = debugController;
        this.gameTurnFlowCoordinator = gameTurnFlowCoordinator;
        this.togglePauseAction = togglePauseAction;
        this.cycleBotSpeedModeAction = cycleBotSpeedModeAction;
        this.payDebtAction = payDebtAction;
        this.declareBankruptcyAction = declareBankruptcyAction;
        this.finishAllAnimationsAction = finishAllAnimationsAction;
        this.gameOverSupplier = gameOverSupplier;
        this.popupVisibleSupplier = popupVisibleSupplier;
        this.canEndTurnSupplier = canEndTurnSupplier;
        this.languageSwitcher = switchLanguageAction;
        this.saveSessionAction = saveSessionAction;
        this.loadSessionAction = loadSessionAction;
    }

    @Override
    public boolean gameOver() {
        return gameOverSupplier.getAsBoolean();
    }

    @Override
    public boolean popupVisible() {
        return popupVisibleSupplier.getAsBoolean();
    }

    @Override
    public boolean debtActive() {
        return debtController.debtState() != null;
    }

    @Override
    public boolean canEndTurn() {
        return canEndTurnSupplier.getAsBoolean();
    }

    @Override
    public void togglePause() {
        togglePauseAction.run();
    }

    @Override
    public void cycleBotSpeedMode() {
        cycleBotSpeedModeAction.run();
    }

    @Override
    public void openTradeMenu() {
        tradeController.openTradeMenu();
    }

    @Override
    public void payDebt() {
        payDebtAction.run();
    }

    @Override
    public void declareBankruptcy() {
        declareBankruptcyAction.run();
    }

    @Override
    public void endRound() {
        if (!popupVisible() && debtController.debtState() == null) {
            gameTurnFlowCoordinator.endRound(true);
        }
    }

    @Override
    public void openGodModeMenu() {
        debugController.openGodModeMenu();
    }

    @Override
    public void finishAllAnimations() {
        finishAllAnimationsAction.run();
    }

    @Override
    public void toggleSkipAnimations() {
        MonopolyApp.SKIP_ANNIMATIONS = !MonopolyApp.SKIP_ANNIMATIONS;
        log.debug("Skip animations: {}", MonopolyApp.SKIP_ANNIMATIONS);
    }

    @Override
    public Spot hoveredSpot() {
        return board.getHoveredSpot();
    }

    @Override
    public boolean debugFlyToHoveredSpot(Spot hoveredSpot) {
        if (!dices.isVisible()) {
            return false;
        }
        dices.setValue(new DiceValue(DiceState.DEBUG_REROLL, 8));
        boolean played = gameTurnFlowCoordinator.playDebugRound(hoveredSpot, DiceState.DEBUG_REROLL);
        if (played) {
            dices.hide();
        }
        return played;
    }

    @Override
    public Locale currentLocale() {
        return UiTexts.getLocale();
    }

    @Override
    public void switchLanguage(Locale locale) {
        languageSwitcher.accept(locale);
    }

    @Override
    public void saveSession() {
        saveSessionAction.run();
    }

    @Override
    public void loadSession() {
        loadSessionAction.run();
    }
}
