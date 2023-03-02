package org.example.components;

import controlP5.Button;
import javafx.scene.paint.Color;
import org.example.MonopolyApp;
import org.example.components.animation.Animation;
import org.example.components.animation.Animations;
import org.example.components.board.Board;
import org.example.components.board.Path;
import org.example.components.popup.OkPopup;
import org.example.components.popup.Popup;
import org.example.components.spots.JailSpot;
import org.example.images.DeedImage;
import org.example.types.*;
import org.example.components.dices.Dices;
import org.example.components.event.MonopolyEventListener;
import org.example.components.spots.Spot;
import org.example.components.dices.DiceValue;
import org.example.utils.GameTurnUtils;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import static org.example.MonopolyApp.ENTER;
import static org.example.MonopolyApp.SPACE;
import static processing.event.MouseEvent.CLICK;

public class Game implements MonopolyEventListener {
    Board board;
    Dices dices;
    Players players;
    Animations animations;
    TurnResult prevTurnResult;
    boolean skipAnimations = false;
    private static final Button endRoundButton = new Button(MonopolyApp.p5, "endRound")
            .setPosition((int) (Spot.SPOT_W * 5.4), MonopolyApp.self.height - Spot.SPOT_W * 3)
            .setLabel("End round")
            .setFont(MonopolyApp.font20)
            .setSize(100, 50)
            .hide();

    public Game() {
        MonopolyApp.addListener(this);
        board = new Board();
        dices = Dices.onRollDice(this::rollDice);
        players = new Players();
        animations = new Animations();

        Spot spot = board.getSpots().get(0);
        players.addPlayer(new Player("Eka", Color.MEDIUMPURPLE, spot));
        players.addPlayer(new Player("Toka", Color.PINK, spot));
        players.addPlayer(new Player("Kolmas", Color.DARKOLIVEGREEN, spot));
//        players.addPlayer(new Player("Neljäs", Color.TURQUOISE, spot));
//        players.addPlayer(new Player("Viides", Color.MEDIUMBLUE, spot));
//        players.addPlayer(new Player("Kuudes", Color.MEDIUMSPRINGGREEN, spot));

        players.giveRandomDeeds(board);

        endRoundButton.addListener(e -> endRound());
    }

    public void draw() {
        if (skipAnimations) {
            animations.finishAllAnimations();
        }
        animations.updateAnimations();
        board.draw(null);
        dices.draw(null);
        players.draw();
    }

    private void rollDice() {
        if (Popup.isAnyVisible()) {
            return;
        }
        playRound(dices.getValue());
    }

    private void endRound() {
        prevTurnResult = null;
        players.switchTurn();
        dices.reset();
        endRoundButton.hide();
    }

    private Spot getNewSpot(DiceValue diceValue) {
        Player turn = players.getTurn();
        Spot oldSpot = turn.getSpot();
        return board.getNewSpot(oldSpot, diceValue.value(), PathMode.NORMAL);
    }

    private void playRound(DiceValue diceValue) {
        Player turn = players.getTurn();
        if (turn.isInJail()) {
            CallbackAction onGetOutOfJail = () -> {
                //Almost like End round, but don't switch player.
                prevTurnResult = null;
                dices.reset();
                endRoundButton.hide();
            };
            if (diceValue.diceState().equals(DiceState.DOUBLES)) {
                JailSpot.releaseFromJail(turn, () -> playRound(diceValue));
                turn.setCoords(turn.getSpot().getTokenCoords(turn));
            } else {
                JailSpot.tryToGetOufOfJail(turn, diceValue, onGetOutOfJail, this::endRound);
                turn.setCoords(turn.getSpot().getTokenCoords(turn));
            }
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
        if (newSpot.equals(players.getTurn().getSpot())) {
            OkPopup.showInfo("Can't move to same spot that player is in");
            return false;
        }
        if (players.getTurn().isInJail()) {
            OkPopup.showInfo("Can't move out when player is in jail");
            return false;
        }
        Player turnPlayer = players.getTurn();
        PathMode pathMode = DiceState.DEBUG_REROLL.equals(diceState) || DiceState.JAIL.equals(diceState) ? PathMode.FLY : PathMode.NORMAL;
        Path path = board.getPath(turnPlayer, newSpot, pathMode);
        return playRound(path, diceState);
    }

    private boolean playRound(Path path, DiceState diceState) {
        Player turnPlayer = players.getTurn();
        addAnimation(path, () -> {
            turnPlayer.setSpot(path.getLastSpot());
            handleTurn(diceState, path);
        });
        return true;
    }

    private void addAnimation(Path path, CallbackAction onAnimationEnd) {
        PlayerToken turnPlayer = players.getTurn();
        animations.addAnimation(new Animation(turnPlayer, path, onAnimationEnd));
    }

    private void handleTurn(DiceState diceState, Path path) {
        GameState gameState = new GameState(players, dices, board, path, TurnResult.copyOf(prevTurnResult));
        prevTurnResult = null; //Important to clear previous turn result before getting next one!
        prevTurnResult = GameTurnUtils.handleTurn(gameState, () -> doTurnEndEvent(diceState));
    }

    private void doTurnEndEvent(DiceState diceState) {
        //TODO any better way to check if player went to jail?
//        Integer jailRoundCount = JailSpot.playersRoundsLeftMap.get(players.getTurn());
//        boolean wentToJail = jailRoundCount != null && jailRoundCount == JailSpot.JAIL_ROUND_NUMBER;
        if (prevTurnResult != null) {
            Path path = board.getPathWithCriteria(prevTurnResult, players.getTurn());
            playRound(path, diceState);
        } else if (DiceState.DOUBLES.equals(diceState)) {
            dices.show();
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
            if (endRoundButton.isVisible() && (keyEvent.getKey() == SPACE || keyEvent.getKey() == ENTER)) {
                endRound();
                consumedEvent = true;
            }
            if (MonopolyApp.DEBUG_MODE && keyEvent.getKey() == 'e') {
                animations.finishAllAnimations();
                endRound();
                consumedEvent = true;
            }
            if (keyEvent.getKey() == 'a') {
                skipAnimations = !skipAnimations;
                consumedEvent = true;
            }
        } else if (event instanceof MouseEvent mouseEvent) {
            if (mouseEvent.getAction() == CLICK) {
                if (Popup.isAnyVisible()) {
                    return consumedEvent;
                }
                Drawable hoveredImage = getHoveredImage();
                if (hoveredImage instanceof Spot hoveredSpot && MonopolyApp.DEBUG_MODE && dices.isVisible()) {
                    dices.setValue(new DiceValue(DiceState.DEBUG_REROLL, 8));
                    if (playRound(hoveredSpot, DiceState.DEBUG_REROLL)) {
                        consumedEvent = true;
                        dices.hide();
                    }
                } else if (hoveredImage instanceof DeedImage deedImage) {
                    deedImage.click();
                }
            }
        }
        return consumedEvent;
    }

    private Drawable getHoveredImage() {
        Drawable result = players.getHoveredDeed();
        if (result == null) {
            result = board.getHoveredSpot();
        }
        return result;
    }
}
