package org.example.components;

import controlP5.Button;
import javafx.scene.paint.Color;
import org.example.MonopolyApp;
import org.example.components.animation.Animation;
import org.example.components.animation.Animations;
import org.example.types.DiceState;
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
        animations.updateAnimations();
        board.draw(null);
        dices.draw(null);
        players.draw();
    }

    private void rollDice() {
        playRound(dices.getValue());
    }

    private void endRound() {
        players.switchTurn();
        dices.reset();
        endRoundButton.hide();
    }

    private void playRound(DiceValue diceValue) {
        Spot newSpot = getNewSpot(diceValue);
        DiceState diceState = diceValue.diceState();
        if (DiceState.JAIL.equals(diceState)) {
            playRound(board.getJailSpot(), null);
        } else {
            playRound(newSpot, diceState);
        }
    }

    private Spot getNewSpot(DiceValue diceValue) {
        Player turn = players.getTurn();
        Spot oldSpot = turn.getSpot();
        return board.getNewSpot(oldSpot, diceValue.value());
    }

    private boolean playRound(Spot newSpot, DiceState diceState) {
        if (newSpot.equals(players.getTurn().getSpot())) {
            System.out.println("Trying to move to same spot that player is in");
            return false;
        }
        CallbackAction roundEndCallback = getRoundEndCallback(diceState);
        addAnimation(newSpot, diceState, roundEndCallback);
        Player turn = players.getTurn();
        turn.setSpot(newSpot);
        return true;
    }

    private void addAnimation(Spot newSpot, DiceState diceState, CallbackAction roundEndCallback) {
        Player turnPlayer = players.getTurn();
        animations.addAnimation(new Animation(turnPlayer, board.getPath(turnPlayer.getSpot(), newSpot, turnPlayer, diceState == null || MonopolyApp.DEBUG_MODE), roundEndCallback));
    }

    private CallbackAction getRoundEndCallback(DiceState diceState) {
        return () -> GameTurnUtils.handleTurn(players, dices, () -> doRoundEvent(diceState));
    }

    private void doRoundEvent(DiceState diceState) {
        if (DiceState.REROLL.equals(diceState)) {
            dices.show();
        } else {
            endRoundButton.show();
        }
    }

    public boolean onEvent(Event event) {
        if (event instanceof KeyEvent keyEvent) {
            if (!endRoundButton.isVisible()) {
                return false;
            }
            if (keyEvent.getKey() == SPACE || keyEvent.getKey() == ENTER) {
                endRound();
                return true;
            }
        } else if (event instanceof MouseEvent mouseEvent) {
            if (mouseEvent.getAction() == CLICK) {
                if (MonopolyApp.DEBUG_MODE && dices.isVisible()) {
                    Spot newSpot = board.getHoveredSpot();
                    if (newSpot != null) {
                        dices.setValue(new DiceValue(DiceState.REROLL, 8));
                        if (playRound(newSpot, DiceState.REROLL)) {
                            dices.hide();
                        }
                    }
                }
            }
        }
        return false;
    }
}
