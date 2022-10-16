package org.example.types;

import javafx.scene.paint.Color;

import static javafx.scene.paint.Color.color;
import static org.example.utils.Utils.toFloat;

public enum StreetType {

    BROWN(PlaceType.STREET, Color.SADDLEBROWN),
    LIGHT_BLUE(PlaceType.STREET, toColor(170, 224, 250)),
    PURPLE(PlaceType.STREET, toColor(218, 59, 151)),
    ORANGE(PlaceType.STREET, toColor(247, 148, 29)),
    RED(PlaceType.STREET, Color.RED),
    YELLOW(PlaceType.STREET, Color.YELLOW),
    GREEN(PlaceType.STREET, Color.GREEN),
    DARK_BLUE(PlaceType.STREET, Color.BLUE),
    RAILROAD(PlaceType.RAILROAD, Color.BLACK, "Railroad.png"),
    UTILITY1(PlaceType.UTILITY, "Utility1.png"), UTILITY2(PlaceType.UTILITY, "Utility2.png"),
    CHANCE1(PlaceType.CHANCE_OR_COMMUNITY, "ChancePink.png"), CHANCE2(PlaceType.CHANCE_OR_COMMUNITY, "ChanceBlue.png"), CHANCE3(PlaceType.CHANCE_OR_COMMUNITY, "ChanceOrange.png"),
    COMMUNITY(PlaceType.CHANCE_OR_COMMUNITY, "Community.png"),
    TAX1(PlaceType.TAX, "Tax1.png"), TAX2(PlaceType.TAX, "Tax2.png"),
    CORNER1(PlaceType.CORNER, "Corner1.png"), CORNER2(PlaceType.CORNER, "Corner2.png"), CORNER3(PlaceType.CORNER, "Corner3.png"), CORNER4(PlaceType.CORNER, "Corner4.png");

    public final Color color;
    public final String imgName;
    public final PlaceType placeType;

    StreetType(PlaceType pt, Color color) {
        this(pt, color, null);
    }

    StreetType(PlaceType pt, String imgName) {
        this(pt, null, imgName);
    }

    StreetType(PlaceType pt, Color color, String imgName) {
        this.color = color;
        this.imgName = imgName;
        this.placeType = pt;
    }

    private static Color toColor(int r, int g, int b) {
        return color(toFloat(r), toFloat(g), toFloat(b));
    }
}

