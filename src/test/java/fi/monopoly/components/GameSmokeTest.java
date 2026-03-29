package fi.monopoly.components;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.payment.DebtState;
import org.junit.jupiter.api.*;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;
import processing.event.KeyEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static processing.event.KeyEvent.PRESS;

/**
 * Headless smoke test for the full game loop.
 *
 * <p>The test boots the game with a minimal Processing graphics context, drives it with a tiny
 * set of synthetic key presses, and verifies that the game can keep progressing through rolls,
 * animations and popup flows without getting stuck.</p>
 */
class GameSmokeTest {
    private static final int STANDARD_ROLL_TARGET = 100;
    private static final int RESIZE_SMOKE_ROLL_TARGET = 40;
    private static final int MAX_STAGNANT_STEPS = 250;
    private static final int MIN_TURN_SWITCHES = 15;
    private static final int MIN_UNIQUE_SPOTS = 12;

    private static void runAutoConfirmedRollSmokeTest(int targetRollCount) {
        runAutoConfirmedRollSmokeTest(targetRollCount, 1700, 996, false);
    }

    private static void runAutoConfirmedRollSmokeTest(int targetRollCount, int width, int height, boolean verifyResponsiveUi) {
        resetNextPlayerId();
        MonopolyApp.SKIP_ANNIMATIONS = true;
        MonopolyRuntime runtime = initHeadlessRuntime(width, height);
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        int initialPlayerCount = Game.players.count();
        int rollCount = 0;
        int popupResolutionCount = 0;
        int debtResolutionCount = 0;
        int bankruptcyCount = 0;
        int turnSwitchCount = 0;
        int stagnantStepCount = 0;
        boolean completedGame = false;
        Set<String> seenTurnPlayers = new HashSet<>();
        Set<String> seenSpotTypes = new HashSet<>();
        String previousTurnName = currentTurnName();
        String previousSnapshot = snapshot();

        // Drive the game with the same small input set a player would use:
        // resolve animations, accept popups, otherwise press space to keep turns moving.
        while (rollCount < targetRollCount) {
            runtime.eventBus().flushPendingChanges();
            invokePrimaryControlInvariant(game);
            if (verifyResponsiveUi) {
                assertResponsiveUiState(game, runtime);
            }

            if (Game.animations.isRunning()) {
                Game.animations.finishAllAnimations();
            } else if (runtime.popupService().isAnyVisible()) {
                popupResolutionCount++;
                dispatchKey(runtime, '1');
            } else if (isDebtResolutionActive(game)) {
                debtResolutionCount++;
                if (isBankruptcyRisk(game)) {
                    bankruptcyCount++;
                    invokeDeclareBankruptcy(game);
                } else {
                    topUpDebtDebtorCash(game);
                    invokeRetryPendingDebtPayment(game);
                }
            } else {
                Object previousDiceValue = Game.DICES.getValue();
                dispatchKey(runtime, MonopolyApp.SPACE);
                if (Game.DICES.getValue() != null && Game.DICES.getValue() != previousDiceValue) {
                    rollCount++;
                }
            }

            runtime.eventBus().flushPendingChanges();
            invokePrimaryControlInvariant(game);
            if (verifyResponsiveUi) {
                assertResponsiveUiState(game, runtime);
            }
            assertCoreInvariants(game, initialPlayerCount);

            String currentTurnName = currentTurnName();
            if (currentTurnName != null) {
                seenTurnPlayers.add(currentTurnName);
            }
            String currentSpotType = currentTurnSpotType();
            if (currentSpotType != null) {
                seenSpotTypes.add(currentSpotType);
            }
            if (!Objects.equals(previousTurnName, currentTurnName)) {
                turnSwitchCount++;
                previousTurnName = currentTurnName;
            }

            String currentSnapshot = snapshot();
            if (currentSnapshot.equals(previousSnapshot)) {
                stagnantStepCount++;
            } else {
                stagnantStepCount = 0;
                previousSnapshot = currentSnapshot;
            }

            if (Game.players.count() <= 1) {
                completedGame = true;
                break;
            }
            assertTrue(stagnantStepCount < MAX_STAGNANT_STEPS, "Game appears stuck. Snapshot: " + currentSnapshot);
        }

        // Finish any last popup/animation chain before asserting the end state.
        settlePendingGameFlow(runtime, game, verifyResponsiveUi);
        assertCoreInvariants(game, initialPlayerCount);

        assertTrue(rollCount >= targetRollCount || completedGame,
                "Game neither completed the expected number of rolls nor reached a finished state");
        assertEquals(3, initialPlayerCount, "Smoke test expects the default three-player setup");
        assertTrue(popupResolutionCount > 0, "Smoke test should encounter at least one popup");
        assertTrue(turnSwitchCount >= MIN_TURN_SWITCHES || completedGame,
                "Turns did not appear to advance often enough");
        assertTrue(seenTurnPlayers.size() >= 2, "Smoke test should rotate through at least two players");
        assertTrue(seenSpotTypes.size() >= MIN_UNIQUE_SPOTS, "Game did not traverse enough of the board to be a useful sanity check");
        assertTrue(debtResolutionCount >= bankruptcyCount, "Bankruptcy count cannot exceed debt resolutions");
        assertFalse(Game.animations.isRunning(), "Animations should not be left running at the end of the smoke test");
        assertFalse(runtime.popupService().isAnyVisible(), "Popup should not be left open at the end of the smoke test");
        assertFalse(isDebtResolutionActive(game), "Debt resolution should not be left active at the end of the smoke test");

        System.out.println(buildPlayerSummary());
    }

