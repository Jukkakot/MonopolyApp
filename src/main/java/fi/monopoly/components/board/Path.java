package fi.monopoly.components.board;

import fi.monopoly.components.Player;
import fi.monopoly.components.popup.Popup;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.types.SpotType;
import fi.monopoly.utils.Coordinates;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static fi.monopoly.components.Game.GO_MONEY_AMOUNT;

@Slf4j
public class Path {
    final List<Spot> spots;
    final Player player;
    @Getter
    final Spot lastSpot;
    private boolean hasShownPopupForGoSpot = false;

    public Path(List<Spot> spots, Player player) {
        this.spots = spots;
        this.player = player;
        this.lastSpot = spots.get(spots.size() - 1);
    }

    public void removePrevious() {
        if (spots == null || spots.isEmpty()) {
            return;
        }
        spots.remove(0);
    }

    public boolean isEmpty() {
        return spots.isEmpty();
    }

    public Coordinates getLast() {
        return this.lastSpot.getTokenCoords(player);
    }

    public Coordinates getNext() {
        Spot nextSpot = getNextSpot();
        if (nextSpot.isSpotType(SpotType.GO_SPOT) && !hasShownPopupForGoSpot) {
            Popup.show("Player gets M" + GO_MONEY_AMOUNT, () -> {
                player.addMoney(GO_MONEY_AMOUNT);
            });
            hasShownPopupForGoSpot = true;
        }
        return nextSpot.getTokenCoords(player);
    }

    private Spot getNextSpot() {
        return spots.get(0);
    }
}
