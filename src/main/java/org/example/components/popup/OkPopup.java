package org.example.components.popup;

import controlP5.Button;
import lombok.Setter;
import org.example.MonopolyApp;

import java.util.Arrays;
import java.util.List;

import static org.example.MonopolyApp.ENTER;
import static org.example.MonopolyApp.SPACE;

public class OkPopup extends Popup {
    private static final List<Character> OK_ACTION_CHAR_LIST = Arrays.asList('1', SPACE, ENTER);
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
        onOkAction = null;
    }

    @Override
    public boolean onKeyAction(char key) {
        if (OK_ACTION_CHAR_LIST.contains(key)) {
            okAction();
            return true;
        }
        return false;
    }
}
