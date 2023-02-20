package org.example.components.dices;

import controlP5.Button;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import org.example.components.CallbackAction;
import org.example.MonopolyApp;
import org.example.components.event.MonopolyEventListener;
import org.example.components.spots.Spot;
import org.example.types.DiceState;
import org.example.utils.Coordinates;
import org.example.utils.SpotProps;
import processing.event.Event;
import processing.event.KeyEvent;

import static org.example.MonopolyApp.ENTER;
import static org.example.MonopolyApp.SPACE;

public class Dices implements MonopolyEventListener {
    private final Pair<Dice, Dice> dices;
    private static final Button rollDiceButton = new Button(MonopolyApp.p5, "rollDice")
            .setPosition((int) (Spot.SPOT_W * 5.4), Spot.SPOT_W * 3)
            .setLabel("Roll dice")
            .setFont(MonopolyApp.font20)
            .setSize(100, 50);
    private int pairCount = 0;
    @Getter
    @Setter // Just for debugging can manually set dice value..
    private DiceValue value;

    public Dices() {
        MonopolyApp.addListener(this);
        float diceSideLength = Spot.SPOT_W / 2;
        SpotProps sp1 = new SpotProps((int) (Spot.SPOT_W * 5.7), (int) (Spot.SPOT_W * 2.5), diceSideLength, diceSideLength);
        SpotProps sp2 = new SpotProps((int) (Spot.SPOT_W * 6.3), sp1.y(), sp1.w(), sp1.h());
        dices = new Pair<>(new Dice(sp1), new Dice(sp2));

        rollDiceButton.addListener(e -> rollDice());
    }

    public static Dices onRollDice(CallbackAction onRollAction) {
        return new Dices() {
            @Override
            public void rollDice() {
                super.rollDice();
                onRollAction.doAction();
            }
        };
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

    public void hide() {
        rollDiceButton.hide();
    }

    public void reset() {
        pairCount = 0;
        value = null;
        show();
    }

    public boolean isVisible() {
        return rollDiceButton.isVisible();
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof KeyEvent keyEvent) {
            if (!rollDiceButton.isVisible()) {
                return false;
            }
            if (keyEvent.getKey() == SPACE || keyEvent.getKey() == ENTER) {
                rollDice();
                return true;
            }
        }
        return true;
    }
}
