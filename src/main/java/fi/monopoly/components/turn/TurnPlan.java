package fi.monopoly.components.turn;

import java.util.List;

public record TurnPlan(TurnPhase phase, List<TurnEffect> effects) {
    public static TurnPlan of(TurnPhase phase, TurnEffect... effects) {
        return new TurnPlan(phase, List.of(effects));
    }
}
