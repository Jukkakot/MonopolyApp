package fi.monopoly.components;

import controlP5.Button;
import fi.monopoly.MonopolyApp;
import fi.monopoly.components.animation.Animation;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.board.Path;
import fi.monopoly.components.dices.DiceValue;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.popup.Popup;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.components.spots.Spot;
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
    public static Dices DICES;
    Board board;
    public static Players players;
    public static Animations animations;
    TurnResult prevTurnResult;

    public static int GO_MONEY_AMOUNT = 200;
    private static final Button endRoundButton = new MonopolyButton("endRound")
            .setPosition((int) (Spot.SPOT_W * 5.4), MonopolyApp.self.height - Spot.SPOT_W * 3)
            .setLabel("End round")
            .setSize(100, 50)
            .hide();

    public Game() {
        MonopolyApp.addListener(this);
        board = new Board();
        DICES = Dices.setRollDice(this::rollDice);
        players = new Players();
        animations = new Animations();

        Spot spot = board.getSpots().get(0);
        players.addPlayer(new Player("Eka", Color.MEDIUMPURPLE, spot));
        players.addPlayer(new Player("Toka", Color.PINK, spot));
        players.addPlayer(new Player("Kolmas", Color.DARKOLIVEGREEN, spot));
//        players.addPlayer(new Player("Neljäs", Color.TURQUOISE, spot));
//        players.addPlayer(new Player("Viides", Color.MEDIUMBLUE, spot));
//        players.addPlayer(new Player("Kuudes", Color.MEDIUMSPRINGGREEN, spot));

        players.getTurn().buyProperty(PropertyFactory.getProperty(SpotType.B1));
        players.getTurn().buyProperty(PropertyFactory.getProperty(SpotType.B2));
        players.giveRandomDeeds(board);

        endRoundButton.addListener(e -> endRound(true));
    }

    public void draw() {
        if (MonopolyApp.SKIP_ANNIMATIONS) {
            animations.finishAllAnimations();
        }
        if (!Popup.isAnyVisible()) {
            animations.updateAnimations();
        }
        board.draw(null);
        DICES.draw(null);
        players.draw();
    }

    private void rollDice() {
        if (Popup.isAnyVisible()) {
            return;
        }
        playRound(DICES.getValue());
    }

    private void endRound(boolean switchTurns) {
        if (Popup.isAnyVisible()) {
            return;
        }
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
            ((JailSpot) turnPlayer.getSpot()).handleInJailTurn(turnPlayer, diceValue, () -> endRound(false), () -> endRound(true));
            return;
        }
        Spot newSpot = getNewSpot(diceValue);
        DiceState diceState = diceValue.diceState();
        if (DiceState.JAIL.equals(diceState)) {
            prevTurnResult = TurnResult.builder().shouldGoToJail(true).build();
            playRound(board.getPathWithCriteria(SpotType.JAIL), diceState);
        } else {
            playRound(newSpot, diceState);
        }
    }

    private boolean playRound(Spot newSpot, DiceState diceState) {
        Player turnPlayer = players.getTurn();
        if (newSpot.equals(turnPlayer.getSpot())) {
            Popup.show("Can't move to same spot that player is in");
            return false;
        }
        if (turnPlayer.isInJail()) {
            Popup.show("Can't move out when player is in jail");
            return false;
        }
        PathMode pathMode = DiceState.DEBUG_REROLL.equals(diceState) || DiceState.JAIL.equals(diceState) ? PathMode.FLY : PathMode.NORMAL;
        Path path = board.getPath(turnPlayer, newSpot, pathMode);
        return playRound(path, diceState);
    }

    private boolean playRound(Path path, DiceState diceState) {
        Player turnPlayer = players.getTurn();
        addAnimationAndHandleSpot(path, () -> {
            turnPlayer.setSpot(path.getLastSpot());
            handleSpotLogic(diceState, path.getLastSpot());
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
        //TODO any better way to check if player went to jail?
//        Integer jailRoundCount = JailSpot.playersRoundsLeftMap.get(players.getTurn());
//        boolean wentToJail = jailRoundCount != null && jailRoundCount == JailSpot.JAIL_ROUND_NUMBER;
        if (prevTurnResult != null) {
            //Go to jail, go to next railway station etc...
            Path path = board.getPathWithCriteria(prevTurnResult, players.getTurn());
            playRound(path, diceState);
        } else if (DiceState.DOUBLES.equals(diceState)) {
            DICES.show();
        } else {
            endRoundButton.show();
        }
    }

    public boolean onEvent(Event event) {
        boolean consumedEvent = false;
        if (event instanceof KeyEvent keyEvent) {
            if (Popup.isAnyVisible()) {
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
                if (Popup.isAnyVisible()) {
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
