package org.example.components;

import controlP5.Button;
import javafx.scene.paint.Color;
import org.example.MonopolyApp;
import org.example.components.animation.Animation;
import org.example.components.animation.Animations;
import org.example.components.board.Board;
import org.example.components.board.Path;
import org.example.components.popup.Popup;
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
        Spot newSpot = getNewSpot(diceValue);
        DiceState diceState = diceValue.diceState();
        if (DiceState.JAIL.equals(diceState)) {
            playRound(board.getPathWithCriteria(SpotType.JAIL), null);
        } else {
            playRound(newSpot, diceState);
        }
    }

    private boolean playRound(Spot newSpot, DiceState diceState) {
        if (newSpot.equals(players.getTurn().getSpot())) {
            System.out.println("Trying to move to same spot that player is in");
            return false;
        }
        Player turnPlayer = players.getTurn();
        PathMode pathMode = diceState == null || DiceState.DEBUG_REROLL.equals(diceState) ? PathMode.FLY : PathMode.NORMAL;
        Path path = board.getPath(turnPlayer.getSpot(), newSpot, pathMode);
        return playRound(path, diceState);
    }

    private boolean playRound(Path path, DiceState diceState) {
        prevTurnResult = null;
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
        GameState gameState = new GameState(players, dices, board, path, prevTurnResult);
        prevTurnResult = GameTurnUtils.handleTurn(gameState, () -> doTurnEndEvent(diceState));
    }

    private void doTurnEndEvent(DiceState diceState) {
        if (prevTurnResult != null) {
            Path path = board.getPathWithCriteria(prevTurnResult, players.getTurn().getSpot());
            playRound(path, diceState);
        } else if (DiceState.REROLL.equals(diceState) || DiceState.DEBUG_REROLL.equals(diceState)) {
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
                endRound();
                consumedEvent = true;
            }
            if (keyEvent.getKey() == 'a') {
                skipAnimations = !skipAnimations;
                consumedEvent = true;
            }
        } else if (event instanceof MouseEvent mouseEvent) {
            if (mouseEvent.getAction() == CLICK) {
                if (MonopolyApp.DEBUG_MODE && dices.isVisible()) {
                    Spot newSpot = board.getHoveredSpot();
                    if (newSpot != null) {
                        dices.setValue(new DiceValue(DiceState.DEBUG_REROLL, 8));
                        if (playRound(newSpot, DiceState.DEBUG_REROLL)) {
                            consumedEvent = true;
                            dices.hide();
                        }
                    }
                }
            }
        }
        return consumedEvent;
    }
}
