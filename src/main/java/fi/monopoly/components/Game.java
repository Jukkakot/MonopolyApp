package fi.monopoly.components;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.animation.Animation;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.board.Path;
import fi.monopoly.components.dices.DiceValue;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.payment.*;
import fi.monopoly.components.popup.components.ButtonProps;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.components.turn.*;
import fi.monopoly.types.DiceState;
import fi.monopoly.types.PathMode;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.TurnResult;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;
import processing.core.PConstants;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import static fi.monopoly.text.UiTexts.text;
import static processing.event.MouseEvent.CLICK;

@Slf4j
public class Game implements MonopolyEventListener {
    private static final boolean FORCE_DEBT_DEBUG_SCENARIO = false;
    private static Game current;
    public static Dices DICES;
    public static Players players;
    public static Animations animations;
    public static int GO_MONEY_AMOUNT = 200;
    private final MonopolyRuntime runtime;
    private final TurnEngine turnEngine = new TurnEngine();
    private final PaymentResolver paymentResolver = new PaymentResolver();
    private final MonopolyButton endRoundButton;
    private final MonopolyButton retryDebtButton;
    private final MonopolyButton declareBankruptcyButton;
    private final MonopolyButton debugAddCashButton;
    private final MonopolyButton debugDebtScenarioButton;
    private final MonopolyButton debugSendToJailButton;
    private final MonopolyButton debugResetTurnButton;
    private final MonopolyButton debugGodModeButton;
    Board board;
    TurnResult prevTurnResult;
    private DebtState debtState;

    public Game(MonopolyRuntime runtime) {
        current = this;
        this.runtime = runtime;
        this.endRoundButton = new MonopolyButton(runtime, "endRound");
        endRoundButton.setPosition((int) (Spot.SPOT_W * 5.4), runtime.app().height - Spot.SPOT_W * 3);
        endRoundButton.setLabel(text("game.button.endRound"));
        endRoundButton.setSize(100, 50);
        this.retryDebtButton = new MonopolyButton(runtime, "retryDebt");
        retryDebtButton.setPosition((int) (Spot.SPOT_W * 12 + 20), 560);
        retryDebtButton.setLabel(text("game.button.retryDebt"));
        retryDebtButton.setSize(140, 40);
        this.declareBankruptcyButton = new MonopolyButton(runtime, "declareBankruptcy");
        declareBankruptcyButton.setPosition((int) (Spot.SPOT_W * 12 + 180), 560);
        declareBankruptcyButton.setLabel(text("game.button.bankrupt"));
        declareBankruptcyButton.setSize(140, 40);
        this.debugAddCashButton = new MonopolyButton(runtime, "debugAddCash");
        debugAddCashButton.setPosition((int) (Spot.SPOT_W * 12 + 20), 640);
        debugAddCashButton.setLabel(text("game.button.debugAddCash"));
        debugAddCashButton.setSize(140, 36);
        this.debugDebtScenarioButton = new MonopolyButton(runtime, "debugDebtScenario");
        debugDebtScenarioButton.setPosition((int) (Spot.SPOT_W * 12 + 180), 640);
        debugDebtScenarioButton.setLabel(text("game.button.debugDebt"));
        debugDebtScenarioButton.setSize(140, 36);
        this.debugSendToJailButton = new MonopolyButton(runtime, "debugSendToJail");
        debugSendToJailButton.setPosition((int) (Spot.SPOT_W * 12 + 20), 684);
        debugSendToJailButton.setLabel(text("game.button.debugJail"));
        debugSendToJailButton.setSize(140, 36);
        this.debugResetTurnButton = new MonopolyButton(runtime, "debugResetTurn");
        debugResetTurnButton.setPosition((int) (Spot.SPOT_W * 12 + 180), 684);
        debugResetTurnButton.setLabel(text("game.button.debugReset"));
        debugResetTurnButton.setSize(140, 36);
        this.debugGodModeButton = new MonopolyButton(runtime, "debugGodMode");
        debugGodModeButton.setPosition((int) (Spot.SPOT_W * 12 + 20), 728);
        debugGodModeButton.setLabel(text("game.button.godMode"));
        debugGodModeButton.setSize(300, 36);
        endRoundButton.hide();
        retryDebtButton.hide();
        declareBankruptcyButton.hide();
        debugAddCashButton.hide();
        debugDebtScenarioButton.hide();
        debugSendToJailButton.hide();
        debugResetTurnButton.hide();
        debugGodModeButton.hide();
        runtime.eventBus().addListener(this);
        board = new Board(runtime);
        DICES = Dices.setRollDice(runtime, this::rollDice);
        players = new Players(runtime);
        animations = new Animations();

        Spot spot = board.getSpots().get(0);
        players.addPlayer(new Player(runtime, "Eka", Color.MEDIUMPURPLE, spot));
        players.addPlayer(new Player(runtime, "Toka", Color.PINK, spot));
        players.addPlayer(new Player(runtime, "Kolmas", Color.DARKOLIVEGREEN, spot));
//        players.addPlayer(new Player("Neljäs", Color.TURQUOISE, spot));
//        players.addPlayer(new Player("Viides", Color.MEDIUMBLUE, spot));
//        players.addPlayer(new Player("Kuudes", Color.MEDIUMSPRINGGREEN, spot));

        players.getTurn().buyProperty(PropertyFactory.getProperty(SpotType.B1));
        players.getTurn().buyProperty(PropertyFactory.getProperty(SpotType.B2));
        if (!FORCE_DEBT_DEBUG_SCENARIO) {
            players.giveRandomDeeds(board);
        }

        endRoundButton.addListener(e -> {
            if (!runtime.popupService().isAnyVisible() && debtState == null) {
                endRound(true);
            }
        });
        retryDebtButton.addListener(this::retryPendingDebtPayment);
        declareBankruptcyButton.addListener(this::declareBankruptcy);
        debugAddCashButton.addListener(this::debugAddCash);
        debugDebtScenarioButton.addListener(() -> debugStartDebtScenario(200));
        debugSendToJailButton.addListener(this::debugSendCurrentPlayerToJail);
        debugResetTurnButton.addListener(this::debugResetTurnState);
        debugGodModeButton.addListener(this::openDebugGodModeMenu);

        if (FORCE_DEBT_DEBUG_SCENARIO) {
            initializeDebtDebugScenario();
        }
    }

