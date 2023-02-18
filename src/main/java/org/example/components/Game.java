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

import static org.example.MonopolyApp.ENTER;
import static org.example.MonopolyApp.SPACE;

public class Game implements MonopolyEventListener {
    Board board;
    Dices dices;
    Players players;
    Animations animations;
    private static final Button endRoundButton = new Button(MonopolyApp.p5, "endRound")
            .setPosition((int) (Spot.spotW * 5.4), MonopolyApp.self.height - Spot.spotW * 3)
            .setLabel("End round")
            .setFont(MonopolyApp.font20)
            .setSize(100, 50)
            .hide();
    float i = 0;

    public Game() {
        MonopolyApp.addListener(this);
        board = new Board();
        dices = Dices.onRollDice(this::rollDice);
        players = new Players();
        animations = new Animations();

        Spot spot = board.getSpots().get(0);
        players.addPlayer(new Player(1, "Eka", new Token(spot, Color.MEDIUMPURPLE), 1), spot);
        players.addPlayer(new Player(2, "Toka", new Token(spot, Color.PINK), 2), spot);
        players.addPlayer(new Player(3, "Kolmas", new Token(spot, Color.DARKOLIVEGREEN), 3), spot);
//        players.addPlayer(new Player(4, "Neljäs", new Token(app, spot, Color.TURQUOISE), 4), spot);
//        players.addPlayer(new Player(5, "Viides", new Token(app, spot, Color.MEDIUMPURPLE), 5), spot);
//        players.addPlayer(new Player(6, "Kuudes", new Token(app, spot, Color.PINK), 6), spot);

        players.giveRandomDeeds(board);

        endRoundButton.addListener(e -> endRound());
    }

    public void draw() {
        animations.updateAnimations();
        board.draw(null);
        dices.draw(null);
        players.draw();
        i += 0.5;
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

    private void playRound(Spot newSpot, DiceState diceState) {
        CallbackAction roundEndCallback = getRoundEndCallback(diceState);
        addAnimation(newSpot, diceState, roundEndCallback);
        Player turn = players.getTurn();
        turn.setSpot(newSpot);
    }

    private void addAnimation(Spot newSpot, DiceState diceState, CallbackAction roundEndCallback) {
        Player turnPlayer = players.getTurn();
        animations.addAnimation(new Animation(turnPlayer, board.getPath(turnPlayer.getSpot(), newSpot, turnPlayer, diceState == null), roundEndCallback));
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
        if(event instanceof KeyEvent keyEvent) {
            if(!endRoundButton.isVisible()) {
                return false;
            }
            if(keyEvent.getKey() == SPACE || keyEvent.getKey() == ENTER) {
                endRound();
                return true;
            }
        }
        return false;
    }
}
