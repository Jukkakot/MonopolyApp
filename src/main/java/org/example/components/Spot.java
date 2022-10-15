package org.example.components;

import javafx.scene.paint.Color;
import lombok.Getter;
import org.example.utils.Coordinates;
import org.example.MonopolyApp;
import org.example.Player;
import org.example.images.IconSpotImage;
import org.example.images.Image;
import org.example.images.PropertySpotImage;
import org.example.images.SpotImage;
import org.example.types.SpotType;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class Spot extends MonopolyApp {
    public static final int spotW = 996 / 12;
    public static final int spotH = (int) (spotW * 1.5);
    @Getter
    protected final int x;
    @Getter
    protected final int y;
    @Getter
    protected final float rotation;
    private SpotImage image;
    protected final PApplet p;
    List<Player> players = new ArrayList<>();
    public static final List<Coordinates> tokenSpots = Arrays.asList(new Coordinates(0, 0), new Coordinates(-Token.TOKEN_RADIUS, 0) , new Coordinates(Token.TOKEN_RADIUS, 0),
            new Coordinates(0, Token.TOKEN_RADIUS), new Coordinates(-Token.TOKEN_RADIUS, Token.TOKEN_RADIUS), new Coordinates(Token.TOKEN_RADIUS, Token.TOKEN_RADIUS));

    public Spot(PApplet p, final int x, final int y, final float rotation) {
        this.p = p;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
    }

    public Player addPlayer(Player p) {
        if (!players.contains(p)) {
            players.add(p);
        }
        return p;
    }

    public void removePlayer(Player p) {
        players.remove(p);
    }

    public void drawPlayers() {
        players.forEach(p -> {
            Coordinates coords = tokenSpots.get(this.players.indexOf(p));
            p.draw(new Coordinates(coords.x() + x, coords.y() + y));
        });
    }
    public Coordinates getCoordinates(Player player) {
        int index = players.stream().filter(p -> !p.equals(player)).toList().size();
        Coordinates tokenSpot = tokenSpots.get(index % tokenSpots.size());
        return new Coordinates(x + tokenSpot.x(), y + tokenSpot.y());
    }
    public Coordinates getCoordinates() {
        return getCoordinates(null);
    }
    public void draw(float rotate) {

        if (image != null) {
            image.draw(rotate);
        } else {
            Image.defaultDraw(p, new Coordinates(x, y), 10, Color.BLACK);
        }
//        p.push();
//        p.fill(0);
//        p.textSize(30);
//        p.text(players.size(),x, y- Token.TOKEN_RADIUS);
//        p.pop();
    }

    public void draw() {
        draw(rotation);
    }

    public void setImage(SpotType spotType) {
        if (spotType.name().startsWith("CORNER")) {
            this.image = new IconSpotImage(p, this, spotType, true);
        } else if (spotType.streetType == null) {
            this.image = new SpotImage(p, this);
        } else if (spotType.streetType.imgName != null) {
            this.image = new IconSpotImage(p, this, spotType);
        } else {
            this.image = new PropertySpotImage(p, this, spotType);
        }
    }
}
