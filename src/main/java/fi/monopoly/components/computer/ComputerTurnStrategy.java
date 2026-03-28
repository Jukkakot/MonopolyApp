package fi.monopoly.components.computer;

import fi.monopoly.components.Player;

public interface ComputerTurnStrategy {
    boolean takeStep(ComputerTurnContext context, Player player);
}
