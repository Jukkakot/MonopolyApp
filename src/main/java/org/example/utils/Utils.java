package org.example.utils;

import javafx.scene.paint.Color;
import org.example.MonopolyApp;

import static javafx.scene.paint.Color.color;
import static processing.core.PApplet.map;

public class Utils {
    public static float toFloat(int value) {
        return map(value, 0, 255, 0, 1);
    }

    public static float toFloat(double value) {
        return map((float) value, 0, 1, 0, 255);
    }

    public static int toColor(Color color) {
        return MonopolyApp.self.color(toFloat(color.getRed()), toFloat(color.getGreen()), toFloat(color.getBlue()));
    }

    public static Color toColor(int r, int g, int b) {
        return color(toFloat(r), toFloat(g), toFloat(b));
    }

    public static boolean isMouseInArea(SpotProps areaProps) {
        return isPointInArea(MonopolyApp.self.mouseX, MonopolyApp.self.mouseY, areaProps);
    }

    public static boolean isPointInArea(int pX, int pY, SpotProps spotProps) {
        return isInsideArea(getCoords(spotProps), pX, pY);
    }

    public static int[] getCoords(SpotProps spotProps) {
        int x1, x2, y1, y2;
        int halfHeight = (int) spotProps.h() / 2;
        int halfWidth = (int) spotProps.w() / 2;
        if (spotProps.r() == 0 || spotProps.r() == 180) {
            x1 = spotProps.x() - halfWidth;
            y1 = spotProps.y() + halfHeight;

            x2 = spotProps.x() + halfWidth;
            y2 = spotProps.y() - halfHeight;

        } else {
            x1 = spotProps.x() - halfHeight;
            y1 = spotProps.y() + halfWidth;

            x2 = spotProps.x() + halfHeight;
            y2 = spotProps.y() - halfWidth;
        }
        return new int[]{x1, y1, x2, y2};
    }

    public static boolean isInsideArea(int[] coords, int x, int y) {
        return isInsideArea(coords[0], coords[1], coords[2], coords[3], x, y);
    }

    public static boolean isInsideArea(int x1, int y1, int x2, int y2, int x, int y) {
        return x > x1 && x < x2 && y < y1 && y > y2;
    }
}
