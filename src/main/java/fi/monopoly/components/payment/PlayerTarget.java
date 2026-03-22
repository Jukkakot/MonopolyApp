package fi.monopoly.components.payment;

import fi.monopoly.components.Player;

public record PlayerTarget(Player player) implements PaymentTarget {
    @Override
    public String getDisplayName() {
        return player.getName();
    }
}