    public static boolean isDebtResolutionActive() {
        return current != null && current.debtState != null;
    }

    public static boolean isDebtResolutionForCurrentTurn() {
        return isDebtResolutionActive();
    }

    public void draw() {
        if (MonopolyApp.SKIP_ANNIMATIONS) {
            animations.finishAllAnimations();
        }
        if (!runtime.popupService().isAnyVisible()) {
            animations.updateAnimations();
        }
        board.draw(null);
        DICES.draw(null);
        players.draw();
        updateDebugButtons();
        drawDebtState();
    }

    private Spot getNewSpot(DiceValue diceValue) {
        Player turn = players.getTurn();
        Spot oldSpot = turn.getSpot();
        return board.getNewSpot(oldSpot, diceValue.value(), PathMode.NORMAL);
    }

    private void rollDice() {
        if (runtime.popupService().isAnyVisible() || debtState != null) {
            log.trace("Ignoring rollDice because popupVisible={} debtStateActive={}",
                    runtime.popupService().isAnyVisible(), debtState != null);
            return;
        }
        log.debug("Starting turn roll for player {}", players.getTurn().getName());
        playRound(DICES.getValue());
    }

    private void endRound(boolean switchTurns) {
        Player currentTurn = players.getTurn();
        prevTurnResult = null;
        if (switchTurns) {
            players.switchTurn();
        }
        DICES.reset();
        endRoundButton.hide();
        log.debug("Ending round. previousTurnPlayer={}, switchTurns={}, nextTurnPlayer={}",
                currentTurn != null ? currentTurn.getName() : "none",
                switchTurns,
                players.getTurn() != null ? players.getTurn().getName() : "none");
    }

    private void playRound(DiceValue diceValue) {
        Player turnPlayer = players.getTurn();
        log.debug("Playing round for player {} with diceState={} value={}",
                turnPlayer.getName(), diceValue.diceState(), diceValue.value());
        if (turnPlayer.isInJail()) {
            ((JailSpot) turnPlayer.getSpot()).handleInJailTurn(
                    turnPlayer,
                    diceValue,
                    this::handlePaymentRequest,
                    () -> applyTurnPlan(turnEngine.endTurn(false)),
                    () -> applyTurnPlan(turnEngine.endTurn(true))
            );
            return;
        }
        if (DiceState.JAIL.equals(diceValue.diceState())) {
            prevTurnResult = TurnResult.builder().shouldGoToJail(true).build();
        }
        applyTurnPlan(turnEngine.createMovementPlan(turnPlayer, board, diceValue));
    }

