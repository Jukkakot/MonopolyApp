package org.example.components.popup;

import controlP5.Button;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.components.MonopolyButton;

import java.util.Arrays;
import java.util.List;

import static org.example.MonopolyApp.ENTER;
import static org.example.MonopolyApp.SPACE;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OkPopup extends Popup {
    private static final List<Character> OK_ACTION_CHAR_LIST = Arrays.asList('1', SPACE, ENTER);
    @Setter
    private ButtonAction onOkAction;
    private final Button okButton = new MonopolyButton("ok")
            .setPosition(coords.x() - 50, coords.y() + (float) height / 4)
            .addListener(e -> okAction())
            .setLabel("Ok")
            .hide()
            .setSize(100, 50);

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
    protected void hide() {
        super.hide();
        okButton.hide();
        onOkAction = null;
    }

    @Override
    protected boolean onKeyAction(char key) {
        if (OK_ACTION_CHAR_LIST.contains(key)) {
            okAction();
            return true;
        }
        return super.onKeyAction(key);
    }
}