    private static void settlePendingGameFlow(MonopolyRuntime runtime, Game game, boolean verifyResponsiveUi) {
        // Drain any popup/animation tail so the test does not stop mid-turn.
        for (int step = 0; step < 500; step++) {
            runtime.eventBus().flushPendingChanges();
            invokePrimaryControlInvariant(game);
            if (verifyResponsiveUi) {
                assertResponsiveUiState(game, runtime);
            }
            if (Game.animations.isRunning()) {
                Game.animations.finishAllAnimations();
                continue;
            }
            if (runtime.popupService().isAnyVisible()) {
                dispatchKey(runtime, '1');
                continue;
            }
            if (isDebtResolutionActive(game)) {
                if (isBankruptcyRisk(game)) {
                    invokeDeclareBankruptcy(game);
                } else {
                    topUpDebtDebtorCash(game);
                    invokeRetryPendingDebtPayment(game);
                }
                continue;
            }
            return;
        }
    }

    private static void assertResponsiveUiState(Game game, MonopolyRuntime runtime) {
        invokeNoArgMethod(game, "updateSidebarControlPositions");

        MonopolyButton endRoundButton = getEndRoundButton(game);
        assertTrue(endRoundButton.getPosition()[0] + endRoundButton.getWidth() <= runtime.app().width,
                "End turn button should stay inside the window");

        MonopolyButton languageButton = getButton(game, "languageButton");
        assertTrue(languageButton.getPosition()[0] >= 0, "Language button should stay inside the window");
        assertTrue(languageButton.getPosition()[1] + languageButton.getHeight() <= runtime.app().height,
                "Language button should stay above the bottom edge");

        fi.monopoly.components.popup.Popup activePopup = getActivePopup(runtime);
        if (activePopup != null) {
            int popupWidth = (int) invokeNoArgMethod(activePopup, fi.monopoly.components.popup.Popup.class, "getPopupWidth");
            int popupHeight = (int) invokeNoArgMethod(activePopup, fi.monopoly.components.popup.Popup.class, "getPopupHeight");
            assertTrue(popupWidth <= runtime.app().width - 64, "Popup width should fit the current window");
            assertTrue(popupHeight <= runtime.app().height - 64, "Popup height should fit the current window");
        }
    }