    private void addAnimationAndHandleSpot(Path path, CallbackAction onAnimationEnd) {
        PlayerToken turnPlayer = players.getTurn();
        animations.addAnimation(new Animation(turnPlayer, path, onAnimationEnd));
    }

    private boolean playRound(Spot newSpot, DiceState diceState) {
        Player turnPlayer = players.getTurn();
        if (newSpot.equals(turnPlayer.getSpot())) {
            runtime.popupService().show(text("game.move.sameSpot"));
            return false;
        }
        if (turnPlayer.isInJail()) {
            runtime.popupService().show(text("game.move.inJail"));
            return false;
        }
        PathMode pathMode = DiceState.DEBUG_REROLL.equals(diceState) || DiceState.JAIL.equals(diceState) ? PathMode.FLY : PathMode.NORMAL;
        Path path = board.getPath(turnPlayer, newSpot, pathMode);
        return playRound(path, diceState);
    }

    private boolean playRound(Path path, DiceState diceState) {
        Player turnPlayer = players.getTurn();
        log.trace("Animating player {} along path to {} with diceState={}",
                turnPlayer.getName(), path.getLastSpot().getSpotType(), diceState);
        addAnimationAndHandleSpot(path, () -> {
            CallbackAction completeTurnMove = () -> {
                turnPlayer.setSpot(path.getLastSpot());
                log.trace("Player {} arrived at {}", turnPlayer.getName(), path.getLastSpot().getSpotType());
                handleSpotLogic(diceState, path.getLastSpot());
            };
            if (path.passesGoSpot()) {
                log.info("Player {} passed GO", turnPlayer.getName());
                runtime.popupService().show(text("game.go.reward", GO_MONEY_AMOUNT), () -> {
                    turnPlayer.addMoney(GO_MONEY_AMOUNT);
                    completeTurnMove.doAction();
                });
                return;
            }
            completeTurnMove.doAction();
        });
        return true;
    }

    private void handleSpotLogic(DiceState diceState, Spot spot) {
        log.debug("Handling spot logic for player {} on spot {} with diceState={}",
                players.getTurn().getName(), spot.getSpotType(), diceState);
        GameState gameState = new GameState(players, DICES, board, TurnResult.copyOf(prevTurnResult), this::handlePaymentRequest);
        prevTurnResult = null; //Important to clear previous turn result before getting next one!
        prevTurnResult = spot.handleTurn(gameState, () -> doTurnEndEvent(diceState));
        log.trace("Spot handling produced prevTurnResult={}", prevTurnResult);
    }

    private void doTurnEndEvent(DiceState diceState) {
        log.trace("Running turn end event for player {} with diceState={} prevTurnResult={}",
                players.getTurn().getName(), diceState, prevTurnResult);
        applyTurnPlan(turnEngine.createFollowUpPlan(players.getTurn(), board, prevTurnResult, diceState));
    }

    private void applyTurnPlan(TurnPlan turnPlan) {
        log.debug("Applying turn plan phase={} effectCount={}", turnPlan.phase(), turnPlan.effects().size());
        for (TurnEffect effect : turnPlan.effects()) {
            log.trace("Applying turn effect {}", effect.getClass().getSimpleName());
            if (effect instanceof MovePlayerEffect movePlayerEffect) {
                playRound(movePlayerEffect.path(), movePlayerEffect.diceState());
            } else if (effect instanceof ShowDiceEffect) {
                DICES.show();
            } else if (effect instanceof ShowEndTurnEffect) {
                endRoundButton.show();
            } else if (effect instanceof EndTurnEffect endTurnEffect) {
                endRound(endTurnEffect.switchTurns());
            } else {
                throw new IllegalStateException("Unhandled turn effect: " + effect.getClass().getSimpleName());
            }
        }
    }

