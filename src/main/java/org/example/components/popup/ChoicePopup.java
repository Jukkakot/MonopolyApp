package org.example.components.popup;

import controlP5.Button;
import org.example.MonopolyApp;

public class ChoicePopup extends Popup {
    private final Button acceptButton;
    private final Button declineButton;

    public ChoicePopup(MonopolyApp p) {
        super(p);
        acceptButton = new Button(p.p5, "accept")
                .setPosition(coords.x() - 150, coords.y() + height / 4)
                .addListener(e -> acceptAction())
                .setLabel("Accept")
                .setFont(MonopolyApp.font20)
                .hide()
                .setSize(100, 50);

        declineButton = new Button(p.p5, "decline")
                .setPosition(coords.x() + 50, coords.y() + height / 4)
                .addListener(e -> declineAction())
                .setLabel("Decline")
                .setFont(MonopolyApp.font20)
                .hide()
                .setSize(100, 50);
    }

    private void acceptAction() {
        buttonActions.onAccept();
        allButtonAction();
    }

    private void declineAction() {
        buttonActions.onDecline();
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
}
