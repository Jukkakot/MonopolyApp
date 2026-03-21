package fi.monopoly.components;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.utils.Coordinates;
import javafx.scene.paint.Color;
import lombok.Getter;
import fi.monopoly.images.Image;

public class PlayerToken extends Image {
    @Getter
    protected final Color color;
    @Getter
    protected Spot spot;
    @Getter
    protected final MonopolyRuntime runtime;
    public static final int TOKEN_RADIUS = 35;
    public static final int PLAYER_TOKEN_BIG_DIAMETER = (int) (TOKEN_RADIUS * 1.5);

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
