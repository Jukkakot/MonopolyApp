package fi.monopoly.components.dices;

import controlP5.Button;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.types.DiceState;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.SpotProps;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import processing.event.Event;
import processing.event.KeyEvent;

@Slf4j
public class Dices implements MonopolyEventListener {
    private final MonopolyRuntime runtime;
    private final Pair<Dice, Dice> dices;
    private final Button rollDiceButton;
    private int pairCount = 0;
    @Getter
    @Setter // Just for debugging can manually set dice value..
    private DiceValue value;

    public Dices(MonopolyRuntime runtime) {
        this.runtime = runtime;
        this.rollDiceButton = new MonopolyButton(runtime, "rollDice")
                .setPosition((int) (Spot.SPOT_W * 5.4), Spot.SPOT_W * 3)
                .setLabel("Roll dice")
                .setSize(100, 50);
        runtime.eventBus().addListener(this);
        float diceSideLength = Spot.SPOT_W / 2;
        SpotProps sp1 = new SpotProps((int) (Spot.SPOT_W * 5.7), (int) (Spot.SPOT_W * 2.5), diceSideLength, diceSideLength);
        SpotProps sp2 = new SpotProps((int) (Spot.SPOT_W * 6.3), sp1.y(), sp1.w(), sp1.h());
        dices = new Pair<>(new Dice(runtime, sp1), new Dice(runtime, sp2));

        rollDiceButton.addListener(e -> rollDice());
    }

    public static Dices setRollDice(MonopolyRuntime runtime, CallbackAction onRollAction) {
        return new Dices(runtime) {
            @Override
            public void rollDice() {
                if (runtime.popupService().isAnyVisible()) {
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
        log.debug("Rolled dice: first={}, second={}, pairCount={}, state={}, total={}",
                dice1, dice2, pairCount, value.diceState(), value.value());
    }

    public void draw(Coordinates c) {
        dices.getKey().draw(c);
        dices.getValue().draw(c);
    }

    public void rollDice() {
        if (!runtime.popupService().isAnyVisible()) {
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
        log.trace("Resetting dice state");
        pairCount = 0;
        value = null;
        show();
    }

    public boolean isVisible() {
        return rollDiceButton.isVisible();
    }

    @Override
    public boolean onEvent(Event event) {
        if (!isVisible() || runtime.popupService().isAnyVisible()) return false;
        if (event instanceof KeyEvent keyEvent && (keyEvent.getKey() == MonopolyApp.SPACE || keyEvent.getKey() == MonopolyApp.ENTER)) {
            rollDice();
            return true;
        }
        return false;
    }
}
