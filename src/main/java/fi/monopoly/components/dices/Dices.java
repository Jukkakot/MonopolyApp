package fi.monopoly.components.dices;

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

import static fi.monopoly.text.UiTexts.text;

@Slf4j
public class Dices implements MonopolyEventListener {
    private static final float SIDEBAR_X = Spot.SPOT_W * 12;
    private static final int SIDEBAR_BUTTON_Y = 184;
    private static final int SIDEBAR_BUTTON_HEIGHT = 44;
    private static final int SIDEBAR_BUTTON_CENTER_Y = SIDEBAR_BUTTON_Y + SIDEBAR_BUTTON_HEIGHT / 2;
    private final MonopolyRuntime runtime;
    private final Pair<Dice, Dice> dices;
    private final MonopolyButton rollDiceButton;
    private int pairCount = 0;
    @Getter
    @Setter // Just for debugging can manually set dice value..
    private DiceValue value;

    public Dices(MonopolyRuntime runtime) {
        this.runtime = runtime;
        this.rollDiceButton = new MonopolyButton(runtime, "rollDice")
                .setPosition(SIDEBAR_X + 20, SIDEBAR_BUTTON_Y)
                .setSize(150, SIDEBAR_BUTTON_HEIGHT)
                .setAutoWidth(100, 28, 180);
        refreshLabels();
        fi.monopoly.text.UiTexts.addChangeListener(this::refreshLabels);
        runtime.eventBus().addListener(this);
        float diceSideLength = Spot.SPOT_W / 2;
        SpotProps sp1 = new SpotProps((int) (SIDEBAR_X + 360), SIDEBAR_BUTTON_CENTER_Y, diceSideLength, diceSideLength);
        SpotProps sp2 = new SpotProps((int) (SIDEBAR_X + 420), sp1.y(), sp1.w(), sp1.h());
        dices = new Pair<>(new Dice(runtime, sp1), new Dice(runtime, sp2));

        rollDiceButton.addListener(e -> rollDice());
    }

    private void refreshLabels() {
        rollDiceButton.setLabel(text("dice.button.roll"));
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
        hide();
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
