package org.example.utils;

import javafx.scene.paint.Color;
import org.example.MonopolyApp;

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
}
