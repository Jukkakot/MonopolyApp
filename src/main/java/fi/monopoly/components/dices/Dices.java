package fi.monopoly.components.dices;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.types.DiceState;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.LayoutMetrics;
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
    private static final LayoutMetrics DEFAULT_LAYOUT = LayoutMetrics.defaultWindow();
    private static final float SIDEBAR_X = DEFAULT_LAYOUT.sidebarX();
    private static final int SIDEBAR_MARGIN = LayoutMetrics.spacingMd();
    private static final int DICE_GAP = LayoutMetrics.spacingMd();
    private static final int SIDEBAR_BUTTON_HEIGHT = 44;
    private static final int SIDEBAR_BUTTON_OFFSET_FROM_PRIMARY = -8;
    private static final int STACKED_DICE_VERTICAL_OFFSET = 56;
    private static final int OVERLAY_BUTTON_X = LayoutMetrics.overlayMargin();
    private static final int OVERLAY_BUTTON_Y = LayoutMetrics.overlayPrimaryButtonY();
    private static final int OVERLAY_BUTTON_CENTER_Y = OVERLAY_BUTTON_Y + SIDEBAR_BUTTON_HEIGHT / 2;
    private static final int OVERLAY_STACKED_DICE_CENTER_Y = OVERLAY_BUTTON_CENTER_Y + 56;
    private final MonopolyRuntime runtime;
    private final Pair<Dice, Dice> dices;
    private final MonopolyButton rollDiceButton;
    private boolean rollDiceRequestedVisible = true;
    private int pairCount = 0;
    @Getter
    @Setter // Just for debugging can manually set dice value..
    private DiceValue value;

    public Dices(MonopolyRuntime runtime) {
        this.runtime = runtime;
        this.rollDiceButton = new MonopolyButton(runtime, "rollDice")
                .setPosition(SIDEBAR_X + LayoutMetrics.spacingLg(), DEFAULT_LAYOUT.sidebarPrimaryButtonY() + SIDEBAR_BUTTON_OFFSET_FROM_PRIMARY)
                .setSize(150, SIDEBAR_BUTTON_HEIGHT)
                .setAutoWidth(100, 28, 180);
        refreshLabels();
        fi.monopoly.text.UiTexts.addChangeListener(this::refreshLabels);
        runtime.eventBus().addListener(this);
        float diceSideLength = Spot.SPOT_W / 2;
        float defaultButtonCenterY = DEFAULT_LAYOUT.sidebarPrimaryButtonY() + SIDEBAR_BUTTON_OFFSET_FROM_PRIMARY + SIDEBAR_BUTTON_HEIGHT / 2f;
        SpotProps sp1 = new SpotProps((int) (SIDEBAR_X + 360), (int) defaultButtonCenterY, diceSideLength, diceSideLength);
        SpotProps sp2 = new SpotProps((int) (SIDEBAR_X + 420), sp1.y(), sp1.w(), sp1.h());
        dices = new Pair<>(new Dice(runtime, sp1), new Dice(runtime, sp2));
        updateLayout(LayoutMetrics.fromWindow(runtime.app().width, runtime.app().height));

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

    public void updateLayout(LayoutMetrics layoutMetrics) {
        if (!layoutMetrics.hasSidebarSpace()) {
            layoutOverlayControls(layoutMetrics);
            applyRollDiceButtonVisibility();
            return;
        }
        float buttonX = layoutMetrics.sidebarX() + LayoutMetrics.spacingLg();
        float buttonY = layoutMetrics.sidebarPrimaryButtonY() + SIDEBAR_BUTTON_OFFSET_FROM_PRIMARY;
        float buttonCenterY = buttonY + SIDEBAR_BUTTON_HEIGHT / 2f;
        rollDiceButton.setPosition(buttonX, buttonY);

        float diceSide = dices.getKey().getUnScaledWidth();
        float inlineSecondCenterX = layoutMetrics.sidebarRight() - SIDEBAR_MARGIN - diceSide / 2f;
        float inlineFirstCenterX = inlineSecondCenterX - diceSide - DICE_GAP;
        float minimumInlineFirstCenterX = buttonX + rollDiceButton.getWidth() + LayoutMetrics.spacingSm() + diceSide / 2f;
        if (inlineFirstCenterX >= minimumInlineFirstCenterX) {
            setDicePositions(inlineFirstCenterX, buttonCenterY, inlineSecondCenterX, buttonCenterY);
            applyRollDiceButtonVisibility();
            return;
        }

        float stackedFirstCenterX = buttonX + diceSide / 2f;
        float stackedSecondCenterX = stackedFirstCenterX + diceSide + DICE_GAP;
        float stackedCenterY = buttonCenterY + STACKED_DICE_VERTICAL_OFFSET;
        setDicePositions(stackedFirstCenterX, stackedCenterY, stackedSecondCenterX, stackedCenterY);
        applyRollDiceButtonVisibility();
    }

    private void layoutOverlayControls(LayoutMetrics layoutMetrics) {
        float buttonX = OVERLAY_BUTTON_X;
        rollDiceButton.setPosition(buttonX, OVERLAY_BUTTON_Y);

        float diceSide = dices.getKey().getUnScaledWidth();
        float inlineFirstCenterX = buttonX + rollDiceButton.getWidth() + LayoutMetrics.spacingSm() + diceSide / 2f;
        float inlineSecondCenterX = inlineFirstCenterX + diceSide + DICE_GAP;
        float maxInlineRight = layoutMetrics.boardWidth() - SIDEBAR_MARGIN;
        if (inlineSecondCenterX + diceSide / 2f <= maxInlineRight) {
            setDicePositions(inlineFirstCenterX, OVERLAY_BUTTON_CENTER_Y, inlineSecondCenterX, OVERLAY_BUTTON_CENTER_Y);
            return;
        }

        float stackedFirstCenterX = buttonX + diceSide / 2f;
        float stackedSecondCenterX = stackedFirstCenterX + diceSide + DICE_GAP;
        setDicePositions(stackedFirstCenterX, OVERLAY_STACKED_DICE_CENTER_Y, stackedSecondCenterX, OVERLAY_STACKED_DICE_CENTER_Y);
        applyRollDiceButtonVisibility();
    }

    public void rollDice() {
        if (!runtime.popupService().isAnyVisible()) {
            roll();
            // After a successful roll, the button must remain logically hidden until the
            // game explicitly re-enables rolling for a later phase/turn.
            hide();
        }
    }

    public void show() {
        rollDiceRequestedVisible = true;
        applyRollDiceButtonVisibility();
    }

    public void hide() {
        rollDiceRequestedVisible = false;
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

    private void applyRollDiceButtonVisibility() {
        if (rollDiceRequestedVisible && !runtime.popupService().isAnyVisible()) {
            rollDiceButton.show();
            return;
        }
        rollDiceButton.hide();
    }

    private void setDicePositions(float firstX, float firstY, float secondX, float secondY) {
        dices.getKey().setCoords(Coordinates.of(firstX, firstY));
        dices.getValue().setCoords(Coordinates.of(secondX, secondY));
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