    public boolean onEvent(Event event) {
        boolean consumedEvent = false;
        if (event instanceof KeyEvent keyEvent) {
            if (runtime.popupService().isAnyVisible()) {
                return consumedEvent;
            }
            if (debtState != null) {
                if (keyEvent.getKey() == MonopolyApp.SPACE || keyEvent.getKey() == MonopolyApp.ENTER) {
                    retryPendingDebtPayment();
                    return true;
                }
                return false;
            }
            if (endRoundButton.isVisible() && (keyEvent.getKey() == MonopolyApp.SPACE || keyEvent.getKey() == MonopolyApp.ENTER)) {
                endRound(true);
                consumedEvent = true;
            }
            if (MonopolyApp.DEBUG_MODE && keyEvent.getKey() == 'e') {
                log.debug("Ending round");
                animations.finishAllAnimations();
                endRound(true);
                consumedEvent = true;
            }
            if (MonopolyApp.DEBUG_MODE && keyEvent.getKey() == 'g') {
                openDebugGodModeMenu();
                consumedEvent = true;
            }
            if (keyEvent.getKey() == 'a') {
                MonopolyApp.SKIP_ANNIMATIONS = !MonopolyApp.SKIP_ANNIMATIONS;
                log.debug("Skip animations: {}", MonopolyApp.SKIP_ANNIMATIONS);
                consumedEvent = true;
            }
        } else if (event instanceof MouseEvent mouseEvent) {
            if (mouseEvent.getAction() == CLICK) {
                if (runtime.popupService().isAnyVisible()) {
                    return consumedEvent;
                }
                Spot hoveredSpot = board.getHoveredSpot();
                //Debugging "flying mechanic"
                if (hoveredSpot != null && MonopolyApp.DEBUG_MODE && DICES.isVisible()) {
                    DICES.setValue(new DiceValue(DiceState.DEBUG_REROLL, 8));
                    if (playRound(hoveredSpot, DiceState.DEBUG_REROLL)) {
                        consumedEvent = true;
                        DICES.hide();
                    }
                }
            }
        }
        return consumedEvent;
    }

    private void handlePaymentRequest(PaymentRequest request, CallbackAction onResolved) {
        log.debug("Handling payment request in Game: debtor={}, target={}, amount={}, reason={}",
                request.debtor().getName(), request.target().getDisplayName(), request.amount(), request.reason());
        PaymentResult result = paymentResolver.tryPay(request);
        if (result.status() == PaymentStatus.PAID) {
            log.trace("Payment completed immediately in Game");
            onResolved.doAction();
            return;
        }

        debtState = new DebtState(request, onResolved, result.status() == PaymentStatus.BANKRUPT);
        DICES.hide();
        endRoundButton.hide();
        updateDebtButtons();
        log.info("Entering debt resolution: debtor={}, target={}, amount={}, bankruptcyRisk={}",
                request.debtor().getName(), request.target().getDisplayName(), request.amount(), debtState.bankruptcyRisk());
        runtime.popupService().show(buildDebtMessage(result));
    }

    private void retryPendingDebtPayment() {
        if (debtState == null) {
            return;
        }

        log.debug("Retrying pending debt payment for debtor={} amount={}",
                debtState.paymentRequest().debtor().getName(), debtState.paymentRequest().amount());
        autoRaiseFundsIfPossible(debtState.paymentRequest());
        PaymentResult result = paymentResolver.tryPay(debtState.paymentRequest());
        if (result.status() == PaymentStatus.PAID) {
            DebtState resolvedDebt = debtState;
            debtState = null;
            updateDebtButtons();
            log.info("Debt resolved for {}", resolvedDebt.paymentRequest().debtor().getName());
            runtime.popupService().show(text("game.debt.paid", resolvedDebt.paymentRequest().reason()), resolvedDebt.onResolved()::doAction);
            return;
        }

        debtState = new DebtState(debtState.paymentRequest(), debtState.onResolved(), result.status() == PaymentStatus.BANKRUPT);
        updateDebtButtons();
        log.info("Debt still unresolved for {}. bankruptcyRisk={}",
                debtState.paymentRequest().debtor().getName(), debtState.bankruptcyRisk());
        runtime.popupService().show(buildDebtMessage(result));
    }

    // Auto-mortgage removed: player must choose which properties to mortgage manually

    private String buildDebtMessage(PaymentResult result) {
        if (debtState == null) {
            return text("game.debt.couldNotComplete");
        }

        PaymentRequest request = debtState.paymentRequest();
        StringBuilder text = new StringBuilder(text(
                "game.debt.message",
                request.debtor().getName(),
                request.amount(),
                request.target().getDisplayName(),
                request.reason(),
                request.debtor().getMoneyAmount(),
                result.missingAmount()
        ));
        if (result.status() == PaymentStatus.BANKRUPT) {
            text.append(text("game.debt.message.bankruptcyLine"));
        }
        return text.toString();
    }

