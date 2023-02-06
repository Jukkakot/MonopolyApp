package org.example;

import controlP5.Button;
import javafx.scene.paint.Color;
import org.example.components.*;
import org.example.components.popup.ChoicePopup;
import org.example.components.popup.OkPopup;
import org.example.components.popup.PopupActions;
import org.example.components.spots.Spot;

public class Game {
    Board board;
    Dices dices;
    Players players;
    MonopolyApp p;
    Animations animations;
    OkPopup okPopup;
    ChoicePopup choicePopup;
    private final Button rollDiceButton;
    float i = 0;

    public Game(MonopolyApp p) {
        this.p = p;
        board = new Board(p);
        dices = new Dices(p);
        players = new Players(p);
        animations = new Animations();

        Spot spot = board.getSpots().get(0);
        players.addPlayer(new Player(1, "Eka", new Token(p, spot, Color.MEDIUMPURPLE), 1), spot);
        players.addPlayer(new Player(2, "Toka", new Token(p, spot, Color.PINK), 2), spot);
        players.addPlayer(new Player(3, "Kolmas", new Token(p, spot, Color.DARKOLIVEGREEN), 3), spot);
        players.addPlayer(new Player(4, "Neljäs", new Token(p, spot, Color.TURQUOISE), 4), spot);
//
//        for(Spot s : board.getSpots()) {
//            if(s instanceof PropertySpot) {
//                players.getPlayerList().forEach(player -> player.addDeed((PropertySpot) s));
//            }
//        }

        rollDiceButton = new Button(p.p5, "rollDice")
                .setPosition((int) (Spot.spotW * 5.4), (int) (Spot.spotW * 3))
                .addListener(e -> rollDice())
                .setLabel("Roll dice")
                .setFont(MonopolyApp.font20)
                .setSize(100, 50);

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

    public void rollDice() {
        if (okPopup.isVisible()) return;
        rollDiceButton.hide();
        int value = dices.roll();
        playRound(value);
    }

    private void playRound(int value) {
        Player turn = players.getTurn();
        Spot oldSpot = turn.getSpot();
        Spot newSpot = board.getNewSpot(oldSpot, value);
        addAnimation(value, turn, oldSpot, newSpot);
        newSpot.addPlayer(turn);
    }

    private void addAnimation(int diceValue, Player turnPlayer, Spot oldSpot, Spot newSpot) {
        animations.addAnimation(new Animation(turnPlayer.getToken(), board.getPath(oldSpot, diceValue, turnPlayer), getRoundEndCallback(turnPlayer, newSpot)));
    }

    private CallbackAction getRoundEndCallback(Player turnPlayer, Spot newSpot) {
        return () -> {
            String text = newSpot.getPopupText(turnPlayer);
            if (text != null) {
                showPopup(text, new PopupActions() {
                    @Override
                    public void onAccept() {
                        switchTurn(turnPlayer, newSpot);
                    }

                    @Override
                    public void onDecline() {
                        switchTurn(turnPlayer, newSpot);
                    }

                    @Override
                    public boolean isChoicePopup() {
                        //TODO not random...
                        return Math.random() < 0.5;
                    }
                });
            } else {
                switchTurn(turnPlayer, newSpot);
            }
        };
    }

    private void switchTurn(Player turnPlayer, Spot newSpot) {
        turnPlayer.moveToken(newSpot);
        players.switchTurn();
        rollDiceButton.show();
    }

    private void showPopup(String text, PopupActions callbackAction) {
        if (callbackAction.isChoicePopup()) {
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
