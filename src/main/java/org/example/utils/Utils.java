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

}