    private static void assertCoreInvariants(Game game, int initialPlayerCount) {
        assertNotNull(Game.players, "Players collection should exist");
        assertNotNull(Game.DICES, "Dice controls should exist");
        assertNotNull(Game.animations, "Animation controller should exist");

        int playerCount = Game.players.count();
        assertTrue(playerCount >= 1, "At least one player should remain in the game");
        assertTrue(playerCount <= initialPlayerCount, "Player count should not increase during the game");

        List<Player> players = getPlayers();
        assertEquals(playerCount, players.size(), "Player list size should match Players.count()");
        assertNotNull(Game.players.getTurn(), "Current turn player should always exist while the game is running");
        assertTrue(players.contains(Game.players.getTurn()), "Turn player should belong to the active player list");

        Set<Integer> playerIds = new HashSet<>();
        Set<Integer> turnNumbers = new HashSet<>();
        for (Player player : players) {
            assertTrue(playerIds.add(player.getId()), "Player ids should stay unique");
            assertTrue(turnNumbers.add(player.getTurnNumber()), "Turn numbers should stay unique");
            assertNotNull(player.getSpot(), "Each player should always occupy a spot");
            assertTrue(player.getMoneyAmount() >= 0, "Player money should not go negative");
            if (player.isInJail()) {
                assertEquals(fi.monopoly.types.SpotType.JAIL, player.getSpot().getSpotType(), "Jailed players should be on the jail spot");
            }
        }

        MonopolyButton endRoundButton = getEndRoundButton(game);
        assertFalse(endRoundButton.isVisible() && Game.DICES.isVisible(), "Roll dice and end turn controls cannot both be visible");
        if (isDebtResolutionActive(game)) {
            assertFalse(Game.DICES.isVisible(), "Roll dice should be hidden during debt resolution");
            assertFalse(endRoundButton.isVisible(), "End turn should be hidden during debt resolution");
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Player> getPlayers() {
        try {
            Field field = Players.class.getDeclaredField("playerList");
            field.setAccessible(true);
            return List.copyOf((List<Player>) field.get(Game.players));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static MonopolyButton getEndRoundButton(Game game) {
        return getButton(game, "endRoundButton");
    }

    private static MonopolyButton getButton(Game game, String fieldName) {
        try {
            Field field = Game.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (MonopolyButton) field.get(game);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static MonopolyRuntime initHeadlessRuntime() {
        return initHeadlessRuntime(1700, 996);
    }

    private static MonopolyRuntime initHeadlessRuntime(int width, int height) {
        MonopolyApp app = new MonopolyApp();
        app.width = width;
        app.height = height;

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

    private static boolean isDebtResolutionActive(Game game) {
        return getDebtState(game) != null;
    }

    private static boolean isBankruptcyRisk(Game game) {
        DebtState debtState = getDebtState(game);
        return debtState != null && debtState.bankruptcyRisk();
    }

    private static DebtState getDebtState(Game game) {
        try {
            Field field = Game.class.getDeclaredField("debtState");
            field.setAccessible(true);
            return (DebtState) field.get(game);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void topUpDebtDebtorCash(Game game) {
        DebtState debtState = getDebtState(game);
        if (debtState == null) {
            return;
        }
        int missingCash = Math.max(0, debtState.paymentRequest().amount() - debtState.paymentRequest().debtor().getMoneyAmount());
        if (missingCash > 0) {
            assertTrue(debtState.paymentRequest().debtor().addMoney(missingCash), "Smoke test cash top-up should always succeed");
        }
    }

    private static void invokeRetryPendingDebtPayment(Game game) {
        invokeNoArgMethod(game, "retryPendingDebtPayment");
    }

    private static void invokeDeclareBankruptcy(Game game) {
        invokeNoArgMethod(game, "declareBankruptcy");
    }

    private static void invokePrimaryControlInvariant(Game game) {
        invokeNoArgMethod(game, "enforcePrimaryTurnControlInvariant");
    }

    private static void invokeNoArgMethod(Game game, String methodName) {
        try {
            Method method = Game.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(game);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object invokeNoArgMethod(Object target, Class<?> declaringClass, String methodName) {
        try {
            Method method = declaringClass.getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static fi.monopoly.components.popup.Popup getActivePopup(MonopolyRuntime runtime) {
        try {
            Field field = runtime.popupService().getClass().getDeclaredField("activePopup");
            field.setAccessible(true);
            return (fi.monopoly.components.popup.Popup) field.get(runtime.popupService());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void resetNextPlayerId() {
        try {
            Field field = Player.class.getDeclaredField("NEXT_ID");
            field.setAccessible(true);
            field.setInt(null, 0);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static String currentTurnName() {
        Player turn = Game.players.getTurn();
        return turn != null ? turn.getName() : null;
    }

    private static String currentTurnSpotType() {
        Player turn = Game.players.getTurn();
        return turn != null && turn.getSpot() != null ? turn.getSpot().getSpotType().name() : null;
    }

    private static String buildPlayerSummary() {
        return getPlayers().stream()
                .map(player -> player.getName()
                        + ": money=" + player.getMoneyAmount()
                        + ", spot=" + player.getSpot().getSpotType().name()
                        + ", properties=" + formatProperties(player))
                .collect(Collectors.joining(System.lineSeparator(), "Smoke test player summary:" + System.lineSeparator(), ""));
    }

    private static String formatProperties(Player player) {
        if (player.getOwnedProperties().isEmpty()) {
            return "[]";
        }
        return player.getOwnedProperties().stream()
                .map(property -> property.getSpotType().name())
                .sorted()
                .collect(Collectors.joining(", ", "[", "]"));
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
                + "|animations=" + Game.animations.isRunning()
                + "|players=" + Game.players.count()
                + "|debt=" + Game.isDebtResolutionActive();
    }

    @AfterEach
    void tearDown() {
        MonopolyApp.DEBUG_MODE = false;
        MonopolyApp.SKIP_ANNIMATIONS = false;
        fi.monopoly.components.spots.JailSpot.jailTimeLeftMap.clear();
    }

    @Test
    @Timeout(5)
    void gameCanPlayHundredAutoConfirmedRollsWithoutGettingStuck() {
        runAutoConfirmedRollSmokeTest(STANDARD_ROLL_TARGET);
    }

    @Test
    @Timeout(5)
    void gameCanRenderAndProgressInNarrowWindowLayout() {
        runAutoConfirmedRollSmokeTest(RESIZE_SMOKE_ROLL_TARGET, 1200, 996, true);
    }

    @Test
    @Timeout(5)
    void gameCanRenderAndProgressInShortWindowLayout() {
        runAutoConfirmedRollSmokeTest(RESIZE_SMOKE_ROLL_TARGET, 1700, 560, true);
    }

    @Test
    @Disabled("Optional longer-running smoke test for local/manual runs")
    @Timeout(20)
    void gameCanPlayExtendedAutoConfirmedRollSequenceWithoutGettingStuck() {
        runAutoConfirmedRollSmokeTest(1_000_000);
    }
}
