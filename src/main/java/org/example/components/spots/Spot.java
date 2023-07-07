package org.example.components.spots;

import lombok.Getter;
import lombok.ToString;
import org.example.components.CallbackAction;
import org.example.components.GameState;
import org.example.components.Player;
import org.example.components.PlayerToken;
import org.example.images.Clickable;
import org.example.images.Image;
import org.example.images.SpotImage;
import org.example.types.SpotType;
import org.example.types.TurnResult;
import org.example.utils.Coordinates;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ToString(onlyExplicitlyIncluded = true)
public abstract class Spot extends Clickable {
    public static final float SPOT_W = 996f / 12f;
    public static final float SPOT_H = SPOT_W * 1.5f;
    @Getter
    protected final SpotType spotType;
    @Getter
    protected final String name;
    Set<Image> players = new HashSet<>();
    private static final List<Coordinates> TOKEN_COORDS = Arrays.asList(new Coordinates(0, 0), new Coordinates(-PlayerToken.TOKEN_RADIUS, 0), new Coordinates(PlayerToken.TOKEN_RADIUS, 0),
            new Coordinates(0, PlayerToken.TOKEN_RADIUS), new Coordinates(-PlayerToken.TOKEN_RADIUS, PlayerToken.TOKEN_RADIUS), new Coordinates(PlayerToken.TOKEN_RADIUS, PlayerToken.TOKEN_RADIUS));

    public Spot(SpotImage image) {
        super(image);
        this.spotType = image.getSpotType();
        this.name = spotType.getStringProperty("name");
    }

    public void addPlayer(Player p) {
        players.add(p);
    }

    public void removePlayer(Player p) {
        players.remove(p);
    }

    public Coordinates getTokenCoords(Player player) {
        int index = players.stream().filter(p -> !p.equals(player)).toList().size();
        Coordinates tokenSpot = TOKEN_COORDS.get(index % TOKEN_COORDS.size());
        return image.getCoords().move(tokenSpot);
    }

    public Coordinates getTokenCoords() {
        Coordinates tokenSpot = TOKEN_COORDS.get(players.size() % TOKEN_COORDS.size());
        return image.getCoords().move(tokenSpot);
    }

    public abstract TurnResult handleTurn(GameState gameState, CallbackAction callbackAction);

    @Override
    public void onClick() {
        //Buying properties if players turn
        System.out.println("Clicked spot " + this);
    }
}
