package org.example.components.popup;

import controlP5.Button;
import lombok.Setter;
import org.example.MonopolyApp;

public class OkPopup extends Popup {
    private static OkPopup instance;
    @Setter
    private ButtonAction onOkAction;
    private final Button okButton = new Button(MonopolyApp.self.p5, "ok")
            .setPosition(coords.x() - 50, coords.y() + (float) height / 4)
            .addListener(e -> okAction())
            .setLabel("Ok")
            .setFont(MonopolyApp.font20)
            .hide()
            .setSize(100, 50);

    public static OkPopup getInstance() {
        if (instance == null) {
            instance = new OkPopup();
        }
        return instance;
    }

    private void okAction() {
        if (onOkAction != null) {
            onOkAction.doAction();
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
