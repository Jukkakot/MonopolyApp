package org.example.utils;

import org.example.MonopolyApp;
import javafx.scene.paint.Color;
import processing.core.PApplet;

public class Utils extends MonopolyApp {
    public static float toFloat(int value) {
        return map(value,0,255,0,1);
    }
    public static float toFloat(double value) {
        return map((float) value, 0,1,0,255);
    }
    public static int toColor (PApplet p, Color color) {
        return p.color(toFloat(color.getRed()), toFloat(color.getGreen()), toFloat(color.getBlue()));
    }
}
