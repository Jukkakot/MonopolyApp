package fi.monopoly.components.dices;

import controlP5.Button;
import fi.monopoly.MonopolyApp;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.popup.Popup;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.types.DiceState;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.SpotProps;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import processing.event.Event;
import processing.event.KeyEvent;

public class Dices implements MonopolyEventListener {
    private final Pair<Dice, Dice> dices;
    private static final Button rollDiceButton = new MonopolyButton( "rollDice")
            .setPosition((int) (Spot.SPOT_W * 5.4), Spot.SPOT_W * 3)
            .setLabel("Roll dice")
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

    public static Dices setRollDice(CallbackAction onRollAction) {
        return new Dices() {
            @Override
            public void rollDice() {
                if (Popup.isAnyVisible()) {
                    return;
                }
                super.rollDice();
                onRollAction.doAction();
            }
        };
    }

    private void roll() {
        int dice1 = dices.getKey().roll(), dice2 = dices.getValue().roll();
        if (dice1 == dice2) pairCount++;
        else pairCount = 0;
        value = new DiceValue(DiceState.valueOf(pairCount), dice1 + dice2);
    }

    public void draw(Coordinates c) {
        dices.getKey().draw(c);
        dices.getValue().draw(c);
    }

    public void rollDice() {
        if (!Popup.isAnyVisible()) {
            roll();
            rollDiceButton.hide();
        }
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
        if (!isVisible() || Popup.isAnyVisible()) return false;
        if (event instanceof KeyEvent keyEvent && (keyEvent.getKey() == MonopolyApp.SPACE || keyEvent.getKey() == MonopolyApp.ENTER)) {
            rollDice();
            return true;
        }
        return false;
    }
}
