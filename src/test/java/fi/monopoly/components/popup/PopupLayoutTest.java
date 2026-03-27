package fi.monopoly.components.popup;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.popup.components.ButtonProps;
import org.junit.jupiter.api.Test;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PopupLayoutTest {

    private static MonopolyRuntime initHeadlessRuntime(int width, int height) {
        MonopolyApp app = new MonopolyApp();
        app.width = width;
        app.height = height;

        PGraphicsJava2D graphics = new PGraphicsJava2D();
        graphics.setParent(app);
        graphics.setPrimary(true);
        graphics.setSize(app.width, app.height);
        app.g = graphics;

        ControlP5 controlP5 = new ControlP5(app);
        PFont font = app.createFont("Arial", 20);
        return MonopolyRuntime.initialize(app, controlP5, font, font, font);
    }

    private static MonopolyButton getButton(Object owner, String fieldName) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (MonopolyButton) field.get(owner);
    }

    @SuppressWarnings("unchecked")
    private static List<MonopolyButton> getCustomButtons(CustomPopup popup) throws ReflectiveOperationException {
        Field field = CustomPopup.class.getDeclaredField("customButtons");
        field.setAccessible(true);
        return (List<MonopolyButton>) field.get(popup);
    }

    @Test
    void popupShrinksToFitSmallWindow() {
        Popup popup = new OkPopup(initHeadlessRuntime(360, 260));

        assertEquals(320, popup.getPopupWidth());
        assertEquals(220, popup.getPopupHeight());
        assertEquals(180f, popup.getPopupCenter().x(), 0.0001f);
        assertEquals(130f, popup.getPopupCenter().y(), 0.0001f);
    }

    @Test
    void choicePopupKeepsButtonsInsideSmallPopup() throws ReflectiveOperationException {
        ChoicePopup popup = new ChoicePopup(initHeadlessRuntime(360, 260));
        popup.setPopupText("Question");
        popup.show();

        MonopolyButton acceptButton = getButton(popup, "acceptButton");
        MonopolyButton declineButton = getButton(popup, "declineButton");

        assertTrue(acceptButton.getPosition()[0] >= popup.getPopupLeft());
        assertTrue(declineButton.getPosition()[0] + declineButton.getWidth() <= popup.getPopupRight());
        assertTrue(acceptButton.getPosition()[1] >= popup.getButtonAreaTop());
        assertTrue(declineButton.getPosition()[1] + declineButton.getHeight() <= popup.getPopupBottom());
    }

    @Test
    void customPopupUsesNarrowerButtonGridOnSmallWindow() throws ReflectiveOperationException {
        CustomPopup popup = new CustomPopup(initHeadlessRuntime(360, 320));
        popup.setPopupText("Pick one");
        popup.setButtons(
                new ButtonProps("One", () -> {}),
                new ButtonProps("Two", () -> {}),
                new ButtonProps("Three", () -> {}),
                new ButtonProps("Four", () -> {})
        );
        popup.show();

        List<MonopolyButton> buttons = getCustomButtons(popup);
        MonopolyButton first = buttons.get(0);
        MonopolyButton second = buttons.get(1);
        MonopolyButton third = buttons.get(2);

        assertTrue(first.getPosition()[0] >= popup.getPopupLeft());
        assertTrue(second.getPosition()[0] + second.getWidth() <= popup.getPopupRight());
        assertTrue(third.getPosition()[1] > first.getPosition()[1]);
    }
}
