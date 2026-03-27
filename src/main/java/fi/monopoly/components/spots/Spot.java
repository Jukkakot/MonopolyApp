package fi.monopoly.components.spots;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.GameState;
import fi.monopoly.components.Player;
import fi.monopoly.components.PlayerToken;
import fi.monopoly.images.AbstractClickable;
import fi.monopoly.images.Image;
import fi.monopoly.images.SpotImage;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.TurnResult;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.MonopolyUtils;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@ToString(onlyExplicitlyIncluded = true)
public abstract class Spot extends AbstractClickable {
    public static final float SPOT_W = 996f / 12f;
    public static final float SPOT_H = SPOT_W * 1.5f;
    @Getter
    protected final SpotType spotType;
    Set<Image> playersOnSpot = new HashSet<>();
    private static final List<Coordinates> TOKEN_COORDS = Arrays.asList(new Coordinates(0, 0), new Coordinates(-PlayerToken.TOKEN_RADIUS, 0), new Coordinates(PlayerToken.TOKEN_RADIUS, 0),
            new Coordinates(0, PlayerToken.TOKEN_RADIUS), new Coordinates(-PlayerToken.TOKEN_RADIUS, PlayerToken.TOKEN_RADIUS), new Coordinates(PlayerToken.TOKEN_RADIUS, PlayerToken.TOKEN_RADIUS));

    public Spot(SpotImage image) {
        super(image);
        this.spotType = image.getSpotType();
    }

    public String getName() {
        String spotName = spotType.getStringProperty("name");
        return spotName.isBlank() ? spotType.name() : MonopolyUtils.parseIllegalCharacters(spotName);
    }

    public void addPlayer(Player p) {
        playersOnSpot.add(p);
    }

    public void removePlayer(Player p) {
        playersOnSpot.remove(p);
    }

    public Coordinates getTokenCoords(Player player) {
        int index = playersOnSpot.stream().filter(p -> !p.equals(player)).toList().size();
        Coordinates tokenSpot = TOKEN_COORDS.get(index % TOKEN_COORDS.size());
        return image.getCoords().move(tokenSpot);
    }

    public Coordinates getTokenCoords() {
        Coordinates tokenSpot = TOKEN_COORDS.get(playersOnSpot.size() % TOKEN_COORDS.size());
        return image.getCoords().move(tokenSpot);
    }

    public TurnResult handleTurn(GameState gameState, CallbackAction callbackAction) {
        callbackAction.doAction();
        return null;
    }

    //TODO move elsewhere?
    protected void updateMoney(Player player, Integer amount, String popupText, CallbackAction callbackAction) {
        runtime.popupService().show(popupText, () -> {
            if (player.addMoney(amount)) {
                callbackAction.doAction();
            } else {
                //TODO what if not enough money
                callbackAction.doAction();
            }
        });
    }

    public boolean isSpotType(SpotType spotType) {
        return this.spotType.equals(spotType);
    }
}
