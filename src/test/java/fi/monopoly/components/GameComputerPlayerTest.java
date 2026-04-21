package fi.monopoly.components;

import controlP5.ControlP5;
import fi.monopoly.client.desktop.DesktopClientSettings;
import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.host.bot.BotTurnScheduler;
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
        game.testFacade().runComputerPlayerStep();
    }

    private static void invokeTogglePause(Game game) throws ReflectiveOperationException {
        game.testFacade().togglePause();
    }

    private static void invokeAnimationFinishCooldown(Game game, boolean animationWasRunning) throws ReflectiveOperationException {
        game.testFacade().applyComputerActionCooldownIfAnimationJustFinished(animationWasRunning);
    }

    private static void invokeShowRollDiceControl(Game game) throws ReflectiveOperationException {
        game.testFacade().showRollDiceControl();
    }

    private static BotTurnScheduler getBotTurnScheduler(Game game) throws ReflectiveOperationException {
        return game.testFacade().botTurnScheduler();
    }

    private static boolean dispatchKeyToGame(Game game, char key) {
        return game.onEvent(new KeyEvent(new Object(), System.currentTimeMillis(), PRESS, 0, key, key));
    }

    private static Players players(Game game) {
        return game.testFacade().players();
    }

    private static fi.monopoly.components.animation.Animations animations(Game game) {
        return game.testFacade().animations();
    }

    private static fi.monopoly.components.dices.Dices dices(Game game) {
        return game.testFacade().dices();
    }

    @SuppressWarnings("unchecked")
    private static List<Player> getPlayerList(Players players) throws ReflectiveOperationException {
        Field field = Players.class.getDeclaredField("playerList");
        field.setAccessible(true);
        return (List<Player>) field.get(players);
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
        DesktopClientSettings.setDebugMode(false);
        DesktopClientSettings.setSkipAnimations(false);
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void defaultSeatsUseStrongBotProfile() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);

        List<Player> players = getPlayerList(players(game));
        long computerPlayerCount = players.stream().filter(Player::isComputerControlled).count();
        long strongBotCount = players.stream()
                .filter(player -> player.getComputerProfile() == ComputerPlayerProfile.STRONG)
                .count();

        assertEquals(3, players.size());
        assertTrue(computerPlayerCount >= 1);
        assertEquals(computerPlayerCount, strongBotCount);
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void defaultBotCanFinishItsTurnWithoutUserInput() throws ReflectiveOperationException {
        resetNextPlayerId();
        DesktopClientSettings.setSkipAnimations(true);
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        players(game).switchTurn();
        players(game).switchTurn();
        Player bot = players(game).getTurn();
        String botName = bot.getName();

        for (int step = 0; step < 200 && Objects.equals(botName, players(game).getTurn().getName()); step++) {
            runtime.eventBus().flushPendingChanges();
            invokeComputerStep(game);
            runtime.eventBus().flushPendingChanges();
            if (animations(game).isRunning()) {
                animations(game).finishAllAnimations();
            }
        }

        assertNotEquals(botName, players(game).getTurn().getName());
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void pausePreventsComputerTurnFromAdvancing() throws ReflectiveOperationException {
        resetNextPlayerId();
        DesktopClientSettings.setSkipAnimations(true);
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        String botName = players(game).getTurn().getName();
        invokeTogglePause(game);

        for (int step = 0; step < 50; step++) {
            runtime.eventBus().flushPendingChanges();
            invokeComputerStep(game);
            runtime.eventBus().flushPendingChanges();
            if (animations(game).isRunning()) {
                animations(game).finishAllAnimations();
            }
        }

        assertEquals(botName, players(game).getTurn().getName());
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void pauseCanBeToggledEvenWhilePopupIsVisible() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        runtime.popupService().show("Pause test");
        runtime.eventBus().flushPendingChanges();

        assertTrue(dispatchKeyToGame(game, 'p'));

        String botName = players(game).getTurn().getName();
        for (int step = 0; step < 20; step++) {
            runtime.eventBus().flushPendingChanges();
            invokeComputerStep(game);
        }

        assertEquals(botName, players(game).getTurn().getName());
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void finishingAnimationAppliesBotCooldownBeforeNextStep() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        players(game).switchTurn();
        BotTurnScheduler scheduler = getBotTurnScheduler(game);
        int now = runtime.app().millis();

        assertFalse(scheduler.isWaiting(now));

        invokeAnimationFinishCooldown(game, true);

        assertTrue(scheduler.isWaiting(now),
                "Finishing a bot animation should add a cooldown before the next computer action");
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void botTurnStartClearsPreviousPlayerDiceState() throws ReflectiveOperationException {
        resetNextPlayerId();
        DesktopClientSettings.setSkipAnimations(true);
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        dices(game).rollDice();
        assertNotNull(dices(game).getValue());

        players(game).switchTurn();
        invokeShowRollDiceControl(game);

        assertNull(dices(game).getValue(), "Bot turn must not inherit a stale dice result from the previous player");
    }
}
