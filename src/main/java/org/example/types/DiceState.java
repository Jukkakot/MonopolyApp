package org.example.types;

public enum DiceState {
    NOREROLL, REROLL, JAIL, DEBUG_REROLL;

    public static DiceState valueOf(int pairCount) {
        if (pairCount == 0) {
            return NOREROLL;
        } else if (pairCount <= 3) {
            return REROLL;
        } else {
            return JAIL;
        }
    }
}
