package fi.monopoly.presentation.game;

import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.Player;
import fi.monopoly.components.dices.Dices;

import java.util.function.Supplier;

public final class GamePrimaryTurnControls {
    private final Dices dices;
    private final MonopolyButton endRoundButton;
    private final Supplier<Boolean> gameOverSupplier;
    private final Supplier<Player> turnPlayerSupplier;
    private State state = State.NONE;

    public GamePrimaryTurnControls(
            Dices dices,
            MonopolyButton endRoundButton,
            Supplier<Boolean> gameOverSupplier,
            Supplier<Player> turnPlayerSupplier
    ) {
        this.dices = dices;
        this.endRoundButton = endRoundButton;
        this.gameOverSupplier = gameOverSupplier;
        this.turnPlayerSupplier = turnPlayerSupplier;
    }

    public void showRollDiceControl() {
        if (gameOverSupplier.get()) {
            hide();
            return;
        }
        state = State.ROLL_DICE;
        dices.reset();
        Player turnPlayer = turnPlayerSupplier.get();
        if (turnPlayer != null && turnPlayer.isComputerControlled()) {
            dices.hide();
            endRoundButton.hide();
            return;
        }
        dices.show();
        endRoundButton.hide();
    }

    public void showEndTurnControl() {
        if (gameOverSupplier.get()) {
            hide();
            return;
        }
        state = State.END_TURN;
        Player turnPlayer = turnPlayerSupplier.get();
        if (turnPlayer != null && turnPlayer.isComputerControlled()) {
            dices.hide();
            endRoundButton.hide();
            return;
        }
        dices.hide();
        endRoundButton.show();
    }

    public void hide() {
        state = State.NONE;
        dices.hide();
        endRoundButton.hide();
    }

    public boolean isRollDiceActionAvailable(boolean popupVisible, boolean debtActive, Player currentPlayer) {
        if (currentPlayer == null || popupVisible || debtActive) {
            return false;
        }
        return state == State.ROLL_DICE;
    }

    public boolean isEndTurnActionAvailable(boolean popupVisible, boolean debtActive, Player currentPlayer) {
        if (currentPlayer == null || popupVisible || debtActive) {
            return false;
        }
        return state == State.END_TURN;
    }

    public void enforceInvariant(boolean debtActive, Runnable onViolation) {
        if (debtActive) {
            hide();
            return;
        }
        if (endRoundButton.isVisible() && dices.isVisible()) {
            onViolation.run();
            dices.hide();
        }
    }

    private enum State {
        NONE,
        ROLL_DICE,
        END_TURN
    }
}
