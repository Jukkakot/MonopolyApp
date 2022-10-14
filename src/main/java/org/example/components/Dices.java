package org.example.components;

import javafx.util.Pair;
import lombok.Getter;
import org.example.spots.Drawable;
import org.example.spots.Spot;
import org.example.types.SpotProps;
import processing.core.PApplet;

public class Dices implements Drawable {
    private final Pair<Dice, Dice> dices;
    @Getter
    private int value = 0;

    public Dices(PApplet p) {
        SpotProps sp1 = new SpotProps((int) (Spot.spotW * 5.6), (int) (Spot.spotW * 2.5), 50, 50);
        SpotProps sp2 = new SpotProps((int) (Spot.spotW * 6.4), sp1.y(), sp1.width(), sp1.height());
        dices = new Pair<>(new Dice(p, sp1), new Dice(p, sp2));
    }

    public int roll() {
        value = dices.getKey().roll() + dices.getValue().roll();
        return value;
    }

    @Override
    public void draw(float rotate) {
        dices.getKey().draw(rotate);
        dices.getValue().draw(rotate);
    }

    @Override
    public void draw() {
        draw(0);
    }
}
