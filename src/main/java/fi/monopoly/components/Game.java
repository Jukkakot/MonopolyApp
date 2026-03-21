package fi.monopoly.components;

import controlP5.Button;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.animation.Animation;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.board.Path;
import fi.monopoly.components.dices.DiceValue;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.components.turn.EndTurnEffect;
import fi.monopoly.components.turn.MovePlayerEffect;
import fi.monopoly.components.turn.ShowDiceEffect;
import fi.monopoly.components.turn.ShowEndTurnEffect;
import fi.monopoly.components.turn.TurnEffect;
import fi.monopoly.components.turn.TurnEngine;
import fi.monopoly.components.turn.TurnPlan;
import fi.monopoly.types.DiceState;
import fi.monopoly.types.PathMode;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.TurnResult;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import static processing.event.MouseEvent.CLICK;

@Slf4j
public class Game implements MonopolyEventListener {
    private final MonopolyRuntime runtime;
    public static Dices DICES;
    Board board;
    public static Players players;
    public static Animations animations;
    TurnResult prevTurnResult;
    private final TurnEngine turnEngine = new TurnEngine();
    private final Button endRoundButton;

    public static int GO_MONEY_AMOUNT = 200;

    public Game(MonopolyRuntime runtime) {
        this.runtime = runtime;
        this.endRoundButton = new MonopolyButton(runtime, "endRound")
                .setPosition((int) (Spot.SPOT_W * 5.4), runtime.app().height - Spot.SPOT_W * 3)
                .setLabel("End round")
                .setSize(100, 50)
                .hide();
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
        players.giveRandomDeeds(board);

        endRoundButton.addListener(e -> {
            if (!runtime.popupService().isAnyVisible()) {
                endRound(true);
            }
        });
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
    }

    private void rollDice() {
        if (runtime.popupService().isAnyVisible()) {
            return;
        }
        playRound(DICES.getValue());
    }

    private void endRound(boolean switchTurns) {
        prevTurnResult = null;
        if (switchTurns) {
            players.switchTurn();
        }
        DICES.reset();
        endRoundButton.hide();
    }

    private Spot getNewSpot(DiceValue diceValue) {
        Player turn = players.getTurn();
        Spot oldSpot = turn.getSpot();
        return board.getNewSpot(oldSpot, diceValue.value(), PathMode.NORMAL);
    }

    private void playRound(DiceValue diceValue) {
        Player turnPlayer = players.getTurn();
        if (turnPlayer.isInJail()) {
            ((JailSpot) turnPlayer.getSpot()).handleInJailTurn(turnPlayer, diceValue, () -> applyTurnPlan(turnEngine.endTurn(false)), () -> applyTurnPlan(turnEngine.endTurn(true)));
            return;
        }
        if (DiceState.JAIL.equals(diceValue.diceState())) {
            prevTurnResult = TurnResult.builder().shouldGoToJail(true).build();
        }
        applyTurnPlan(turnEngine.createMovementPlan(turnPlayer, board, diceValue));
    }

    private boolean playRound(Spot newSpot, DiceState diceState) {
        Player turnPlayer = players.getTurn();
        if (newSpot.equals(turnPlayer.getSpot())) {
            runtime.popupService().show("Can't move to same spot that player is in");
            return false;
        }
        if (turnPlayer.isInJail()) {
            runtime.popupService().show("Can't move out when player is in jail");
            return false;
        }
        PathMode pathMode = DiceState.DEBUG_REROLL.equals(diceState) || DiceState.JAIL.equals(diceState) ? PathMode.FLY : PathMode.NORMAL;
        Path path = board.getPath(turnPlayer, newSpot, pathMode);
        return playRound(path, diceState);
    }

    private boolean playRound(Path path, DiceState diceState) {
        Player turnPlayer = players.getTurn();
        addAnimationAndHandleSpot(path, () -> {
            CallbackAction completeTurnMove = () -> {
                turnPlayer.setSpot(path.getLastSpot());
                handleSpotLogic(diceState, path.getLastSpot());
            };
            if (path.passesGoSpot()) {
                runtime.popupService().show("Player gets M" + GO_MONEY_AMOUNT, () -> {
                    turnPlayer.addMoney(GO_MONEY_AMOUNT);
                    completeTurnMove.doAction();
                });
                return;
            }
            completeTurnMove.doAction();
        });
        return true;
    }

    private void addAnimationAndHandleSpot(Path path, CallbackAction onAnimationEnd) {
        PlayerToken turnPlayer = players.getTurn();
        animations.addAnimation(new Animation(turnPlayer, path, onAnimationEnd));
    }

    private void handleSpotLogic(DiceState diceState, Spot spot) {
        GameState gameState = new GameState(players, DICES, board, TurnResult.copyOf(prevTurnResult));
        prevTurnResult = null; //Important to clear previous turn result before getting next one!
        prevTurnResult = spot.handleTurn(gameState, () -> doTurnEndEvent(diceState));
    }

    private void doTurnEndEvent(DiceState diceState) {
        applyTurnPlan(turnEngine.createFollowUpPlan(players.getTurn(), board, prevTurnResult, diceState));
    }

    private void applyTurnPlan(TurnPlan turnPlan) {
        for (TurnEffect effect : turnPlan.effects()) {
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
}
