package fi.monopoly.components.turn;

import fi.monopoly.components.Player;

public record PayRentEffect(Player fromPlayer, Player toPlayer, int amount, String message) implements TurnEffect {
}
