package fi.monopoly.components;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameBotSimulationTest {
    private static final int SIMULATION_RUNS = 3;
    private static final int MAX_STEPS_PER_RUN = 3000;
    private static final int MAX_STAGNANT_STEPS = 300;
    private static final int MIN_TURN_SWITCHES_PER_RUN = 30;

    @AfterEach
    void tearDown() {
        MonopolyApp.DEBUG_MODE = false;
        MonopolyApp.SKIP_ANNIMATIONS = false;
    }

    @Test
    @Timeout(15)
    void strongBotsDoNotDeadlockAcrossRepeatedSimulations() throws ReflectiveOperationException {
        MonopolyApp.SKIP_ANNIMATIONS = true;

        int stalledRuns = 0;
        int totalTurnSwitches = 0;
        int totalDebtResolutions = 0;
        int totalBankruptcies = 0;
        int completedGames = 0;

        for (int run = 0; run < SIMULATION_RUNS; run++) {
            resetNextPlayerId();
            MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
            Game game = new Game(runtime);
            promoteAllPlayersToStrongBots();
            runtime.eventBus().flushPendingChanges();

            SimulationResult result = simulateGame(runtime, game);
            stalledRuns += result.stalled() ? 1 : 0;
            totalTurnSwitches += result.turnSwitches();
            totalDebtResolutions += result.debtResolutions();
            totalBankruptcies += result.bankruptcies();
            completedGames += result.completedGame() ? 1 : 0;

            assertTrue(!result.stalled(), "Strong bot simulation stalled. " + result);
            assertTrue(result.turnSwitches() >= MIN_TURN_SWITCHES_PER_RUN || result.completedGame(),
                    "Strong bot simulation did not progress enough. " + result);
        }

        double averageTurnSwitches = totalTurnSwitches / (double) SIMULATION_RUNS;
        System.out.println("Strong bot simulation metrics: runs=" + SIMULATION_RUNS
                + ", avgTurnSwitches=" + averageTurnSwitches
                + ", debtResolutions=" + totalDebtResolutions
                + ", bankruptcies=" + totalBankruptcies
                + ", completedGames=" + completedGames
                + ", stalledRuns=" + stalledRuns);

        assertEquals(0, stalledRuns, "No simulation run should stall");
    }

    private static SimulationResult simulateGame(MonopolyRuntime runtime, Game game) throws ReflectiveOperationException {
        int turnSwitches = 0;
        int debtResolutions = 0;
        int bankruptcies = 0;
        int stagnantSteps = 0;
        int previousPlayerCount = Game.players.count();
        String previousTurn = currentTurnName();
        String previousSnapshot = snapshot(runtime);
        boolean debtActiveLastStep = false;

        for (int step = 0; step < MAX_STEPS_PER_RUN; step++) {
            runtime.eventBus().flushPendingChanges();
            invokeComputerStep(game);
            runtime.eventBus().flushPendingChanges();
            if (Game.animations.isRunning()) {
                Game.animations.finishAllAnimations();
            }

            boolean debtActive = isDebtResolutionActive(game);
            if (debtActive && !debtActiveLastStep) {
                debtResolutions++;
            }
            debtActiveLastStep = debtActive;

            int playerCount = Game.players.count();
            if (playerCount < previousPlayerCount) {
                bankruptcies += previousPlayerCount - playerCount;
                previousPlayerCount = playerCount;
            }

            String currentTurn = currentTurnName();
            if (!Objects.equals(previousTurn, currentTurn)) {
                turnSwitches++;
                previousTurn = currentTurn;
            }

            String currentSnapshot = snapshot(runtime);
            if (currentSnapshot.equals(previousSnapshot)) {
                stagnantSteps++;
            } else {
                previousSnapshot = currentSnapshot;
                stagnantSteps = 0;
            }

            if (Game.players.count() <= 1) {
                return new SimulationResult(turnSwitches, debtResolutions, bankruptcies, false, true, currentSnapshot);
            }
            if (stagnantSteps >= MAX_STAGNANT_STEPS) {
                return new SimulationResult(turnSwitches, debtResolutions, bankruptcies, true, false, currentSnapshot);
            }
        }

        return new SimulationResult(turnSwitches, debtResolutions, bankruptcies, false, false, previousSnapshot);
    }

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
        Method method = Game.class.getDeclaredMethod("runComputerPlayerStep");
        method.setAccessible(true);
        method.invoke(game);
    }

    private static boolean isDebtResolutionActive(Game game) throws ReflectiveOperationException {
        Field field = Game.class.getDeclaredField("debtState");
        field.setAccessible(true);
        return field.get(game) != null;
    }

    private static void promoteAllPlayersToStrongBots() throws ReflectiveOperationException {
        Field field = Player.class.getDeclaredField("computerProfile");
        field.setAccessible(true);
        for (Player player : Game.players.getPlayers()) {
            field.set(player, ComputerPlayerProfile.STRONG);
        }
    }

    private static String currentTurnName() {
        Player turn = Game.players.getTurn();
        return turn == null ? "none" : turn.getName();
    }

    private static String snapshot(MonopolyRuntime runtime) {
        String playerState = Game.players.getPlayers().stream()
                .sorted(Comparator.comparingInt(Player::getId))
                .map(player -> player.getId() + ":" + player.getMoneyAmount() + ":" + player.getSpot().getSpotType() + ":" + player.getOwnedProperties().size())
                .collect(Collectors.joining("|"));
        String diceValue = Game.DICES != null && Game.DICES.getValue() != null ? Game.DICES.getValue().toString() : "null";
        String popupKind = runtime.popupService().isAnyVisible() ? runtime.popupService().activePopupKind() : "none";
        return playerState
                + "|turn=" + currentTurnName()
                + "|players=" + Game.players.count()
                + "|popup=" + runtime.popupService().isAnyVisible()
                + "|popupKind=" + popupKind
                + "|diceVisible=" + (Game.DICES != null && Game.DICES.isVisible())
                + "|dice=" + diceValue
                + "|animations=" + (Game.animations != null && Game.animations.isRunning())
                + "|debt=" + Game.isDebtResolutionActive();
    }

    private record SimulationResult(
            int turnSwitches,
            int debtResolutions,
            int bankruptcies,
            boolean stalled,
            boolean completedGame,
            String lastSnapshot
    ) {
    }
}
