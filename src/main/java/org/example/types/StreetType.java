package org.example.types;
import javafx.scene.paint.Color;

import static javafx.scene.paint.Color.color;
import static org.example.Utils.toFloat;

public enum StreetType {

    BROWN(Color.BROWN),
    LIGHT_BLUE(color(toFloat(90),toFloat(200),toFloat(250))),
    PINK(Color.PINK),
    ORANGE(Color.ORANGE),
    RED(Color.RED),
    YELLOW(Color.YELLOW),
    GREEN(Color.GREEN),
    DARK_BLUE(color(toFloat(255),toFloat(51),toFloat(51))),
    RAILROAD(null),
    UTILITY(null),
    CORNER(null);

    public final Color color;
    StreetType(Color color) {
        this.color = color;
    }
}
