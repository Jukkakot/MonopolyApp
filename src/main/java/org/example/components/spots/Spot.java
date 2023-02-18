package org.example.components.spots;

import lombok.Getter;
import org.example.components.Player;
import org.example.components.Token;
import org.example.images.SpotImage;
import org.example.types.SpotType;
import org.example.utils.Coordinates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Spot {
    public static final int spotW = 996 / 12;
    public static final int spotH = (int) (spotW * 1.5);
    private final SpotImage image;
    @Getter
    protected SpotType spotType;
    @Getter
    protected final String name;
    List<Player> players = new ArrayList<>();
    public static final List<Coordinates> tokenSpots = Arrays.asList(new Coordinates(0, 0), new Coordinates(-Token.TOKEN_RADIUS, 0), new Coordinates(Token.TOKEN_RADIUS, 0),
            new Coordinates(0, Token.TOKEN_RADIUS), new Coordinates(-Token.TOKEN_RADIUS, Token.TOKEN_RADIUS), new Coordinates(Token.TOKEN_RADIUS, Token.TOKEN_RADIUS));

    public Spot(SpotImage spotImage) {
        this.image = spotImage;
        this.spotType = spotImage.getSpotType();
        this.image.setCoords(getTokenCoords());

        name = spotType.getProperty("name");
    }

    public void addPlayer(Player p) {
        if (!players.contains(p)) {
            players.add(p);
        }
    }

    public void removePlayer(Player p) {
        players.remove(p);
    }

    public Coordinates getTokenCoords(Player player) {
        int index = players.stream().filter(p -> !p.equals(player)).toList().size();
        Coordinates tokenSpot = tokenSpots.get(index % tokenSpots.size());
        return image.getCoords().move(tokenSpot);
    }

    public Coordinates getTokenCoords() {
        Coordinates tokenSpot = tokenSpots.get(players.size() % tokenSpots.size());
        return image.getCoords().move(tokenSpot);
    }


    public Coordinates getCoords() {
        return image.getCoords();
    }

    public void draw(Coordinates c) {
        image.draw(c);
        players.forEach(p -> p.draw(c));
    }
}
