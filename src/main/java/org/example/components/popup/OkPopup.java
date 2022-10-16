package org.example.components.popup;

import controlP5.Button;
import org.example.MonopolyApp;

public class OkPopup extends Popup {
    private final Button okButton;

    public OkPopup(MonopolyApp p, String popupText) {
        super(p, popupText);
        okButton = new Button(p.p5, "ok")
                .setPosition(coords.x() - 50, coords.y() + height / 4)
                .addListener(e -> okAction())
                .setLabel("Ok")
                .setFont(MonopolyApp.font20)
                .hide()
                .setSize(100, 50);
    }

    public void okAction() {
        allButtonAction();
    }

    @Override
    public void show() {
        super.show();
        okButton.show();
    }

    @Override
    protected void allButtonAction() {
        super.allButtonAction();
        okButton.hide();
    }
}
