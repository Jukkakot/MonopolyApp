package fi.monopoly.components;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.support.TestLogLevels;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;
import processing.event.KeyEvent;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static processing.event.KeyEvent.PRESS;

class GameComputerPlayerTest {
    private TestLogLevels.LogConfigSnapshot logConfigSnapshot;

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

    private static void invokeAnimationFinishCooldown(Game game, boolean animationWasRunning) throws ReflectiveOperationException {
        var method = Game.class.getDeclaredMethod("applyComputerActionCooldownIfAnimationJustFinished", boolean.class);
        method.setAccessible(true);
        method.invoke(game, animationWasRunning);
    }

    private static void invokeShowRollDiceControl(Game game) throws ReflectiveOperationException {
        var method = Game.class.getDeclaredMethod("showRollDiceControl");
        method.setAccessible(true);
        method.invoke(game);
    }

    private static int getLastComputerActionAt(Game game) throws ReflectiveOperationException {
        Field field = Game.class.getDeclaredField("lastComputerActionAt");
        field.setAccessible(true);
        return field.getInt(game);
    }

    private static boolean dispatchKeyToGame(Game game, char key) {
        return game.onEvent(new KeyEvent(new Object(), System.currentTimeMillis(), PRESS, 0, key, key));
    }

    @SuppressWarnings("unchecked")
    private static List<Player> getPlayerList() throws ReflectiveOperationException {
        Field field = Players.class.getDeclaredField("playerList");
        field.setAccessible(true);
        return (List<Player>) field.get(Game.PLAYERS);
    }

    @BeforeEach
    void setWarnOnlyLogging() {
        logConfigSnapshot = TestLogLevels.useSimulationLogging();
    }

    @AfterEach
    void tearDown() {
        if (logConfigSnapshot != null) {
            logConfigSnapshot.restore();
        }
        MonopolyApp.DEBUG_MODE = false;
        MonopolyApp.SKIP_ANNIMATIONS = false;
    }

    @Test
    @Timeout(5)
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
    @Timeout(5)
    void defaultBotCanFinishItsTurnWithoutUserInput() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyApp.SKIP_ANNIMATIONS = true;
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        Game.PLAYERS.switchTurn();
        Game.PLAYERS.switchTurn();
        Player bot = Game.PLAYERS.getTurn();
        String botName = bot.getName();

        for (int step = 0; step < 200 && Objects.equals(botName, Game.PLAYERS.getTurn().getName()); step++) {
            runtime.eventBus().flushPendingChanges();
            invokeComputerStep(game);
            runtime.eventBus().flushPendingChanges();
            if (Game.ANIMATIONS.isRunning()) {
                Game.ANIMATIONS.finishAllAnimations();
            }
        }

        assertNotEquals(botName, Game.PLAYERS.getTurn().getName());
    }

    @Test
    @Timeout(5)
    void pausePreventsComputerTurnFromAdvancing() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyApp.SKIP_ANNIMATIONS = true;
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        String botName = Game.PLAYERS.getTurn().getName();
        invokeTogglePause(game);

        for (int step = 0; step < 50; step++) {
            runtime.eventBus().flushPendingChanges();
            invokeComputerStep(game);
            runtime.eventBus().flushPendingChanges();
            if (Game.ANIMATIONS.isRunning()) {
                Game.ANIMATIONS.finishAllAnimations();
            }
        }

        assertEquals(botName, Game.PLAYERS.getTurn().getName());
    }

    @Test
    @Timeout(5)
    void pauseCanBeToggledEvenWhilePopupIsVisible() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        runtime.popupService().show("Pause test");
        runtime.eventBus().flushPendingChanges();

        assertTrue(dispatchKeyToGame(game, 'p'));

        String botName = Game.PLAYERS.getTurn().getName();
        for (int step = 0; step < 20; step++) {
            runtime.eventBus().flushPendingChanges();
            invokeComputerStep(game);
        }

        assertEquals(botName, Game.PLAYERS.getTurn().getName());
    }

    @Test
    @Timeout(5)
    void finishingAnimationAppliesBotCooldownBeforeNextStep() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        Game.PLAYERS.switchTurn();

        assertEquals(-1, getLastComputerActionAt(game));

        invokeAnimationFinishCooldown(game, true);

        assertTrue(getLastComputerActionAt(game) >= 0,
                "Finishing a bot animation should add a cooldown before the next computer action");
    }

    @Test
    @Timeout(5)
    void botTurnStartClearsPreviousPlayerDiceState() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyApp.SKIP_ANNIMATIONS = true;
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        Game.DICES.rollDice();
        assertNotNull(Game.DICES.getValue());

        Game.PLAYERS.switchTurn();
        invokeShowRollDiceControl(game);

        assertNull(Game.DICES.getValue(), "Bot turn must not inherit a stale dice result from the previous player");
    }
}
