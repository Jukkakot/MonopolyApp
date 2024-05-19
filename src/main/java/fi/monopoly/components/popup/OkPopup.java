package fi.monopoly.components.popup;

import controlP5.Button;
import fi.monopoly.MonopolyApp;
import fi.monopoly.components.MonopolyButton;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OkPopup extends Popup {
    private static final List<Character> OK_ACTION_CHAR_LIST = Arrays.asList('1', MonopolyApp.SPACE, MonopolyApp.ENTER);
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
