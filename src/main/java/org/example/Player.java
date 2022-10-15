package org.example;

import lombok.Getter;
import org.example.components.Spot;
import org.example.components.Token;
import org.example.utils.Coordinates;

public class Player implements Drawable {
    private final int id;
    private final String name;
    @Getter
    private final Token token;
    private Integer money;
    @Getter
    private final int turnNumber;
    public Player(int id, String name, Token token, int turnNumber) {
        this.id = id;
        this.name = name;
        this.token = token;
        this.money = 2000;
        this.turnNumber = turnNumber;
    }
    public void moveToken(Spot spot) {
        token.getSpot().removePlayer(this);
        spot.addPlayer(this);
        token.setSpot(spot);
    }
    public void draw(Coordinates coords) {
        token.draw(coords);
    }
    @Override
    public void draw(float rotate) {
        token.draw(rotate);
    }
    @Override
    public void draw() {
        draw(0);
    }
}
