package fi.monopoly.components.popup;

import controlP5.ControlP5;
import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.popup.components.ButtonProps;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.types.SpotType;
import fi.monopoly.utils.LayoutMetrics;
import fi.monopoly.utils.UiTokens;
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
        Class<?> type = owner.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return (MonopolyButton) field.get(owner);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
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
        assertTrue(popup.getTextAreaHeight() > 0);
        assertTrue(popup.getPopupTop() + UiTokens.popupTextTopOffset() + popup.getTextAreaHeight() <= popup.getButtonAreaTop());
    }

    @Test
    void popupCentersToBoardAreaWhenSidebarIsVisible() {
        Popup popup = new OkPopup(initHeadlessRuntime(1700, 996));

        assertEquals(498f, popup.getPopupCenter().x(), 0.0001f);
        assertEquals(500, popup.getPopupWidth());
    }

    @Test
    void tradePopupAlsoCentersToBoardAreaWhenSidebarIsVisible() {
        TradePopup popup = new TradePopup(initHeadlessRuntime(1700, 996));

        assertEquals(498f, popup.getPopupCenter().x(), 0.0001f);
        assertTrue(popup.getPopupRight() <= LayoutMetrics.fromWindow(1700, 996).boardWidth());
    }

    @Test
    void tradePopupUsesSessionPopupKind() {
        TradePopup popup = new TradePopup(initHeadlessRuntime(1700, 996));

        assertEquals("trade", popup.getPopupKind());
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
    void visibleChoicePopupRelayoutsButtonsAfterWindowResize() throws ReflectiveOperationException {
        MonopolyRuntime runtime = initHeadlessRuntime(1700, 996);
        ChoicePopup popup = new ChoicePopup(runtime);
        popup.setPopupText("Question");
        popup.show();

        MonopolyButton acceptButton = getButton(popup, "acceptButton");
        float originalX = acceptButton.getPosition()[0];
        float originalY = acceptButton.getPosition()[1];

        runtime.app().width = 800;
        runtime.app().height = 700;
        popup.refreshControlLayout();

        assertTrue(acceptButton.getPosition()[0] != originalX || acceptButton.getPosition()[1] != originalY);
        assertTrue(acceptButton.getPosition()[0] >= popup.getPopupLeft());
        assertTrue(acceptButton.getPosition()[0] + acceptButton.getWidth() <= popup.getPopupRight());
        assertTrue(acceptButton.getPosition()[1] >= popup.getButtonAreaTop());
        assertTrue(acceptButton.getPosition()[1] + acceptButton.getHeight() <= popup.getPopupBottom());
    }

    @Test
    void propertyOfferPopupButtonsRemainVisibleAfterChoicePopupExists() throws ReflectiveOperationException {
        MonopolyRuntime runtime = initHeadlessRuntime(1700, 996);
        ChoicePopup choicePopup = new ChoicePopup(runtime);
        choicePopup.setPopupText("Question");
        choicePopup.show();

        PropertyOfferPopup propertyOfferPopup = new PropertyOfferPopup(runtime);
        propertyOfferPopup.setPopupText("Buy it?");
        propertyOfferPopup.setOfferedProperty(new StreetProperty(SpotType.B1));
        propertyOfferPopup.show();

        MonopolyButton acceptButton = getButton(propertyOfferPopup, "acceptButton");
        MonopolyButton declineButton = getButton(propertyOfferPopup, "declineButton");

        assertTrue(acceptButton.isVisible());
        assertTrue(declineButton.isVisible());
        assertTrue(acceptButton.getPosition()[0] >= propertyOfferPopup.getPopupLeft());
        assertTrue(declineButton.getPosition()[0] + declineButton.getWidth() <= propertyOfferPopup.getPopupRight());
        assertTrue(acceptButton.getPosition()[1] >= propertyOfferPopup.getButtonAreaTop());
    }

    @Test
    void propertyOfferPopupUsesSessionPopupKind() {
        PropertyOfferPopup popup = new PropertyOfferPopup(initHeadlessRuntime(1700, 996));

        assertEquals("propertyOffer", popup.getPopupKind());
    }

    @Test
    void propertyAuctionPopupKeepsButtonsInsidePropertyPopupLayout() throws ReflectiveOperationException {
        MonopolyRuntime runtime = initHeadlessRuntime(1700, 996);
        PropertyAuctionPopup popup = new PropertyAuctionPopup(runtime);
        popup.setPopupText("Auction for Brown 1");
        popup.setOfferedProperty(new StreetProperty(SpotType.B1));
        popup.setButtonLabels("Bid M10", "Pass");
        popup.show();

        MonopolyButton acceptButton = getButton(popup, "acceptButton");
        MonopolyButton declineButton = getButton(popup, "declineButton");

        assertTrue(acceptButton.isVisible());
        assertTrue(declineButton.isVisible());
        assertTrue(acceptButton.getPosition()[0] >= popup.getPopupLeft());
        assertTrue(declineButton.getPosition()[0] + declineButton.getWidth() <= popup.getPopupRight());
        assertTrue(acceptButton.getPosition()[1] >= popup.getButtonAreaTop());
        assertTrue(declineButton.getPosition()[1] + declineButton.getHeight() <= popup.getPopupBottom());
    }

    @Test
    void propertyAuctionPopupUsesSessionPopupKind() {
        PropertyAuctionPopup popup = new PropertyAuctionPopup(initHeadlessRuntime(1700, 996));

        assertEquals("propertyAuction", popup.getPopupKind());
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
