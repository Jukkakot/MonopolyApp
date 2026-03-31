package fi.monopoly.components;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.support.TestLogLevels;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GameBotSimulationTest {
    private static final int SIMULATION_RUNS = 3;
    private static final int MAX_STEPS_PER_RUN = 3000;
    private static final int MAX_STAGNANT_STEPS = 300;
    private static final int MIN_TURN_SWITCHES_PER_RUN = 30;
    private static final int HEADS_UP_STRONG_WIN_MAX_STEPS = 400;
    private TestLogLevels.LogConfigSnapshot logConfigSnapshot;

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

            assertFalse(result.stalled(), "Strong bot simulation stalled. " + result);
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

    @Test
    @Timeout(10)
    void strongBotBeatsSmokeTestBotInHeadsUpAdvantageScenario() throws ReflectiveOperationException {
        MonopolyApp.SKIP_ANNIMATIONS = true;

        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        configureHeadsUpProfiles(ComputerPlayerProfile.STRONG, ComputerPlayerProfile.SMOKE_TEST);
        Player strongPlayer = Game.PLAYERS.getPlayers().stream()
                .min(Comparator.comparingInt(Player::getTurnNumber))
                .orElseThrow();
        Player smokePlayer = Game.PLAYERS.getPlayers().stream()
                .filter(player -> player != strongPlayer)
                .findFirst()
                .orElseThrow();
        setupStrongAdvantageScenario(game, strongPlayer, smokePlayer);
        runtime.eventBus().flushPendingChanges();

        SimulationResult result = simulateGame(runtime, game, HEADS_UP_STRONG_WIN_MAX_STEPS, MAX_STAGNANT_STEPS);

        assertFalse(result.stalled(), "Heads-up strong vs smoke simulation stalled. " + result);
        assertTrue(result.completedGame(), "Heads-up strong vs smoke simulation did not finish in time. " + result);
        assertEquals(strongPlayer.getName(), result.winnerName(), "Strong bot should win the configured heads-up scenario");
    }

    private static SimulationResult simulateGame(MonopolyRuntime runtime, Game game) throws ReflectiveOperationException {
        return simulateGame(runtime, game, MAX_STEPS_PER_RUN, MAX_STAGNANT_STEPS);
    }

    private static SimulationResult simulateGame(MonopolyRuntime runtime, Game game, int maxSteps, int maxStagnantSteps) throws ReflectiveOperationException {
        int turnSwitches = 0;
        int debtResolutions = 0;
        int bankruptcies = 0;
        int stagnantSteps = 0;
        int previousPlayerCount = Game.PLAYERS.count();
        String previousTurn = currentTurnName();
        String previousSnapshot = snapshot(runtime);
        boolean debtActiveLastStep = false;

        for (int step = 0; step < maxSteps; step++) {
            runtime.eventBus().flushPendingChanges();
            invokeComputerStep(game);
            runtime.eventBus().flushPendingChanges();
            if (Game.ANIMATIONS.isRunning()) {
                Game.ANIMATIONS.finishAllAnimations();
            }

            boolean debtActive = isDebtResolutionActive(game);
            if (debtActive && !debtActiveLastStep) {
                debtResolutions++;
            }
            debtActiveLastStep = debtActive;

            int playerCount = Game.PLAYERS.count();
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

            if (Game.PLAYERS.count() <= 1) {
                return new SimulationResult(turnSwitches, debtResolutions, bankruptcies, false, true, currentSnapshot, currentTurnName());
            }
            if (stagnantSteps >= maxStagnantSteps) {
                return new SimulationResult(turnSwitches, debtResolutions, bankruptcies, true, false, currentSnapshot, currentTurnName());
            }
        }

        return new SimulationResult(turnSwitches, debtResolutions, bankruptcies, false, false, previousSnapshot, currentTurnName());
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
        for (Player player : Game.PLAYERS.getPlayers()) {
            field.set(player, ComputerPlayerProfile.STRONG);
        }
    }

    private static void configureHeadsUpProfiles(ComputerPlayerProfile firstProfile, ComputerPlayerProfile secondProfile) throws ReflectiveOperationException {
        Field field = Player.class.getDeclaredField("computerProfile");
        field.setAccessible(true);
        var players = Game.PLAYERS.getPlayers().stream()
                .sorted(Comparator.comparingInt(Player::getTurnNumber))
                .collect(Collectors.toCollection(java.util.ArrayList::new));
        while (players.size() > 2) {
            Player removed = players.remove(players.size() - 1);
            Game.PLAYERS.removePlayer(removed);
        }
        field.set(players.get(0), firstProfile);
        field.set(players.get(1), secondProfile);
    }

    private static void setupStrongAdvantageScenario(Game game, Player strongPlayer, Player smokePlayer) {
        TestObjectFactory.giveProperty(strongPlayer, PropertyFactory.getProperty(SpotType.O1));
        TestObjectFactory.giveProperty(strongPlayer, PropertyFactory.getProperty(SpotType.O2));
        TestObjectFactory.giveProperty(strongPlayer, PropertyFactory.getProperty(SpotType.O3));
        TestObjectFactory.giveProperty(strongPlayer, PropertyFactory.getProperty(SpotType.RR1));
        TestObjectFactory.giveProperty(strongPlayer, PropertyFactory.getProperty(SpotType.RR2));
        TestObjectFactory.giveProperty(smokePlayer, PropertyFactory.getProperty(SpotType.B1));

        StreetProperty o1 = (StreetProperty) PropertyFactory.getProperty(SpotType.O1);
        assertTrue(o1.buyBuildingRoundsAcrossSet(2));

        strongPlayer.addMoney(700);
        smokePlayer.addMoney(-(smokePlayer.getMoneyAmount() - 180));
        smokePlayer.setSpot(game.getBoard().getSpots().stream()
                .filter(spot -> spot.getSpotType() == SpotType.RR2)
                .findFirst()
                .orElseThrow());
        Game.PLAYERS.focusPlayer(strongPlayer);
    }

    private static String currentTurnName() {
        Player turn = Game.PLAYERS.getTurn();
        return turn == null ? "none" : turn.getName();
    }

    private static String snapshot(MonopolyRuntime runtime) {
        String playerState = Game.PLAYERS.getPlayers().stream()
                .sorted(Comparator.comparingInt(Player::getId))
                .map(player -> player.getId() + ":" + player.getMoneyAmount() + ":" + player.getSpot().getSpotType() + ":" + player.getOwnedProperties().size())
                .collect(Collectors.joining("|"));
        String diceValue = Game.DICES != null && Game.DICES.getValue() != null ? Game.DICES.getValue().toString() : "null";
        String popupKind = runtime.popupService().isAnyVisible() ? runtime.popupService().activePopupKind() : "none";
        String popupMessage = runtime.popupService().isAnyVisible() ? runtime.popupService().activePopupMessage() : "none";
        return playerState
                + "|turn=" + currentTurnName()
                + "|players=" + Game.PLAYERS.count()
                + "|popup=" + runtime.popupService().isAnyVisible()
                + "|popupKind=" + popupKind
                + "|popupMessage=" + popupMessage
                + "|diceVisible=" + (Game.DICES != null && Game.DICES.isVisible())
                + "|dice=" + diceValue
                + "|animations=" + (Game.ANIMATIONS != null && Game.ANIMATIONS.isRunning())
                + "|debt=" + Game.isDebtResolutionActive();
    }

    private record SimulationResult(
            int turnSwitches,
            int debtResolutions,
            int bankruptcies,
            boolean stalled,
            boolean completedGame,
            String lastSnapshot,
            String winnerName
    ) {
    }
}
