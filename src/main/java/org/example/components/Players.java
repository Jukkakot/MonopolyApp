package org.example.components;

import controlP5.Button;
import lombok.Getter;
import org.example.MonopolyApp;
import org.example.components.spots.PropertySpot;
import org.example.components.spots.Spot;
import org.example.types.PlaceType;
import org.example.types.StreetType;
import org.example.utils.Coordinates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.example.MonopolyApp.IMAGES;

public class Players {
    private final Coordinates coords = new Coordinates(Spot.spotW * 12, 0, 0);
    @Getter //FIXME temporary!
    private final List<Player> playerList = new ArrayList<>();
    private final List<Button> playerButtons = new ArrayList<>();
    private int turnNum = 1;
    private final MonopolyApp p;
    private Player selectedPlayer;

    public Players(MonopolyApp p) {
        this.p = p;
    }

    public Player addPlayer(Player p, Spot spot) {
        playerList.add(p);
        p.getToken().setSpot(spot);
        spot.addPlayer(p);
        playerButtons.add(new Button(this.p.p5, "" + p.getId())
                .setValue(p.getId())
                .addListener(e -> selectedPlayer = playerList.get(playerList.indexOf(p)))
                .setImages(IMAGES.get("BigToken.png"), IMAGES.get("BigTokenHover.png"), IMAGES.get("BigTokenPressed.png"))
                .setSize(Token.TOKEN_RADIUS * 2, Token.TOKEN_RADIUS * 2)
        );
        return p;
    }

    public void removePlayer(Player p) {
        playerList.remove(p);
    }

    public Player switchTurn() {
        turnNum = turnNum++ % playerList.size() + 1;
        List<Player> players = playerList.stream().filter(p -> p.getTurnNumber() == turnNum).toList();
        if (players.size() == 1) {
            selectedPlayer = players.get(0);
            return players.get(0);
        } else {
            System.out.println("Player not found, trying again " + turnNum);
            return switchTurn();
        }
    }

    public Player getTurn() {
        if (playerList.isEmpty()) return null;
        List<Player> players = playerList.stream().filter(p -> p.getTurnNumber() == turnNum).toList();
        if (players.size() == 1) {
            return players.get(0);
        } else {
            System.out.println("Player not found");
            return null;
        }
    }

    public void draw() {
        drawPlayers();
        drawDeeds();
    }

    private void drawDeeds() {
        p.push();
        p.translate((int) (coords.x() + Spot.spotW / 2) + 10, Spot.spotH + (int) (coords.y() + Spot.spotW));
        selectedPlayer = selectedPlayer != null ? selectedPlayer : getTurn();
        Map<StreetType, List<PropertySpot>> deedsMap = selectedPlayer.getDeeds().getDeeds();
        int index = 0;
        for(StreetType pt : deedsMap.keySet()) {
            for (PropertySpot ps : deedsMap.get(pt)) {
                ps.drawDeed(new Coordinates(0, 0, 0));
                index++;
                if (index > 0 && index % 5 == 0) {
                    p.pop();
                    p.push();
                    int row = index / 5;
                    p.translate((int) (coords.x() + Spot.spotW / 2) + 10, row * 10 + row * Spot.spotH + (Spot.spotH + (int) (coords.y() + Spot.spotW)));
                } else {
                    p.translate(Spot.spotW + 10, 0);
                }

            }
        }


        p.pop();
    }

    private void drawPlayers() {
        p.push();
        p.translate((int) (coords.x() + Token.TOKEN_RADIUS * 1.5), (int) (coords.y() + Token.TOKEN_RADIUS * 1.5));
        Coordinates absoluteCoords = new Coordinates((int) (coords.x() + Token.TOKEN_RADIUS * 1.5), (int) (coords.y() + Token.TOKEN_RADIUS * 1.5));
        for (Player player : playerList) {
            drawPlayerIcon(new Coordinates(0, 0, 0), player, absoluteCoords);
            int index = playerList.indexOf(player);
            if (index > 0 && index % 2 == 0) {
                p.pop();
                p.push();
                p.translate((int) (coords.x() + Token.TOKEN_RADIUS * 1.5), 2 * (int) (coords.y() + Token.TOKEN_RADIUS * 2));
                absoluteCoords = new Coordinates((int) (coords.x() + Token.TOKEN_RADIUS * 1.5), 2 * (int) (coords.y() + Token.TOKEN_RADIUS * 2));
            } else {
                p.translate(10 + Token.TOKEN_RADIUS * 2 + player.getName().length() * 20, 0);
                absoluteCoords = absoluteCoords.move(10 + Token.TOKEN_RADIUS * 2 + player.getName().length() * 20, 0);
            }

        }
        p.pop();
    }

    private void drawPlayerIcon(Coordinates coords, Player player, Coordinates absoluteCoords) {
        p.translate(coords.x(), coords.y());
        if (player.equals(selectedPlayer)) {
//            p.stroke(toColor(p, player.getToken().getColor()));
            p.stroke(0);
            p.strokeWeight(10);
            p.circle(0, 0, Token.TOKEN_RADIUS * 2);
        }

        Button button = playerButtons.stream().filter(b -> b.getName().equals("" + player.getId())).toList().get(0);
        button.setPosition(absoluteCoords.x() - Token.TOKEN_RADIUS, absoluteCoords.y() - Token.TOKEN_RADIUS);

        p.fill(0);
        p.textSize(30);
        p.text(player.getName(), Token.TOKEN_RADIUS + 10, 0);
    }
}
