package org.example.components;

import controlP5.Button;
import org.example.MonopolyApp;
import org.example.components.spots.PropertySpot;
import org.example.components.spots.Spot;
import org.example.types.StreetType;
import org.example.utils.Coordinates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Players {
    private static final int PLAYERS_PER_ROW = 3;
    private final static int DEEDS_PER_ROW = 5;
    private final static int MARGIN = 10;
    private static final int TEXT_INFO_HEIGHT = 50;
    private final Coordinates baseCoords = new Coordinates(Spot.SPOT_W * 12, 0, 0);
    private final List<Player> playerList = new ArrayList<>();
    private final Map<String, Button> playerButtons = new HashMap<>();
    private int turnNum = 1;
    private final MonopolyApp p = MonopolyApp.self;
    private Player selectedPlayer;

    public void addPlayer(Player p) {
        playerList.add(p);
        playerButtons.put("" + p.getId(), new Button(MonopolyApp.p5, "" + p.getId())
                .setValue(p.getId())
                .addListener(e -> selectedPlayer = playerList.get(playerList.indexOf(p)))
                .setImages(MonopolyApp.getImage("BigToken.png", p.getColor()), MonopolyApp.getImage("BigTokenHover.png", p.getColor()), MonopolyApp.getImage("BigTokenPressed.png", p.getColor()))
                .setSize(PlayerToken.TOKEN_RADIUS * 2, PlayerToken.TOKEN_RADIUS * 2)
        );
    }

    public void giveRandomDeeds(Board board) {
        List<Spot> propertySpots = new ArrayList<>(board.getSpots().stream().filter(spot -> spot instanceof PropertySpot).toList());
        while (propertySpots.size() > 20) {
            PropertySpot spot = (PropertySpot) propertySpots.get(0);
            playerList.forEach(player -> {
                if (Math.random() < 0.2) {
                    boolean couldBuyProperty = player.buyProperty(spot);
                    if (couldBuyProperty) {
                        propertySpots.remove(spot);
                    }
                }
            });
        }
    }

    public boolean removePlayer(Player p) {
        return playerList.remove(p);
    }

    public Player switchTurn() {
        turnNum = turnNum % playerList.size() + 1;
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
        drawAll(this::drawPlayerButtons, this::drawTextInfo, this::drawDeeds);
        drawTokens();
    }

    public void drawAll(Drawable... drawables) {
        Coordinates startCoords = new Coordinates();
        for (Drawable drawable : drawables) {
            startCoords = drawable.draw(startCoords);
        }
    }

    public void drawTokens() {
        playerList.forEach(PlayerToken::draw);
    }

    /**
     * Draws players deeds
     *
     * @param startCoords starting coordinates for drawing
     * @return Y-axel of where this function last drew something
     */
    private Coordinates drawDeeds(Coordinates startCoords) {
        Coordinates absoluteCoods = new Coordinates(baseCoords.x() + startCoords.x(), baseCoords.y() + startCoords.y());
        float deedTotalHeight = Spot.SPOT_H + MARGIN;
        //Offset by needed amounts...
        absoluteCoods = absoluteCoods.move(Spot.SPOT_W / 2 + MARGIN, deedTotalHeight / 2);
        selectedPlayer = selectedPlayer != null ? selectedPlayer : getTurn();
        Map<StreetType, List<PropertySpot>> deedsMap = selectedPlayer.getDeeds().getDeeds();
        int index = 0;
        int totalDX = 0;

        for (StreetType pt : deedsMap.keySet()) {
            for (PropertySpot ps : deedsMap.get(pt)) {
                ps.draw(absoluteCoods);
                index++;
                if (index % DEEDS_PER_ROW == 0) {
                    absoluteCoods = absoluteCoods.move(-totalDX, deedTotalHeight);
                    totalDX = 0;
                } else {
                    float dX = Spot.SPOT_W + MARGIN;
                    absoluteCoods = absoluteCoods.move(dX, 0);
                    totalDX += dX;
                }
            }
        }
        int rowCount = (selectedPlayer.getAllDeeds().size() / DEEDS_PER_ROW);
        if (selectedPlayer.getAllDeeds().size() % DEEDS_PER_ROW != 0) {
            rowCount++;
        }
        return new Coordinates(0, startCoords.y() + (deedTotalHeight * rowCount) + deedTotalHeight);
    }

    /**
     * Draws players
     *
     * @param startCoords starting coordinates for drawing
     * @return Y-axel of where this function last drew something
     */
    private Coordinates drawPlayerButtons(Coordinates startCoords) {
        p.push();
        translate(baseCoords);
        Coordinates absoluteCoords = baseCoords.move(startCoords);
        absoluteCoords = absoluteCoords.move((float) (PlayerToken.TOKEN_RADIUS * 1.5), (float) (PlayerToken.TOKEN_RADIUS * 1.5));
        translate(absoluteCoords);
        int totalDX = 0;
        int tokenHeight = 3 * PlayerToken.TOKEN_RADIUS;
        for (Player player : playerList) {
            drawPlayerIcon(player, absoluteCoords);
            int index = playerList.indexOf(player);

            if ((index + 1) % PLAYERS_PER_ROW == 0) {

                absoluteCoords = absoluteCoords.move(-totalDX, tokenHeight);
                translate(absoluteCoords);
                totalDX = 0;
            } else {
                int dX = MARGIN + PlayerToken.TOKEN_RADIUS * 2 + player.getName().length() * 17;
                totalDX += dX;
                absoluteCoords = absoluteCoords.move(dX, 0);
                p.translate(dX, 0);
            }
        }
        p.pop();
        int rowCount = (playerList.size() / PLAYERS_PER_ROW);
        if (playerList.size() % PLAYERS_PER_ROW != 0) {
            rowCount++;
        }
        return new Coordinates(0, startCoords.y() + (rowCount * tokenHeight));
    }

    /**
     * Draws players text info
     *
     * @param startCoords starting coordinates for drawing
     * @return Y-axel of where this function last drew something
     */
    private Coordinates drawTextInfo(Coordinates startCoords) {
        p.push();
        translate(baseCoords);
        translate(startCoords);
        float textYAxel = (float) TEXT_INFO_HEIGHT / 3;
        p.translate(0, MARGIN);

        p.fill(0);
        p.textFont(MonopolyApp.font30);
        int moneyAmount = selectedPlayer != null ? selectedPlayer.getMoney() : 0;
        p.text("M" + moneyAmount, MARGIN, textYAxel);

        p.pop();
        return new Coordinates(0, startCoords.y() + TEXT_INFO_HEIGHT);
    }

    private void drawPlayerIcon(Player player, Coordinates absoluteCoords) {

        if (player.equals(selectedPlayer)) {
//            p.stroke(toColor( player.getToken().getColor()));
            //TODO why pop before push?
            p.pop();
            p.stroke(0);
            p.strokeWeight(5);
            p.circle(absoluteCoords.x(), absoluteCoords.y(), PlayerToken.TOKEN_RADIUS * 2);
            p.push();
        }

        Button button = playerButtons.get("" + player.getId());
        button.setPosition(absoluteCoords.x() - PlayerToken.TOKEN_RADIUS, absoluteCoords.y() - PlayerToken.TOKEN_RADIUS);
        //TODO why pop before push?
        p.pop();
        p.fill(0);
        p.textFont(MonopolyApp.font30);
        p.text(player.getName(), absoluteCoords.x() + PlayerToken.TOKEN_RADIUS + MARGIN, absoluteCoords.y());
        p.noFill();
        p.push();
    }

    private void translate(Coordinates c) {
        p.translate(c.x(), c.y());
    }

    private interface Drawable {
        /**
         * Draws some player info
         *
         * @param startCoords starting coordinates for drawing
         * @return Y-axel of where this function last drew something
         */
        Coordinates draw(Coordinates startCoords);
    }
}
