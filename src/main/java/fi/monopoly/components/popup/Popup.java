package fi.monopoly.components.popup;


import controlP5.Canvas;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.GameSession;
import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.LayoutMetrics;
import fi.monopoly.utils.MonopolyUtils;
import fi.monopoly.utils.TextWrapUtils;
import fi.monopoly.utils.UiTokens;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import processing.core.PGraphics;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.util.List;

import static processing.core.PConstants.CENTER;
import static processing.core.PConstants.TOP;

@Slf4j
public abstract class Popup extends Canvas implements MonopolyEventListener {
    protected final MonopolyRuntime runtime;
    protected String popupText;
    @Getter(AccessLevel.PROTECTED)
    protected boolean isVisible = false;

    protected Popup(MonopolyRuntime runtime) {
        this.runtime = runtime;
        runtime.controlP5().addCanvas(this);
        runtime.eventBus().addListener(this);
    }

    protected void show() {
        isVisible = true;
        log.info(popupText);
    }

    protected void hide() {
        isVisible = false;
    }

    protected void allButtonAction() {
        hide();
        runtime.popupService().onPopupClosed(this);
    }

    protected final void completeAction(ButtonAction action, PopupActionTrigger trigger) {
        if (!isInteractionAllowed(trigger)) {
            return;
        }
        allButtonAction();
        if (action != null) {
            action.doAction();
        }
        runtime.popupService().showNextPending();
    }

    protected final void completeManualAction(ButtonAction action) {
        completeAction(action, PopupActionTrigger.MANUAL);
    }

    protected final void completeComputerAction(ButtonAction action) {
        completeAction(action, PopupActionTrigger.COMPUTER);
    }

    protected final boolean isManualInteractionAllowed() {
        GameSession session = runtime.gameSessionOrNull();
        if (session != null && session.isGameOverActive()) {
            return true;
        }
        Player turnPlayer = session != null && session.players() != null ? session.players().getTurn() : null;
        return turnPlayer == null || !turnPlayer.isComputerControlled() || allowManualInteractionDuringComputerTurn();
    }

    protected final boolean isInteractionAllowed(PopupActionTrigger trigger) {
        return trigger == PopupActionTrigger.COMPUTER || isManualInteractionAllowed();
    }

    @Override
    public void draw(PGraphics p) {
        if (!isVisible) {
            return;
        }
        refreshControlLayout();
        float width = getPopupWidth();
        float height = getPopupHeight();
        float left = getPopupLeft();
        float top = getPopupTop();
        float centerX = getPopupCenter().x();
        float centerY = getPopupCenter().y();
        p.pushMatrix();
        p.pushStyle();
        p.resetMatrix();

        p.fill(p.color(255, 217, 127));
        p.rectMode(CENTER);
        p.stroke(0);
        p.strokeWeight(10);
        p.rect(centerX, centerY, width, height, 30);

        p.fill(0);
        p.textFont(runtime.font20());
        p.textAlign(CENTER, TOP);
        p.textLeading(UiTokens.popupTextLineHeight());
        drawPopupText(p, centerX, top + UiTokens.popupTextTopOffset(), width - UiTokens.popupTextSidePadding() * 2f);

        p.popStyle();
        p.popMatrix();
    }

    private void drawPopupText(PGraphics p, float centerX, float startY, float maxWidth) {
        List<String> lines = TextWrapUtils.wrapText(p, popupText, maxWidth);
        for (int i = 0; i < lines.size(); i++) {
            float y = startY + i * UiTokens.popupTextLineHeight();
            if (y + UiTokens.popupTextLineHeight() > getPopupTop() + UiTokens.popupTextTopOffset() + getTextAreaHeight()) {
                break;
            }
            p.text(lines.get(i), centerX, y);
        }
    }

    public void setPopupText(String text) {
        this.popupText = MonopolyUtils.parseIllegalCharacters(text);
    }

    public String getPopupText() {
        return popupText;
    }

    public String getPopupKind() {
        return getClass().getSimpleName();
    }

    public List<String> getVisibleActionLabels() {
        return List.of();
    }

    protected boolean onKeyAction(char key) {
        return false;
    }

    protected boolean onMouseAction(MouseEvent event) {
        return false;
    }

    protected boolean onComputerAction(ComputerPlayerProfile profile) {
        return triggerPrimaryAction(PopupActionTrigger.COMPUTER);
    }

    protected boolean triggerPrimaryAction(PopupActionTrigger trigger) {
        return false;
    }

    protected boolean triggerSecondaryAction(PopupActionTrigger trigger) {
        return false;
    }

    protected void refreshControlLayout() {
    }

    protected boolean allowManualInteractionDuringComputerTurn() {
        return false;
    }

    protected Coordinates getPopupCenter() {
        LayoutMetrics layoutMetrics = LayoutMetrics.fromWindow(runtime.app().width, runtime.app().height);
        return Coordinates.of(layoutMetrics.boardWidth() / 2f, runtime.app().height / 2f);
    }

    protected int getPopupWidth() {
        LayoutMetrics layoutMetrics = LayoutMetrics.fromWindow(runtime.app().width, runtime.app().height);
        int availableWidth = Math.round(layoutMetrics.boardWidth()) - UiTokens.popupWindowMargin() * 2;
        return Math.max(UiTokens.popupMinWidth(), Math.min(UiTokens.popupDefaultWidth(), availableWidth));
    }

    protected int getPopupHeight() {
        int availableHeight = runtime.app().height - UiTokens.popupWindowMargin() * 2;
        return Math.max(UiTokens.popupMinHeight(), Math.min(UiTokens.popupDefaultHeight(), availableHeight));
    }

    protected float getPopupLeft() {
        return getPopupCenter().x() - getPopupWidth() / 2f;
    }

    protected float getPopupTop() {
        return getPopupCenter().y() - getPopupHeight() / 2f;
    }

    protected float getPopupRight() {
        return getPopupLeft() + getPopupWidth();
    }

    protected float getPopupBottom() {
        return getPopupTop() + getPopupHeight();
    }

    protected float getTextAreaHeight() {
        return Math.max(
                0,
                getButtonAreaTop() - getPopupTop() - UiTokens.popupTextTopOffset() - UiTokens.popupTextBottomPadding()
        );
    }

    protected float getButtonAreaTop() {
        return getPopupTop() + UiTokens.popupButtonAreaTopPadding();
    }

    protected float getButtonAreaHeight() {
        return Math.max(0, getPopupHeight() - UiTokens.popupButtonAreaTopPadding() - UiTokens.popupButtonAreaBottomPadding());
    }

    @Override
    public final boolean onEvent(Event event) {
        if (!isVisible) {
            return false;
        }
        if (!isManualInteractionAllowed()) {
            return false;
        }
        if (event instanceof KeyEvent keyEvent) {
            // Deliberately do not debounce SPACE/ENTER here.
            // A previous attempt tracked "advance key held" in Popup, but that
            // broke expected hold-to-advance behavior and leaked state across
            // popup transitions because the guard lived in the shared base class.
            // If progression needs throttling, it must be handled in game flow
            // based on animation/turn state rather than here in popup input.
            return onKeyAction(keyEvent.getKey());
        }
        if (event instanceof MouseEvent mouseEvent) {
            return onMouseAction(mouseEvent);
        }
        return false;
    }
}
