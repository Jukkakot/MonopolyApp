package fi.monopoly.client.desktop;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.components.event.MonopolyEventBus;
import fi.monopoly.components.GameSession;
import fi.monopoly.components.popup.PopupService;
import processing.core.PFont;

public final class MonopolyRuntime {
    private static MonopolyRuntime current;

    private final MonopolyApp app;
    private final ControlP5 controlP5;
    private final PFont font10;
    private final PFont font20;
    private final PFont font30;
    private final MonopolyEventBus eventBus;
    private final PopupService popupService;
    private GameSession gameSession;

    private MonopolyRuntime(MonopolyApp app, ControlP5 controlP5, PFont font10, PFont font20, PFont font30) {
        this.app = app;
        this.controlP5 = controlP5;
        this.font10 = font10;
        this.font20 = font20;
        this.font30 = font30;
        this.eventBus = new MonopolyEventBus();
        this.popupService = new PopupService(this);
    }

    public static MonopolyRuntime initialize(MonopolyApp app, ControlP5 controlP5, PFont font10, PFont font20, PFont font30) {
        current = new MonopolyRuntime(app, controlP5, font10, font20, font30);
        return current;
    }

    public static MonopolyRuntime get() {
        if (current == null) {
            throw new IllegalStateException("MonopolyRuntime has not been initialized yet");
        }
        return current;
    }

    public MonopolyApp app() {
        return app;
    }

    public static MonopolyRuntime peek() {
        return current;
    }

    public ControlP5 controlP5() {
        return controlP5;
    }

    public PFont font10() {
        return font10;
    }

    public PFont font20() {
        return font20;
    }

    public PFont font30() {
        return font30;
    }

    public MonopolyEventBus eventBus() {
        return eventBus;
    }

    public PopupService popupService() {
        return popupService;
    }

    public void setGameSession(GameSession gameSession) {
        this.gameSession = gameSession;
    }

    public GameSession gameSession() {
        if (gameSession == null) {
            throw new IllegalStateException("GameSession has not been initialized yet");
        }
        return gameSession;
    }

    public GameSession gameSessionOrNull() {
        return gameSession;
    }
}