    private void drawDebtState() {
        if (debtState == null) {
            return;
        }

        MonopolyApp app = runtime.app();
        app.push();
        app.fill(0);
        app.textFont(runtime.font20());
        app.textAlign(PConstants.LEFT);
        PaymentRequest request = debtState.paymentRequest();
        app.text(
                text(
                        "game.debt.sidebar",
                        request.debtor().getName(),
                        request.amount(),
                        request.target().getDisplayName(),
                        request.debtor().getMoneyAmount(),
                        request.debtor().getTotalLiquidationValue()
                ),
                Spot.SPOT_W * 12 + 20,
                420
        );
        app.pop();
    }

    /**
     * This method is called when the player needs to raise funds to pay a debt.
     * Auto-mortgage is disabled, so the player must manually choose which properties to mortgage or sell.
     * Shows a popup to instruct the player.
     */
    private void autoRaiseFundsIfPossible(PaymentRequest request) {
        log.info("Auto-mortgage is disabled. Player must choose properties to mortgage manually.");
        runtime.popupService().show(text("game.debt.manual"));
    }

    private void declareBankruptcy() {
        if (debtState == null) {
            return;
        }
        if (!debtState.bankruptcyRisk()) {
            runtime.popupService().show(text("game.debt.assetsCover"));
            return;
        }
        PaymentRequest request = debtState.paymentRequest();
        log.warn("Declaring bankruptcy: debtor={}, target={}, amount={}",
                request.debtor().getName(), request.target().getDisplayName(), request.amount());
        if (request.target() instanceof PlayerTarget playerTarget) {
            request.debtor().transferAssetsTo(playerTarget.player());
        } else {
            request.debtor().releaseAssetsToBank();
        }
        request.debtor().setGetOutOfJailCardCount(0);
        players.removePlayer(request.debtor());
        debtState = null;
        updateDebtButtons();

        if (players.count() <= 1) {
            Player winner = players.getTurn();
            String winnerName = winner != null ? winner.getName() : "No one";
            log.info("Game over after bankruptcy. winner={}", winnerName);
            runtime.popupService().show(text("game.bankruptcy.gameOver", winnerName));
            DICES.hide();
            endRoundButton.hide();
            return;
        }

        players.switchTurn();
        DICES.reset();
        log.info("Bankruptcy handled. Next turn player={}", players.getTurn() != null ? players.getTurn().getName() : "none");
        runtime.popupService().show(text("game.bankruptcy.playerWent", request.debtor().getName()));
    }

    private void updateDebtButtons() {
        if (debtState == null) {
            retryDebtButton.hide();
            declareBankruptcyButton.hide();
            return;
        }
        retryDebtButton.show();
        if (debtState.bankruptcyRisk()) {
            declareBankruptcyButton.show();
        } else {
            declareBankruptcyButton.hide();
        }
    }

