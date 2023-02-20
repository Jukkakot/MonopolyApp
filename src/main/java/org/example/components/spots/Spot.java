package org.example.components.spots;

import lombok.Getter;
import org.example.components.Drawable;
import org.example.components.Player;
import org.example.components.PlayerToken;
import org.example.images.Image;
import org.example.images.SpotImage;
import org.example.types.SpotType;
import org.example.utils.Coordinates;

import java.util.*;


public class Spot implements Drawable {
    public static final float SPOT_W = 996f / 12f;
    public static final float SPOT_H = SPOT_W * 1.5f;
    private final SpotImage image;
    @Getter
    protected SpotType spotType;
    @Getter
    protected final String name;
    Set<Image> players = new HashSet<>();
    private static final List<Coordinates> TOKEN_COORDS = Arrays.asList(new Coordinates(0, 0), new Coordinates(-PlayerToken.TOKEN_RADIUS, 0), new Coordinates(PlayerToken.TOKEN_RADIUS, 0),
            new Coordinates(0, PlayerToken.TOKEN_RADIUS), new Coordinates(-PlayerToken.TOKEN_RADIUS, PlayerToken.TOKEN_RADIUS), new Coordinates(PlayerToken.TOKEN_RADIUS, PlayerToken.TOKEN_RADIUS));

    public Spot(SpotImage spotImage) {
        this.image = spotImage;
        this.spotType = spotImage.getSpotType();
        this.image.setCoords(getTokenCoords());

        name = spotType.getProperty("name");
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


    public Coordinates getCoords() {
        return image.getCoords();
    }

    @Override
    public void setCoords(Coordinates coords) {
        image.setCoords(coords);
    }

    @Override
    public void draw(Coordinates c) {
        image.draw(c);
    }

    public boolean isHovered() {
        return image.isHovered();
    }
}
