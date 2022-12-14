package org.example.components.spots;

import javafx.scene.paint.Color;
import lombok.Getter;
import org.example.components.Token;
import org.example.images.ImageFactory;
import org.example.images.StreetSpotImage;
import org.example.utils.Coordinates;
import org.example.MonopolyApp;
import org.example.components.Player;
import org.example.images.Image;
import org.example.images.SpotImage;
import org.example.types.SpotTypeEnum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Spot implements SpotInterface {
    public static final int spotW = 996 / 12;
    public static final int spotH = (int) (spotW * 1.5);
    private final SpotImage image;
    @Getter
    protected SpotTypeEnum spotTypeEnum;
    private final MonopolyApp p;
    @Getter
    protected final String name;
    List<Player> players = new ArrayList<>();
    public static final List<Coordinates> tokenSpots = Arrays.asList(new Coordinates(0, 0), new Coordinates(-Token.TOKEN_RADIUS, 0), new Coordinates(Token.TOKEN_RADIUS, 0),
            new Coordinates(0, Token.TOKEN_RADIUS), new Coordinates(-Token.TOKEN_RADIUS, Token.TOKEN_RADIUS), new Coordinates(Token.TOKEN_RADIUS, Token.TOKEN_RADIUS));

    public Spot(MonopolyApp p, SpotImage spotImage) {
        this.p = p;
        this.image = spotImage;
        this.spotTypeEnum = spotImage.getSpotTypeEnum();
        this.image.setCoords(getTokenCoords());

        name = spotTypeEnum.getProperty("name");
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

    public Coordinates getTokenCoords(Player player) {
        int index = players.stream().filter(p -> !p.equals(player)).toList().size();
        Coordinates tokenSpot = tokenSpots.get(index % tokenSpots.size());
        return new Coordinates(image.getCoords().x() + tokenSpot.x(), image.getCoords().y() + tokenSpot.y(), image.getCoords().rotation());
    }

    public Coordinates getTokenCoords() {
        Coordinates tokenSpot = tokenSpots.get(players.size() % tokenSpots.size());
        return new Coordinates(image.getCoords().x() + tokenSpot.x(), image.getCoords().y() + tokenSpot.y(), image.getCoords().rotation());
    }


    public Coordinates getCoords() {
        return image.getCoords();
    }

    public void draw(Coordinates c) {
        if (image != null) {
            image.draw(c);
        } else {
            Image.defaultDraw(p, image.getCoords(), 10, Color.BLACK);
        }
        players.forEach(p -> p.draw(c));
//        p.push();
//        p.fill(0);
//        p.textSize(30);
//        p.text(players.size(),x, y- Token.TOKEN_RADIUS);
//        p.pop();
    }
    public void drawDeed(Coordinates c) {
        SpotImage newImg = ImageFactory.getImage(p, image.getCoords(), spotTypeEnum);
        newImg.draw(c, false);
    }
    public void draw() {
        this.draw(null);
    }

    @Override
    public String getPopupText(Player p) {
        return "Default spot text, hello " + p.getName();
    }
}
