package org.example;

import controlP5.Button;
import javafx.scene.paint.Color;
import org.example.components.*;
import org.example.components.popup.OkPopup;
import org.example.components.popup.Popup;
import org.example.components.spots.PropertySpot;
import org.example.components.spots.Spot;

public class Game {
    Board board;
    Dices dices;
    Players players;
    MonopolyApp p;
    Animations animations;
    Popup confirmPopup;
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

        confirmPopup = new OkPopup(p, "Default text");
        p.p5.addCanvas(confirmPopup);
    }

    public void draw() {
        animations.updateAnimations();
        board.draw(null);
        dices.draw(null);
        players.draw();
        rollDiceButton.setVisible(!animations.isRunning());
        i += 0.5;
    }

    public void rollDice() {
        if (confirmPopup.isVisible()) return;
        int value = dices.roll();
        Player turn = players.getTurn();
        Spot oldSpot = turn.getToken().getSpot();
        Spot newSpot = board.getNewSpot(oldSpot, value);
        animations.addAnimation(new Animation(turn.getToken(), board.getPath(oldSpot, value, turn), () -> {
            confirmPopup.show(newSpot.getPopupText(turn));
            //TODO why this called twice?
        }));
        players.switchTurn();
        turn.moveToken(newSpot);
//        confirmPopup.show();

    }
}
