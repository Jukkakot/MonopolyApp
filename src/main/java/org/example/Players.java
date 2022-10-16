package org.example;

import org.example.components.spots.Spot;

import java.util.ArrayList;
import java.util.List;

public class Players {

    private final List<Player> playerList = new ArrayList<>();
    private int turnNum = 1;

    public Player addPlayer(Player p, Spot spot) {
        playerList.add(p);
        p.getToken().setSpot(spot);
        spot.addPlayer(p);
       return p;
    }
    public void removePlayer(Player p) {
        playerList.remove(p);
    }

    public Player switchTurn() {
        turnNum = turnNum++ % playerList.size() + 1;
        List<Player> players = playerList.stream().filter(p -> p.getTurnNumber() == turnNum).toList();
        if (players.size() == 1) {
            return players.get(0);
        } else {
            System.out.println("Player not found, trying again " + turnNum);
            return switchTurn();
        }
    }
    public Player getTurn() {
        List<Player> players = playerList.stream().filter(p -> p.getTurnNumber() == turnNum).toList();
        if (players.size() == 1) {
            return players.get(0);
        } else {
            System.out.println("Player not found");
            return null;
        }
    }
    public void draw() {
        playerList.forEach(Player::draw);
    }

}
