package org.example.components;

import controlP5.Button;
import javafx.util.Pair;
import lombok.Getter;
import org.example.MonopolyApp;
import org.example.components.spots.Spot;
import org.example.utils.Coordinates;
import org.example.utils.DiceValue;
import org.example.utils.SpotProps;

public class Dices {
    private final Pair<Dice, Dice> dices;
    private final Button rollDiceButton;
    private int pairCount = 0;
    @Getter
    private DiceValue value;

    public Dices(MonopolyApp p) {
        SpotProps sp1 = new SpotProps((int) (Spot.spotW * 5.7), (int) (Spot.spotW * 2.5), Spot.spotW / 2, Spot.spotW / 2);
        SpotProps sp2 = new SpotProps((int) (Spot.spotW * 6.3), sp1.y(), sp1.width(), sp1.height());
        dices = new Pair<>(new Dice(p, sp1), new Dice(p, sp2));

        rollDiceButton = new Button(p.p5, "rollDice")
                .setPosition((int) (Spot.spotW * 5.4), (int) (Spot.spotW * 3))
                .addListener(e -> rollDice())
                .setLabel("Roll dice")
                .setFont(MonopolyApp.font20)
                .setSize(100, 50);
    }

    private void roll() {
        int dice1 = dices.getKey().roll();
        int dice2 = dices.getValue().roll();
        if (dice1 == dice2) {
            pairCount++;
        } else {
            pairCount = 0;
        }
        value = new DiceValue(DiceState.valueOf(pairCount), dice1 + dice2);
    }

    public void draw(Coordinates c) {
        dices.getKey().draw(c);
        dices.getValue().draw(c);
    }

    public void rollDice() {
        roll();
        rollDiceButton.hide();
    }

    public void show() {
        rollDiceButton.show();
    }

    public void reset() {
        pairCount = 0;
        value = null;
        show();
    }

}
