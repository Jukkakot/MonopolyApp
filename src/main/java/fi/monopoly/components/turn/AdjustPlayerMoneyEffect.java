package fi.monopoly.components.turn;

import fi.monopoly.components.Player;

public record AdjustPlayerMoneyEffect(Player player, int amount, String message) implements TurnEffect {
}
