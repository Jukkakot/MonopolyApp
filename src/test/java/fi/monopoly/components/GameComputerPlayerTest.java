package fi.monopoly.components;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameComputerPlayerTest {

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

    private static void resetNextPlayerId() throws ReflectiveOperationException {
        Field field = Player.class.getDeclaredField("NEXT_ID");
        field.setAccessible(true);
        field.setInt(null, 0);
    }

    private static void invokeComputerStep(Game game) throws ReflectiveOperationException {
        var method = Game.class.getDeclaredMethod("runComputerPlayerStep");
        method.setAccessible(true);
        method.invoke(game);
    }

    private static void invokeTogglePause(Game game) throws ReflectiveOperationException {
        var method = Game.class.getDeclaredMethod("togglePause");
        method.setAccessible(true);
        method.invoke(game);
    }

    @SuppressWarnings("unchecked")
    private static List<Player> getPlayerList() throws ReflectiveOperationException {
        Field field = Players.class.getDeclaredField("playerList");
        field.setAccessible(true);
        return (List<Player>) field.get(Game.players);
    }

    @AfterEach
    void tearDown() {
        MonopolyApp.DEBUG_MODE = false;
        MonopolyApp.SKIP_ANNIMATIONS = false;
    }

    @Test
    void defaultSeatsUseStrongBotProfile() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        new Game(runtime);

        List<Player> players = getPlayerList();
        long computerPlayerCount = players.stream().filter(Player::isComputerControlled).count();
        long strongBotCount = players.stream()
                .filter(player -> player.getComputerProfile() == ComputerPlayerProfile.STRONG)
                .count();

        assertEquals(3, players.size());
        assertEquals(3, computerPlayerCount);
        assertEquals(3, strongBotCount);
    }

    @Test
    void defaultBotCanFinishItsTurnWithoutUserInput() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyApp.SKIP_ANNIMATIONS = true;
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        Game.players.switchTurn();
        Game.players.switchTurn();
        Player bot = Game.players.getTurn();
        String botName = bot.getName();

        for (int step = 0; step < 200 && Objects.equals(botName, Game.players.getTurn().getName()); step++) {
            runtime.eventBus().flushPendingChanges();
            invokeComputerStep(game);
            runtime.eventBus().flushPendingChanges();
            if (Game.animations.isRunning()) {
                Game.animations.finishAllAnimations();
            }
        }

        assertNotEquals(botName, Game.players.getTurn().getName());
    }

    @Test
    void pausePreventsComputerTurnFromAdvancing() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyApp.SKIP_ANNIMATIONS = true;
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        String botName = Game.players.getTurn().getName();
        invokeTogglePause(game);

        for (int step = 0; step < 50; step++) {
            runtime.eventBus().flushPendingChanges();
            invokeComputerStep(game);
            runtime.eventBus().flushPendingChanges();
            if (Game.animations.isRunning()) {
                Game.animations.finishAllAnimations();
            }
        }

        assertEquals(botName, Game.players.getTurn().getName());
    }
}