    private void initializeDebtDebugScenario() {
        Player turnPlayer = players.getTurn();
        turnPlayer.buyProperty(PropertyFactory.getProperty(SpotType.RR1));
        turnPlayer.addMoney(-(turnPlayer.getMoneyAmount() - 40));
        handlePaymentRequest(
                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, 200, "Debug debt scenario: pay M200 tax"),
                this::restoreNormalTurnControls
        );
    }

    private void updateDebugButtons() {
        if (!MonopolyApp.DEBUG_MODE) {
            debugAddCashButton.hide();
            debugDebtScenarioButton.hide();
            debugSendToJailButton.hide();
            debugResetTurnButton.hide();
            debugGodModeButton.hide();
            return;
        }
        debugAddCashButton.show();
        debugDebtScenarioButton.show();
        debugSendToJailButton.show();
        debugResetTurnButton.show();
        debugGodModeButton.show();
    }

    private void debugAddCash() {
        Player turnPlayer = players.getTurn();
        if (turnPlayer != null) {
            log.debug("Debug action: add cash to {}", turnPlayer.getName());
            turnPlayer.addMoney(500);
            runtime.popupService().show(text("game.debug.receivedCash", turnPlayer.getName(), 500));
        }
    }

    private void debugStartDebtScenario() {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        log.debug("Debug action: start debt scenario for {}", turnPlayer.getName());
        turnPlayer.addMoney(-(turnPlayer.getMoneyAmount() - 40));
        handlePaymentRequest(
                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, 200, "Debug debt scenario: pay M200 tax"),
                this::restoreNormalTurnControls
        );
    }

    private void debugSendCurrentPlayerToJail() {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        log.debug("Debug action: send player {} to jail", turnPlayer.getName());
        Spot jailSpot = board.getPathWithCriteria(SpotType.JAIL);
        JailSpot.jailTimeLeftMap.put(turnPlayer, JailSpot.JAIL_ROUND_NUMBER);
        turnPlayer.setSpot(jailSpot);
        turnPlayer.setCoords(jailSpot.getTokenCoords(turnPlayer));
        runtime.popupService().show(text("game.debug.sentToJail", turnPlayer.getName()));
    }

    private void debugResetTurnState() {
        log.debug("Debug action: reset turn state");
        animations.finishAllAnimations();
        debtState = null;
        updateDebtButtons();
        runtime.popupService().hideAll();
        DICES.reset();
        endRoundButton.hide();
        runtime.popupService().show(text("game.debug.reset"));
    }

    private void openDebugGodModeMenu() {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        runtime.popupService().show(
                text("game.debug.godMode.title", turnPlayer.getName()),
                new ButtonProps(text("game.debug.button.money"), this::openDebugMoneyMenu),
                new ButtonProps(text("game.debug.button.move"), this::openDebugMoveMenu),
                new ButtonProps(text("game.debug.button.debt"), this::openDebugDebtMenu),
                new ButtonProps(text("game.debug.button.jail"), this::openDebugJailMenu),
                new ButtonProps(text("game.debug.button.scenarios"), this::openDebugScenarioMenu)
        );
    }

    private void openDebugMoneyMenu() {
        runtime.popupService().show(
                text("game.debug.money.title"),
                new ButtonProps(text("game.debug.money.add500"), () -> debugAdjustCurrentPlayerMoney(500)),
                new ButtonProps(text("game.debug.money.minus100"), () -> debugAdjustCurrentPlayerMoney(-100)),
                new ButtonProps(text("game.debug.money.set0"), () -> debugSetCurrentPlayerMoney(0)),
                new ButtonProps(text("game.debug.money.set50"), () -> debugSetCurrentPlayerMoney(50)),
                new ButtonProps(text("game.debug.money.set1500"), () -> debugSetCurrentPlayerMoney(1500))
        );
    }

    private void openDebugMoveMenu() {
        ButtonProps[] spotButtons = SpotType.SPOT_TYPES.stream()
                .map(spotType -> new ButtonProps(spotType.name(), () -> debugMoveCurrentPlayerTo(spotType)))
                .toArray(ButtonProps[]::new);
        runtime.popupService().show(text("game.debug.move.title"), spotButtons);
    }

    private void openDebugDebtMenu() {
        runtime.popupService().show(
                text("game.debug.debt.title"),
                new ButtonProps(text("game.debug.debt.50"), () -> debugStartDebtScenario(50)),
                new ButtonProps(text("game.debug.debt.100"), () -> debugStartDebtScenario(100)),
                new ButtonProps(text("game.debug.debt.200"), () -> debugStartDebtScenario(200)),
                new ButtonProps(text("game.debug.debt.500"), () -> debugStartDebtScenario(500)),
                new ButtonProps(text("game.button.retryDebt"), this::retryPendingDebtPayment)
        );
    }

    private void openDebugJailMenu() {
        runtime.popupService().show(
                text("game.debug.jail.title"),
                new ButtonProps(text("game.debug.jail.send"), this::debugSendCurrentPlayerToJail),
                new ButtonProps(text("game.debug.jail.oneRound"), () -> debugSetCurrentPlayerJailRounds(1)),
                new ButtonProps(text("game.debug.jail.threeRounds"), () -> debugSetCurrentPlayerJailRounds(JailSpot.JAIL_ROUND_NUMBER)),
                new ButtonProps(text("game.debug.jail.release"), this::debugReleaseCurrentPlayerFromJail)
        );
    }

    private void openDebugScenarioMenu() {
        runtime.popupService().show(
                text("game.debug.scenario.title"),
                new ButtonProps(text("game.debug.scenario.brownMonopoly"), this::debugGiveBrownMonopoly),
                new ButtonProps(text("game.debug.scenario.brownDebt"), this::debugSetupBrownDebtScenario),
                new ButtonProps(text("game.debug.scenario.railDebt"), this::debugSetupRailroadDebtScenario),
                new ButtonProps(text("game.debug.scenario.resetUi"), this::debugResetTurnState)
        );
    }

    private void debugAdjustCurrentPlayerMoney(int delta) {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        int targetMoney = Math.max(0, turnPlayer.getMoneyAmount() + delta);
        debugSetCurrentPlayerMoney(targetMoney);
    }

    private void debugSetCurrentPlayerMoney(int targetMoney) {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        turnPlayer.addMoney(targetMoney - turnPlayer.getMoneyAmount());
        runtime.popupService().show(text("game.debug.money.nowHas", turnPlayer.getName(), turnPlayer.getMoneyAmount()));
    }

    private void debugMoveCurrentPlayerTo(SpotType spotType) {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        Spot targetSpot = board.getPathWithCriteria(spotType);
        if (turnPlayer.isInJail()) {
            JailSpot.jailTimeLeftMap.remove(turnPlayer);
        }
        turnPlayer.setSpot(targetSpot);
        turnPlayer.setCoords(targetSpot.getTokenCoords(turnPlayer));
        runtime.popupService().show(text("game.debug.move.moved", turnPlayer.getName(), spotType.name()));
    }

    private void debugStartDebtScenario(int amount) {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        log.debug("Debug action: start custom debt scenario for {} amount={}", turnPlayer.getName(), amount);
        handlePaymentRequest(
                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, amount, "Debug debt scenario: pay M" + amount),
                this::restoreNormalTurnControls
        );
    }

    private void debugSetCurrentPlayerJailRounds(int roundsLeft) {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        Spot jailSpot = board.getPathWithCriteria(SpotType.JAIL);
        JailSpot.jailTimeLeftMap.put(turnPlayer, roundsLeft);
        turnPlayer.setSpot(jailSpot);
        turnPlayer.setCoords(jailSpot.getTokenCoords(turnPlayer));
        runtime.popupService().show(text("game.debug.jail.rounds", turnPlayer.getName(), roundsLeft));
    }

    private void debugReleaseCurrentPlayerFromJail() {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        JailSpot.jailTimeLeftMap.remove(turnPlayer);
        runtime.popupService().show(text("game.debug.jail.released", turnPlayer.getName()));
    }

    private void debugGiveBrownMonopoly() {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        debugAssignProperty(turnPlayer, SpotType.B1);
        debugAssignProperty(turnPlayer, SpotType.B2);
        runtime.popupService().show(text("game.debug.scenario.brownOwned", turnPlayer.getName()));
    }

    private void debugSetupBrownDebtScenario() {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        debugGiveBrownMonopoly();
        StreetProperty b1 = (StreetProperty) PropertyFactory.getProperty(SpotType.B1);
        StreetProperty b2 = (StreetProperty) PropertyFactory.getProperty(SpotType.B2);
        debugSetCurrentPlayerMoney(1500);
        b1.buyHouses(2);
        b2.buyHouses(2);
        debugSetCurrentPlayerMoney(50);
        handlePaymentRequest(
                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, 300, "Debug brown debt scenario"),
                this::restoreNormalTurnControls
        );
    }

    private void debugSetupRailroadDebtScenario() {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        debugAssignProperty(turnPlayer, SpotType.RR1);
        debugSetCurrentPlayerMoney(40);
        handlePaymentRequest(
                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, 200, "Debug railroad debt scenario"),
                this::restoreNormalTurnControls
        );
    }

    private void debugAssignProperty(Player player, SpotType spotType) {
        Property property = PropertyFactory.getProperty(spotType);
        Player previousOwner = property.getOwnerPlayer();
        if (previousOwner != null && previousOwner != player) {
            previousOwner.removeOwnedProperty(property);
        }
        property.setMortgaged(false);
        player.addOwnedProperty(property);
    }

    private void restoreNormalTurnControls() {
        log.trace("Restoring normal turn controls");
        debtState = null;
        updateDebtButtons();
        DICES.reset();
        endRoundButton.hide();
    }
}
