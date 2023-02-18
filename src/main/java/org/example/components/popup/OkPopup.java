package org.example.components.popup;

import controlP5.Button;
import lombok.Setter;
import org.example.MonopolyApp;
import processing.event.Event;
import processing.event.KeyEvent;

import java.util.Arrays;
import java.util.List;

import static org.example.MonopolyApp.ENTER;
import static org.example.MonopolyApp.SPACE;

public class OkPopup extends Popup {
    private static OkPopup instance;
    @Setter
    private ButtonAction onOkAction;
    private final Button okButton = new Button(MonopolyApp.p5, "ok")
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

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof KeyEvent keyEvent) {
            if (!isVisible()) {
                return false;
            }
            List<Character> charList = Arrays.asList('1', SPACE, ENTER);
            if (charList.contains(keyEvent.getKey())) {
                okAction();
                return true;
            }
        }
        return false;
    }
}
