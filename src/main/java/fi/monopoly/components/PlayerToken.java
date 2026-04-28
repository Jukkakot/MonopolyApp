package fi.monopoly.components;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.images.Image;
import fi.monopoly.utils.Coordinates;
import javafx.scene.paint.Color;
import lombok.Getter;

public class PlayerToken extends Image {
    public static final int TOKEN_RADIUS = 35;
    public static final int PLAYER_TOKEN_BIG_DIAMETER = (int) (TOKEN_RADIUS * 1.5);
    @Getter
    protected final Color color;
    @Getter
    protected final MonopolyRuntime runtime;
    @Getter
    protected Spot spot;

    public PlayerToken(MonopolyRuntime runtime, Spot spot, Color color) {
        super(runtime, spot.getTokenCoords(), "Token.png", PlayerToken.TOKEN_RADIUS, PlayerToken.TOKEN_RADIUS);
        this.runtime = runtime;
        this.color = color;
        this.spot = spot;
    }

    protected PlayerToken(MonopolyRuntime runtime, Coordinates coords, Color color) {
        super(runtime, coords, "Token.png", PlayerToken.TOKEN_RADIUS, PlayerToken.TOKEN_RADIUS);
        this.runtime = runtime;
        this.color = color;
        this.spot = null;
    }

    @Override
    public void draw(Coordinates c) {
        draw(c, color);
    }

    public void draw() {
        draw(null);
    }
}
