package org.example.components.popup;

import controlP5.Button;
import lombok.Setter;
import org.example.MonopolyApp;
import processing.event.Event;
import processing.event.KeyEvent;

public class ChoicePopup extends Popup {
    private static ChoicePopup instance;

    private final Button acceptButton = new Button(MonopolyApp.p5, "accept")
            .setPosition(coords.x() - 150, coords.y() + (float) height / 4)
            .addListener(e -> acceptAction())
            .setLabel("Accept")
            .setFont(MonopolyApp.font20)
            .hide()
            .setSize(100, 50);
    private final Button declineButton = new Button(MonopolyApp.p5, "decline")
            .setPosition(coords.x() + 50, coords.y() + (float) height / 4)
            .addListener(e -> declineAction())
            .setLabel("Decline")
            .setFont(MonopolyApp.font20)
            .hide()
            .setSize(100, 50);
    @Setter
    private ButtonAction onAcceptAction;
    @Setter
    private ButtonAction onDeclineAction;

    public static ChoicePopup getInstance() {
        if (instance == null) {
            instance = new ChoicePopup();
        }
        return instance;
    }

    private void acceptAction() {
        if (onAcceptAction != null) {
            onAcceptAction.doAction();
        }
        allButtonAction();
    }

    private void declineAction() {
        if (onDeclineAction != null) {
            onDeclineAction.doAction();
        }
        allButtonAction();
    }

    @Override
    public void show() {
        super.show();
        acceptButton.show();
        declineButton.show();
    }

    @Override
    protected void allButtonAction() {
        super.allButtonAction();
        acceptButton.hide();
        declineButton.hide();
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof KeyEvent keyEvent) {
            if (!isVisible()) {
                return;
            }
            if (keyEvent.getKey() == '1') {
                acceptAction();
            } else if (keyEvent.getKey() == '2') {
                declineAction();
            }
        }

    }
}
