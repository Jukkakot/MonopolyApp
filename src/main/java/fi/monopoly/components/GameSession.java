package fi.monopoly.components;

import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.presentation.session.debt.DebtActionDispatcher;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * Shared per-game runtime state that used to live as global static fields on
 * {@link Game}. This keeps the state tied to the active runtime instead of the
 * Game type itself.
 */
public final class GameSession {
    private final Players players;
    private final Dices dices;
    private final Animations animations;
    private BooleanSupplier debtResolutionActiveSupplier = () -> false;
    private BooleanSupplier gameOverActiveSupplier = () -> false;
    private IntSupplier goMoneyAmountSupplier = () -> 200;
    private DebtActionDispatcher debtActionDispatcher;

    public GameSession(Players players, Dices dices, Animations animations) {
        this.players = players;
        this.dices = dices;
        this.animations = animations;
    }

    public GameSession withStateSuppliers(
            BooleanSupplier debtResolutionActiveSupplier,
            BooleanSupplier gameOverActiveSupplier,
            IntSupplier goMoneyAmountSupplier
    ) {
        this.debtResolutionActiveSupplier = debtResolutionActiveSupplier;
        this.gameOverActiveSupplier = gameOverActiveSupplier;
        this.goMoneyAmountSupplier = goMoneyAmountSupplier;
        return this;
    }

    public Players players() {
        return players;
    }

    public Dices dices() {
        return dices;
    }

    public Animations animations() {
        return animations;
    }

    public boolean isDebtResolutionActive() {
        return debtResolutionActiveSupplier.getAsBoolean();
    }

    public boolean isGameOverActive() {
        return gameOverActiveSupplier.getAsBoolean();
    }

    public int goMoneyAmount() {
        return goMoneyAmountSupplier.getAsInt();
    }

    public GameSession withDebtActionDispatcher(DebtActionDispatcher debtActionDispatcher) {
        this.debtActionDispatcher = debtActionDispatcher;
        return this;
    }

    public DebtActionDispatcher debtActionDispatcher() {
        return debtActionDispatcher;
    }
}
