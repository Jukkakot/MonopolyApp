package org.example.types;

import javafx.scene.paint.Color;

import static org.example.utils.Utils.toColor;

public enum StreetType {

    BROWN(PlaceType.STREET, Color.SADDLEBROWN),
    LIGHT_BLUE(PlaceType.STREET, toColor(170, 224, 250)),
    PURPLE(PlaceType.STREET, toColor(218, 59, 151)),
    ORANGE(PlaceType.STREET, toColor(247, 148, 29)),
    RED(PlaceType.STREET, Color.RED),
    YELLOW(PlaceType.STREET, Color.YELLOW),
    GREEN(PlaceType.STREET, Color.GREEN),
    DARK_BLUE(PlaceType.STREET, Color.BLUE),
    RAILROAD(PlaceType.RAILROAD, "Railroad.png"),
    UTILITY(PlaceType.UTILITY, "Utility.png"),
    CHANCE(PlaceType.PICK_CARD, "Chance.png"),
    COMMUNITY(PlaceType.PICK_CARD, "Community.png"),
    TAX(PlaceType.TAX, "Tax.png"),
    CORNER(PlaceType.CORNER, "Corner.png");

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
}

