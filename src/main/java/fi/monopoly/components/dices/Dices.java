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
import fi.monopoly.utils.UiTokens;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import processing.event.Event;
import processing.event.KeyEvent;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
public class Dices implements MonopolyEventListener {
    private final MonopolyRuntime runtime;
    private final Pair<Dice, Dice> dices;
    private final MonopolyButton rollDiceButton;
    private boolean rollDiceRequestedVisible = true;
    private int pairCount = 0;
    private int lastLayoutHash = Integer.MIN_VALUE;
    @Getter
    @Setter // Just for debugging can manually set dice value..
    private DiceValue value;

    public Dices(MonopolyRuntime runtime) {
        this.runtime = runtime;
        LayoutMetrics defaultLayout = LayoutMetrics.defaultWindow();
        this.rollDiceButton = new MonopolyButton(runtime, "rollDice")
                .setPosition(
                        defaultLayout.sidebarX() + UiTokens.spacingLg(),
                        defaultLayout.sidebarPrimaryButtonY() + UiTokens.diceButtonOffsetFromPrimary()
                )
                .setSize(150, UiTokens.diceButtonHeight())
                .setAutoWidth(100, 28, 180);
        refreshLabels();
        fi.monopoly.text.UiTexts.addChangeListener(this::refreshLabels);
        runtime.eventBus().addListener(this);
        float diceSideLength = Spot.SPOT_W / 2;
        float defaultButtonCenterY = defaultLayout.sidebarPrimaryButtonY()
                + UiTokens.diceButtonOffsetFromPrimary()
                + UiTokens.diceButtonHeight() / 2f;
        float defaultInlineSecondCenterX = defaultLayout.sidebarRight() - UiTokens.spacingMd() - diceSideLength / 2f;
        float defaultInlineFirstCenterX = defaultInlineSecondCenterX - diceSideLength - UiTokens.spacingMd();
        SpotProps sp1 = new SpotProps((int) defaultInlineFirstCenterX, (int) defaultButtonCenterY, diceSideLength, diceSideLength);
        SpotProps sp2 = new SpotProps((int) defaultInlineSecondCenterX, sp1.y(), sp1.w(), sp1.h());
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
        int layoutHash = layoutHash(layoutMetrics);
        if (layoutHash == lastLayoutHash) {
            applyRollDiceButtonVisibility();
            return;
        }
        lastLayoutHash = layoutHash;
        if (!layoutMetrics.hasSidebarSpace()) {
            layoutOverlayControls(layoutMetrics);
            applyRollDiceButtonVisibility();
            return;
        }
        float buttonX = layoutMetrics.sidebarX() + UiTokens.spacingLg();
        float buttonY = layoutMetrics.sidebarPrimaryButtonY() + UiTokens.diceButtonOffsetFromPrimary();
        float buttonCenterY = buttonY + UiTokens.diceButtonHeight() / 2f;
        rollDiceButton.setPosition(buttonX, buttonY);

        float diceSide = dices.getKey().getUnScaledWidth();
        float inlineSecondCenterX = layoutMetrics.sidebarRight() - UiTokens.spacingMd() - diceSide / 2f;
        float inlineFirstCenterX = inlineSecondCenterX - diceSide - UiTokens.spacingMd();
        float minimumInlineFirstCenterX = buttonX + rollDiceButton.getWidth() + UiTokens.spacingSm() + diceSide / 2f;
        if (inlineFirstCenterX >= minimumInlineFirstCenterX) {
            setDicePositions(inlineFirstCenterX, buttonCenterY, inlineSecondCenterX, buttonCenterY);
            applyRollDiceButtonVisibility();
            return;
        }

        float stackedFirstCenterX = buttonX + diceSide / 2f;
        float stackedSecondCenterX = stackedFirstCenterX + diceSide + UiTokens.spacingMd();
        float stackedCenterY = buttonCenterY + UiTokens.diceStackedVerticalOffset();
        setDicePositions(stackedFirstCenterX, stackedCenterY, stackedSecondCenterX, stackedCenterY);
        applyRollDiceButtonVisibility();
    }

    private void layoutOverlayControls(LayoutMetrics layoutMetrics) {
        float buttonX = UiTokens.overlayMargin();
        rollDiceButton.setPosition(buttonX, UiTokens.overlayPrimaryButtonY());

        float diceSide = dices.getKey().getUnScaledWidth();
        float inlineFirstCenterX = buttonX + rollDiceButton.getWidth() + UiTokens.spacingSm() + diceSide / 2f;
        float inlineSecondCenterX = inlineFirstCenterX + diceSide + UiTokens.spacingMd();
        float maxInlineRight = layoutMetrics.boardWidth() - UiTokens.spacingMd();
        float overlayButtonCenterY = UiTokens.overlayPrimaryButtonY() + UiTokens.diceButtonHeight() / 2f;
        if (inlineSecondCenterX + diceSide / 2f <= maxInlineRight) {
            setDicePositions(inlineFirstCenterX, overlayButtonCenterY, inlineSecondCenterX, overlayButtonCenterY);
            return;
        }

        float stackedFirstCenterX = buttonX + diceSide / 2f;
        float stackedSecondCenterX = stackedFirstCenterX + diceSide + UiTokens.spacingMd();
        float overlayStackedCenterY = overlayButtonCenterY + UiTokens.diceStackedVerticalOffset();
        setDicePositions(stackedFirstCenterX, overlayStackedCenterY, stackedSecondCenterX, overlayStackedCenterY);
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

    private int layoutHash(LayoutMetrics layoutMetrics) {
        return java.util.Objects.hash(
                layoutMetrics.hasSidebarSpace(),
                Math.round(layoutMetrics.sidebarX()),
                Math.round(layoutMetrics.sidebarRight()),
                Math.round(layoutMetrics.sidebarPrimaryButtonY()),
                Math.round(layoutMetrics.boardWidth()),
                rollDiceButton.getWidth()
        );
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
