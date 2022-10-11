package org.example.types;
import javafx.scene.paint.Color;

import static javafx.scene.paint.Color.color;
import static org.example.Utils.toFloat;

public enum StreetType {

    BROWN(Color.SADDLEBROWN),
    LIGHT_BLUE(toColor(90,200,250)),
    PURPLE(toColor(180, 39, 219)),
    ORANGE(toColor(240, 169, 26)),
    RED(Color.RED),
    YELLOW(Color.YELLOW),
    GREEN(Color.GREEN),
    DARK_BLUE(Color.BLUE),
    RAILROAD(Color.BLACK),
    UTILITY(Color.PINK);

    public final Color color;
    StreetType(Color color) {
        this.color = color;
    }
    private static Color toColor(int r, int g, int b) {
        return color(toFloat(r), toFloat(g), toFloat(b));
    }
}

