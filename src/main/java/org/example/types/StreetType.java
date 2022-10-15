package org.example.types;
import javafx.scene.paint.Color;

import static javafx.scene.paint.Color.color;
import static org.example.utils.Utils.toFloat;

public enum StreetType {

    BROWN(Color.SADDLEBROWN),
    LIGHT_BLUE(toColor(170,224,250)),
    PURPLE(toColor(218, 59, 151)),
    ORANGE(toColor(247, 148, 29)),
    RED(Color.RED),
    YELLOW(Color.YELLOW),
    GREEN(Color.GREEN),
    DARK_BLUE(Color.BLUE),
    RAILROAD(Color.BLACK, "Railroad.png"),
    UTILITY1(null, "Utility1.png"),UTILITY2(null, "Utility2.png"),
    CHANCE1(null, "ChancePink.png"),CHANCE2(null, "ChanceBlue.png"), CHANCE3(null, "ChanceOrange.png"),
    COMMUNITY(null, "Community.png"),
    TAX1(null, "Tax1.png"), TAX2(null, "Tax2.png"),
    CORNER1(null, "Corner1.png"), CORNER2(null, "Corner2.png"), CORNER3(null, "Corner3.png"), CORNER4(null, "Corner4.png");

    public final Color color;
    public final String imgName;
    StreetType(Color color) {
        this(color,null);
    }
    StreetType(Color color, String imgName) {
        this.color = color;
        this.imgName = imgName;
    }
    private static Color toColor(int r, int g, int b) {
        return color(toFloat(r), toFloat(g), toFloat(b));
    }
}

