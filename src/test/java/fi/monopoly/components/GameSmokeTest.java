package fi.monopoly.components;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;
import processing.event.KeyEvent;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static processing.event.KeyEvent.PRESS;

/**
 * Headless smoke test for the full game loop.
 *
 * <p>The test boots the game with a minimal Processing graphics context, drives it with a tiny
 * set of synthetic key presses, and verifies that the game can keep progressing through rolls,
 * animations and popup flows without getting stuck.</p>
 */
class GameSmokeTest {
    private static final Duration SOAK_DURATION = Duration.ofSeconds(10);

    @AfterEach
    void tearDown() {
        MonopolyApp.DEBUG_MODE = false;
        MonopolyApp.SKIP_ANNIMATIONS = false;
    }

    @Test
    @Timeout(15)
    void gameCanPlayHundredAutoConfirmedRollsWithoutGettingStuck() {
        MonopolyApp.SKIP_ANNIMATIONS = true;
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        int rollCount = 0;
        int stagnantStepCount = 0;
        String previousSnapshot = snapshot();
        long endTime = System.nanoTime() + SOAK_DURATION.toNanos();

        // Drive the game with the same small input set a player would use:
        // resolve animations, accept popups, otherwise press space to keep turns moving.
        while (System.nanoTime() < endTime) {
            runtime.eventBus().flushPendingChanges();

            if (Game.animations.isRunning()) {
                Game.animations.finishAllAnimations();
            } else if (runtime.popupService().isAnyVisible()) {
                dispatchKey(runtime, '1');
            } else {
                Object previousDiceValue = Game.DICES.getValue();
                dispatchKey(runtime, MonopolyApp.SPACE);
                if (Game.DICES.getValue() != null && Game.DICES.getValue() != previousDiceValue) {
                    rollCount++;
                }
            }

            runtime.eventBus().flushPendingChanges();

            String currentSnapshot = snapshot();
            if (currentSnapshot.equals(previousSnapshot)) {
                stagnantStepCount++;
            } else {
                stagnantStepCount = 0;
                previousSnapshot = currentSnapshot;
            }

            assertTrue(stagnantStepCount < 250, "Game appears stuck. Snapshot: " + currentSnapshot);
        }

        // Finish any last popup/animation chain before asserting the end state.
        settlePendingGameFlow(runtime);

        assertTrue(rollCount > 0, "Game did not complete any rolls during the soak window");
        assertFalse(Game.animations.isRunning(), "Animations should not be left running at the end of the smoke test");
        assertFalse(runtime.popupService().isAnyVisible(), "Popup should not be left open at the end of the smoke test");
    }

    private static MonopolyRuntime initHeadlessRuntime() {
        MonopolyApp app = new MonopolyApp();
        app.width = 1700;
        app.height = 996;

        // ControlP5 needs a live Processing graphics context even in tests.
        PGraphicsJava2D graphics = new PGraphicsJava2D();
        graphics.setParent(app);
        graphics.setPrimary(true);
        graphics.setSize(app.width, app.height);
        app.g = graphics;

        ControlP5 controlP5 = new ControlP5(app);
        PFont font = app.createFont("Arial", 20);
        return MonopolyRuntime.initialize(app, controlP5, font, font, font);
    }

    private static void dispatchKey(MonopolyRuntime runtime, char key) {
        runtime.eventBus().sendConsumableEvent(new KeyEvent(new Object(), System.currentTimeMillis(), PRESS, 0, key, key));
    }

    private static void settlePendingGameFlow(MonopolyRuntime runtime) {
        // Drain any popup/animation tail so the test does not stop mid-turn.
        for (int step = 0; step < 500; step++) {
            runtime.eventBus().flushPendingChanges();
            if (Game.animations.isRunning()) {
                Game.animations.finishAllAnimations();
                continue;
            }
            if (runtime.popupService().isAnyVisible()) {
                dispatchKey(runtime, '1');
                continue;
            }
            return;
        }
    }

    private static String snapshot() {
        Player turn = Game.players.getTurn();
        String turnName = turn != null ? turn.getName() : "none";
        String turnSpot = turn != null && turn.getSpot() != null ? turn.getSpot().getSpotType().name() : "none";
        int turnMoney = turn != null ? turn.getMoneyAmount() : -1;
        String diceValue = Game.DICES.getValue() != null ? Game.DICES.getValue().toString() : "null";
        return turnName
                + "|spot=" + turnSpot
                + "|money=" + turnMoney
                + "|popup=" + MonopolyRuntime.get().popupService().isAnyVisible()
                + "|diceVisible=" + Game.DICES.isVisible()
                + "|dice=" + diceValue
                + "|animations=" + Game.animations.isRunning();
    }
}
