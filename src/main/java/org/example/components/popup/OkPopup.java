package org.example.components.popup;

import controlP5.Button;
import lombok.Setter;
import org.example.MonopolyApp;

public class OkPopup extends Popup {
    private final Button okButton;
    @Setter
    private ButtonAction onAccept;

    public OkPopup() {
        super();
        okButton = new Button(p.p5, "ok")
                .setPosition(coords.x() - 50, coords.y() + (float) height / 4)
                .addListener(e -> okAction())
                .setLabel("Ok")
                .setFont(MonopolyApp.font20)
                .hide()
                .setSize(100, 50);
    }

    private void okAction() {
        if (onAccept != null) {
            onAccept.doAction();
        }
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
