package org.example.components;

import jogamp.opengl.glu.nurbs.Property;
import lombok.Getter;
import org.example.components.spots.PropertySpot;
import org.example.components.spots.Spot;
import org.example.utils.Coordinates;

import java.util.List;

public class Player implements Drawable {
    @Getter
    private final int id;
    private final String name;
    @Getter
    private final Token token;
    private Integer money;
    @Getter
    private final int turnNumber;
    @Getter
    private final Deeds deeds;
    public Player(int id, String name, Token token, int turnNumber) {
        this.id = id;
        this.name = name;
        this.token = token;
        this.money = 2000;
        this.turnNumber = turnNumber;
        deeds = new Deeds();
    }
    public void moveToken(Spot spot) {
        token.getSpot().removePlayer(this);
        spot.addPlayer(this);
        token.setSpot(spot);
        if(spot instanceof PropertySpot) {
            addDeed((PropertySpot) spot);
        }
    }
    public String getName() {
        return name.replace("ö","o").replace("ä","a").replace("Ö","O").replace("Ä","A");
    }
    public void draw(Coordinates coords) {
        token.draw(coords);
    }

    @Override
    public Coordinates getCoords() {
        return token.getCoords();
    }

    @Override
    public void setCoords(Coordinates coords) {
        token.setCoords(coords);
    }

    @Override
    public void draw() {
        draw(null);
    }

    public void addDeed(PropertySpot ps) {
        if(ps.getOwnerPlayer() == null) {
            ps.setOwnerPlayer(this);
            deeds.addDeed(ps);
        }
    }
    public List<PropertySpot> getAllDeeds() {
        return deeds.getAllDeeds();
    }
}
