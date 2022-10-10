package org.example;

public class Utils extends MonopolyApp {
    public static float toFloat(int value) {
        return map(value,0,255,0,1);
    }
    public static float toFloat(double value) {
        return map((float) value, 0,1,0,255);
    }
}
