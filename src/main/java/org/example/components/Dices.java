package org.example.components;

import javafx.util.Pair;
import lombok.Getter;
import org.example.components.spots.Spot;
import org.example.utils.Coordinates;
import org.example.utils.SpotProps;
import processing.core.PApplet;

public class Dices {
    private final Pair<Dice, Dice> dices;
    @Getter
    private int value = 0;

    public Dices(PApplet p) {
        SpotProps sp1 = new SpotProps((int) (Spot.spotW * 5.7), (int) (Spot.spotW * 2.5), Spot.spotW / 2, Spot.spotW / 2);
        SpotProps sp2 = new SpotProps((int) (Spot.spotW * 6.3), sp1.y(), sp1.width(), sp1.height());
        dices = new Pair<>(new Dice(p, sp1), new Dice(p, sp2));
    }

    public int roll() {
        value = dices.getKey().roll() + dices.getValue().roll();
        return value;
    }

    public void draw(Coordinates c) {
        dices.getKey().draw(c);
        dices.getValue().draw(c);
    }

}
