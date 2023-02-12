package org.example;

import controlP5.Button;
import javafx.scene.paint.Color;
import org.example.components.*;
import org.example.components.popup.ChoicePopup;
import org.example.components.popup.OkPopup;
import org.example.components.popup.PopupActions;
import org.example.components.spots.Spot;
import org.example.utils.DiceValue;

public class Game {
    Board board;
    Dices dices;
    Players players;
    MonopolyApp p;
    Animations animations;
    OkPopup okPopup;
    ChoicePopup choicePopup;
    private final Button endRoundButton;
    float i = 0;

    public Game(MonopolyApp p) {
        this.p = p;
        board = new Board(p);
        dices = new Dices(p) {
            @Override
            public void rollDice() {
                super.rollDice();
                Game.this.rollDice();
            }
        };
        players = new Players(p);
        animations = new Animations();

        Spot spot = board.getSpots().get(0);
        players.addPlayer(new Player(1, "Eka", new Token(p, spot, Color.MEDIUMPURPLE), 1), spot);
        players.addPlayer(new Player(2, "Toka", new Token(p, spot, Color.PINK), 2), spot);
        players.addPlayer(new Player(3, "Kolmas", new Token(p, spot, Color.DARKOLIVEGREEN), 3), spot);
        players.addPlayer(new Player(4, "Neljäs", new Token(p, spot, Color.TURQUOISE), 4), spot);
        players.addPlayer(new Player(5, "Viides", new Token(p, spot, Color.MEDIUMPURPLE), 5), spot);
        players.addPlayer(new Player(6, "Kuudes", new Token(p, spot, Color.PINK), 6), spot);

        players.giveRandomDeeds(board);

        endRoundButton = new Button(p.p5, "endRound")
                .setPosition((int) (Spot.spotW * 5.4), (int) (p.height - Spot.spotW * 3))
                .addListener(e -> endRound())
                .setLabel("End round")
                .setFont(MonopolyApp.font20)
                .setSize(100, 50)
                .hide();

        okPopup = new OkPopup(p);
        p.p5.addCanvas(okPopup);

        choicePopup = new ChoicePopup(p);
        p.p5.addCanvas(choicePopup);
    }

    public void draw() {
        animations.updateAnimations();
        board.draw(null);
        dices.draw(null);
        players.draw();
        i += 0.5;
    }

    private void rollDice() {
        if (okPopup.isVisible()) return;
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
        CallbackAction roundEndCallback = getRoundEndCallback(newSpot, diceState);
        addAnimation(newSpot, diceState, roundEndCallback);
        Player turn = players.getTurn();
        turn.setSpot(newSpot);
    }

    private void addAnimation(Spot newSpot, DiceState diceState, CallbackAction roundEndCallback) {
        Player turnPlayer = players.getTurn();
        animations.addAnimation(new Animation(turnPlayer, board.getPath(turnPlayer.getSpot(), newSpot, turnPlayer, diceState == null), roundEndCallback));
    }

    private CallbackAction getRoundEndCallback(Spot newSpot, DiceState diceState) {
        Player turnPlayer = players.getTurn();
        return () -> {
            String text = newSpot.getPopupText(turnPlayer);
            if (text != null) {
                showPopup(text, new PopupActions() {
                    @Override
                    public void onAccept() {
                        doRoundEvent(diceState);
                    }

                    @Override
                    public void onDecline() {
                        onAccept();
                    }

                    @Override
                    public boolean isChoicePopup() {
                        return false;
                    }
                });
            } else {
                doRoundEvent(diceState);
            }
        };
    }

    private void doRoundEvent(DiceState diceState) {
        if (diceState == null || diceState.equals(DiceState.NOREROLL)) {
            endRoundButton.show();
        } else if (diceState.equals(DiceState.JAIL)) {
            //shouldn't go here anymore?
            System.out.println("JAIL CASE, shouldn't happen?");
//            playRound(board.getJailSpot(), null);
        } else {
            dices.show();
        }
    }

    private void showPopup(String text, PopupActions callbackAction) {
        if (callbackAction != null && callbackAction.isChoicePopup()) {
            choicePopup.setPopupText(text);
            choicePopup.setButtonActions(callbackAction);
            choicePopup.show();
        } else {
            okPopup.setPopupText(text);
            okPopup.setButtonActions(callbackAction);
            okPopup.show();
        }
    }
}
