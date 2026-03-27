package fi.monopoly.components.popup;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

import static fi.monopoly.text.UiTexts.text;

public class OkPopup extends Popup {
    private static final List<Character> OK_ACTION_CHAR_LIST = Arrays.asList('1', MonopolyApp.SPACE, MonopolyApp.ENTER);
    @Setter
    private ButtonAction onOkAction;
    private final MonopolyButton okButton;

    protected OkPopup(MonopolyRuntime runtime) {
        super(runtime);
        this.okButton = new MonopolyButton(runtime, "ok");
        okButton.addListener(this::okAction);
        okButton.setSize(100, 50);
        okButton.setAutoWidth(100, 28, 180);
        okButton.hide();
        refreshLabels();
        fi.monopoly.text.UiTexts.addChangeListener(this::refreshLabels);
    }

    private void refreshLabels() {
        okButton.setLabel(text("popup.ok.label"));
    }

    private void okAction() {
        completeAction(onOkAction);
    }

    @Override
    public void show() {
        super.show();
        layoutButtons();
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

    private void layoutButtons() {
        float buttonX = getPopupCenter().x() - okButton.getWidth() / 2f;
        float buttonY = getButtonAreaTop() + Math.max(0, (getButtonAreaHeight() - okButton.getHeight()) / 2f);
        okButton.setPosition(buttonX, buttonY);
    }
}
