package fi.monopoly.components;

import controlP5.ControlP5;
import fi.monopoly.client.desktop.DesktopClientSettings;
import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.components.turn.PropertyAuctionResolver;
import fi.monopoly.domain.session.SessionState;
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
        DesktopClientSettings.setDebugMode(false);
        DesktopClientSettings.setSkipAnimations(false);
    }

    @Test
    @Timeout(value = 20, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void strongBotsDoNotDeadlockAcrossRepeatedSimulations() throws ReflectiveOperationException {
        DesktopClientSettings.setSkipAnimations(true);

        int stalledRuns = 0;
        int totalTurnSwitches = 0;
        int totalDebtResolutions = 0;
        int totalBankruptcies = 0;
        int completedGames = 0;
        int runsWithRecordedCashAt100 = 0;
        double totalAverageCashAt100Turns = 0;
        double totalAuctionDiscountRate = 0;
        double totalWinnerNetWorthShare = 0;
        int totalFirstBankruptcyTurns = 0;
        int runsWithBankruptcy = 0;

        for (int run = 0; run < SIMULATION_RUNS; run++) {
            resetNextPlayerId();
            MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
            Game game = new Game(runtime);
            promoteAllPlayersToStrongBots(game);
            runtime.eventBus().flushPendingChanges();
            PropertyAuctionResolver.resetMetrics();

            SimulationResult result = simulateGame(runtime, game);
            stalledRuns += result.stalled() ? 1 : 0;
            totalTurnSwitches += result.turnSwitches();
            totalDebtResolutions += result.debtResolutions();
            totalBankruptcies += result.bankruptcies();
            completedGames += result.completedGame() ? 1 : 0;
            if (!Double.isNaN(result.averageCashAfter100Turns())) {
                runsWithRecordedCashAt100++;
                totalAverageCashAt100Turns += result.averageCashAfter100Turns();
            }
            totalAuctionDiscountRate += result.auctionDiscountRate();
            if (!Double.isNaN(result.winnerNetWorthShare())) {
                totalWinnerNetWorthShare += result.winnerNetWorthShare();
            }
            if (result.turnsToFirstBankruptcy() >= 0) {
                runsWithBankruptcy++;
                totalFirstBankruptcyTurns += result.turnsToFirstBankruptcy();
            }

            assertFalse(result.stalled(), "Strong bot simulation stalled. " + result);
            assertTrue(result.turnSwitches() >= MIN_TURN_SWITCHES_PER_RUN || result.completedGame(),
                    "Strong bot simulation did not progress enough. " + result);
        }

        double averageTurnSwitches = totalTurnSwitches / (double) SIMULATION_RUNS;
        double averageCashAt100Turns = runsWithRecordedCashAt100 == 0 ? Double.NaN : totalAverageCashAt100Turns / runsWithRecordedCashAt100;
        double averageAuctionDiscountRate = totalAuctionDiscountRate / SIMULATION_RUNS;
        double averageWinnerNetWorthShare = completedGames == 0 ? Double.NaN : totalWinnerNetWorthShare / completedGames;
        double averageTurnsToFirstBankruptcy = runsWithBankruptcy == 0 ? Double.NaN : totalFirstBankruptcyTurns / (double) runsWithBankruptcy;
        System.out.println("Strong bot simulation metrics: runs=" + SIMULATION_RUNS
                + ", avgTurnSwitches=" + averageTurnSwitches
                + ", debtResolutions=" + totalDebtResolutions
                + ", bankruptcies=" + totalBankruptcies
                + ", completedGames=" + completedGames
                + ", avgTurnsToFirstBankruptcy=" + formatMetric(averageTurnsToFirstBankruptcy)
                + ", avgCashAfter100Turns=" + formatMetric(averageCashAt100Turns)
                + ", avgAuctionDiscountRate=" + formatMetric(averageAuctionDiscountRate)
                + ", avgWinnerNetWorthShare=" + formatMetric(averageWinnerNetWorthShare)
                + ", stalledRuns=" + stalledRuns);

        assertEquals(0, stalledRuns, "No simulation run should stall");
    }

    @Test
    @Timeout(value = 15, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void strongBotBeatsSmokeTestBotInHeadsUpAdvantageScenario() throws ReflectiveOperationException {
        DesktopClientSettings.setSkipAnimations(true);

        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        configureHeadsUpProfiles(game, ComputerPlayerProfile.STRONG, ComputerPlayerProfile.SMOKE_TEST);
        PropertyAuctionResolver.resetMetrics();
        Player strongPlayer = players(game).getPlayers().stream()
                .min(Comparator.comparingInt(Player::getTurnNumber))
                .orElseThrow();
        Player smokePlayer = players(game).getPlayers().stream()
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
        int previousPlayerCount = players(game).count();
        String previousTurn = currentTurnName(game);
        String previousSnapshot = snapshot(runtime, game);
        boolean debtActiveLastStep = false;
        int turnsToFirstBankruptcy = -1;
        double averageCashAfter100Turns = Double.NaN;

        for (int step = 0; step < maxSteps; step++) {
            runtime.eventBus().flushPendingChanges();
            invokeComputerStep(game);
            runtime.eventBus().flushPendingChanges();
            if (animations(game).isRunning()) {
                animations(game).finishAllAnimations();
            }

            boolean debtActive = isDebtResolutionActive(game);
            if (debtActive && !debtActiveLastStep) {
                debtResolutions++;
            }
            debtActiveLastStep = debtActive;

            int playerCount = players(game).count();
            if (playerCount < previousPlayerCount) {
                bankruptcies += previousPlayerCount - playerCount;
                if (turnsToFirstBankruptcy < 0) {
                    turnsToFirstBankruptcy = turnSwitches;
                }
                previousPlayerCount = playerCount;
            }

            String currentTurn = currentTurnName(game);
            if (!Objects.equals(previousTurn, currentTurn)) {
                turnSwitches++;
                previousTurn = currentTurn;
                if (Double.isNaN(averageCashAfter100Turns) && turnSwitches >= 100) {
                    averageCashAfter100Turns = averageCash(game);
                }
            }

            String currentSnapshot = snapshot(runtime, game);
            if (currentSnapshot.equals(previousSnapshot)) {
                stagnantSteps++;
            } else {
                previousSnapshot = currentSnapshot;
                stagnantSteps = 0;
            }

            if (players(game).count() <= 1) {
                return new SimulationResult(
                        turnSwitches,
                        debtResolutions,
                        bankruptcies,
                        false,
                        true,
                        currentSnapshot,
                        currentTurnName(game),
                        turnsToFirstBankruptcy,
                        averageCashAfter100Turns,
                        PropertyAuctionResolver.metrics().discountRate(),
                        winnerNetWorthShare(game)
                );
            }
            if (stagnantSteps >= maxStagnantSteps) {
                return new SimulationResult(
                        turnSwitches,
                        debtResolutions,
                        bankruptcies,
                        true,
                        false,
                        currentSnapshot,
                        currentTurnName(game),
                        turnsToFirstBankruptcy,
                        averageCashAfter100Turns,
                        PropertyAuctionResolver.metrics().discountRate(),
                        Double.NaN
                );
            }
        }

        return new SimulationResult(
                turnSwitches,
                debtResolutions,
                bankruptcies,
                false,
                false,
                previousSnapshot,
                currentTurnName(game),
                turnsToFirstBankruptcy,
                averageCashAfter100Turns,
                PropertyAuctionResolver.metrics().discountRate(),
                Double.NaN
        );
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

    private static Players players(Game game) {
        return game.testFacade().players();
    }

    private static fi.monopoly.components.animation.Animations animations(Game game) {
        return game.testFacade().animations();
    }

    private static fi.monopoly.components.dices.Dices dices(Game game) {
        return game.testFacade().dices();
    }

    private static fi.monopoly.presentation.session.debt.DebtController debtController(Game game) {
        return game.testFacade().debtController();
    }

    private static void invokeComputerStep(Game game) throws ReflectiveOperationException {
        game.testFacade().runComputerPlayerStep();
    }

    private static boolean isDebtResolutionActive(Game game) {
        return debtController(game).debtState() != null;
    }

    private static void promoteAllPlayersToStrongBots(Game game) throws ReflectiveOperationException {
        Field field = Player.class.getDeclaredField("computerProfile");
        field.setAccessible(true);
        for (Player player : players(game).getPlayers()) {
            field.set(player, ComputerPlayerProfile.STRONG);
        }
    }

    private static void configureHeadsUpProfiles(Game game, ComputerPlayerProfile firstProfile, ComputerPlayerProfile secondProfile) throws ReflectiveOperationException {
        Field field = Player.class.getDeclaredField("computerProfile");
        field.setAccessible(true);
        var players = players(game).getPlayers().stream()
                .sorted(Comparator.comparingInt(Player::getTurnNumber))
                .collect(Collectors.toCollection(java.util.ArrayList::new));
        while (players.size() > 2) {
            Player removed = players.remove(players.size() - 1);
            GameBotSimulationTest.players(game).removePlayer(removed);
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
        smokePlayer.setSpot(game.testFacade().board().getSpots().stream()
                .filter(spot -> spot.getSpotType() == SpotType.RR2)
                .findFirst()
                .orElseThrow());
        players(game).focusPlayer(strongPlayer);
    }

    private static String currentTurnName(Game game) {
        Player turn = players(game).getTurn();
        return turn == null ? "none" : turn.getName();
    }

    private static String snapshot(MonopolyRuntime runtime, Game game) {
        SessionState sessionState = game.testFacade().projectedSessionState();
        String playerState = players(game).getPlayers().stream()
                .sorted(Comparator.comparingInt(Player::getId))
                .map(player -> player.getId() + ":" + player.getMoneyAmount() + ":" + player.getSpot().getSpotType() + ":" + player.getOwnedProperties().size())
                .collect(Collectors.joining("|"));
        String diceValue = dices(game) != null && dices(game).getValue() != null ? dices(game).getValue().toString() : "null";
        String popupKind = runtime.popupService().isAnyVisible() ? runtime.popupService().activePopupKind() : "none";
        String popupMessage = runtime.popupService().isAnyVisible() ? runtime.popupService().activePopupMessage() : "none";
        return playerState
                + "|turn=" + currentTurnName(game)
                + "|players=" + players(game).count()
                + "|popup=" + runtime.popupService().isAnyVisible()
                + "|popupKind=" + popupKind
                + "|popupMessage=" + popupMessage
                + "|diceVisible=" + (dices(game) != null && dices(game).isVisible())
                + "|dice=" + diceValue
                + "|animations=" + (animations(game) != null && animations(game).isRunning())
                + "|debt=" + (debtController(game).debtState() != null)
                + "|phase=" + sessionState.turn().phase()
                + "|pendingDecision=" + (sessionState.pendingDecision() != null ? sessionState.pendingDecision().decisionType() : "none")
                + "|auctionStatus=" + (sessionState.auctionState() != null ? sessionState.auctionState().status() : "none")
                + "|auctionActor=" + (sessionState.auctionState() != null ? sessionState.auctionState().currentActorPlayerId() : "none")
                + "|auctionWinner=" + (sessionState.auctionState() != null ? sessionState.auctionState().winningPlayerId() : "none");
    }

    private static double averageCash(Game game) {
        return players(game).getPlayers().stream()
                .mapToInt(Player::getMoneyAmount)
                .average()
                .orElse(Double.NaN);
    }

    private static double winnerNetWorthShare(Game game) {
        if (players(game).count() != 1) {
            return Double.NaN;
        }
        int totalNetWorth = players(game).getPlayers().stream()
                .mapToInt(GameBotSimulationTest::netWorth)
                .sum();
        if (totalNetWorth <= 0) {
            return Double.NaN;
        }
        Player winner = players(game).getPlayers().get(0);
        return netWorth(winner) / (double) totalNetWorth;
    }

    private static int netWorth(Player player) {
        return player.getMoneyAmount() + player.getTotalLiquidationValue();
    }

    private static String formatMetric(double value) {
        if (Double.isNaN(value)) {
            return "n/a";
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private record SimulationResult(
            int turnSwitches,
            int debtResolutions,
            int bankruptcies,
            boolean stalled,
            boolean completedGame,
            String lastSnapshot,
            String winnerName,
            int turnsToFirstBankruptcy,
            double averageCashAfter100Turns,
            double auctionDiscountRate,
            double winnerNetWorthShare
    ) {
    }
}
