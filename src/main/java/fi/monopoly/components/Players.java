package fi.monopoly.components;

import controlP5.Button;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.spots.PropertySpot;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.images.Image;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.SpotProps;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
public class Players {
    private static final int PLAYER_LIST_BUTTON_DIAMETER = 42;
    private static final int PLAYER_ROW_HEIGHT = 64;
    private static final int PLAYER_LIST_ICON_OFFSET = 24;
    private static final int DEEDS_PER_ROW = 5;
    private final static int MARGIN = 16;
    private static final int SECTION_GAP = 16;
    private static final int SECTION_TITLE_BASELINE = 24;
    private static final int PLAYER_LIST_START_Y = 64;
    private static final int PLAYER_LIST_TOP_RIGHT_X = 336;
    private static final int PLAYER_LIST_TOP_RIGHT_Y = 0;
    private static final int PLAYER_LIST_ROWS_PER_COLUMN = 2;
    private static final int PLAYER_LIST_COLUMN_WIDTH = 176;
    private static final int PLAYER_LIST_HIGHLIGHT_DIAMETER = PLAYER_LIST_BUTTON_DIAMETER + 8;
    private static final int PLAYER_LIST_JAIL_HIGHLIGHT_DIAMETER = PLAYER_LIST_BUTTON_DIAMETER + 12;
    private static final int PLAYER_LIST_VISUAL_CENTER_OFFSET_X = 4;
    private static final int PLAYER_LIST_VISUAL_CENTER_OFFSET_Y = 4;
    private static final int TEXT_INFO_HEIGHT = 160;
    private static final int DEEDS_TOP_OFFSET = 88;
    private static final int DEED_PAGER_BUTTON_WIDTH = 32;
    private static final int DEED_PAGER_LEFT_X = 256;
    private static final int DEED_PAGER_RIGHT_X = 296;
    private static final int DEED_PAGER_BUTTON_Y = 0;
    private static final int[] TURN_HIGHLIGHT_COLOR = {214, 158, 46};
    private final MonopolyRuntime runtime;
    private final Coordinates baseCoords = new Coordinates(Spot.SPOT_W * 12, 0, 0);
    private final List<Player> playerList = new ArrayList<>();
    private final Map<String, Button> playerButtons = new HashMap<>();
    private final Image getOutOFJailImg;
    private final MonopolyButton previousDeedsButton;
    private final MonopolyButton nextDeedsButton;
    private int turnNum = 1;
    private Player selectedPlayer;
    private int deedPageStartIndex = 0;

    public Players(MonopolyRuntime runtime) {
        this.runtime = runtime;
        if (runtime == null) {
            this.getOutOFJailImg = null;
            this.previousDeedsButton = null;
            this.nextDeedsButton = null;
            return;
        }
        this.getOutOFJailImg = new Image(runtime, new SpotProps(0, 0, Spot.SPOT_H / 2, Spot.SPOT_W / 2), "GetOutOfJail.png");
        this.previousDeedsButton = new MonopolyButton(runtime, "previousDeeds")
                .setLabel("<")
                .setSize(DEED_PAGER_BUTTON_WIDTH, 28);
        this.nextDeedsButton = new MonopolyButton(runtime, "nextDeeds")
                .setLabel(">")
                .setSize(DEED_PAGER_BUTTON_WIDTH, 28);
        previousDeedsButton.addListener(this::showPreviousDeedsPage);
        nextDeedsButton.addListener(this::showNextDeedsPage);
    }

    public void addPlayer(Player p) {
        playerList.add(p);
        if (runtime == null) {
            return;
        }
        playerButtons.put("" + p.getId(), new MonopolyButton(runtime, "" + p.getId())
                .setValue(p.getId())
                .addListener(e -> selectPlayer(playerList.get(playerList.indexOf(p))))
                .setImages(MonopolyApp.getImage("BigToken.png", p.getColor()), MonopolyApp.getImage("BigTokenHover.png", p.getColor()), MonopolyApp.getImage("BigTokenPressed.png", p.getColor()))
                .setSize(PLAYER_LIST_BUTTON_DIAMETER, PLAYER_LIST_BUTTON_DIAMETER)
        );
    }

    public void giveRandomDeeds(Board board) {
        List<Spot> propertySpots = new ArrayList<>(board.getSpots().stream().filter(spot -> spot instanceof PropertySpot).toList());
        Collections.shuffle(propertySpots);
        int loopCount = 0;
        while (propertySpots.size() > 20) {
            PropertySpot spot = (PropertySpot) propertySpots.get(0);
            playerList.forEach(player -> {
                if (Math.random() < 0.2) {
                    boolean couldBuyProperty = player.buyProperty(spot.getProperty());
                    if (couldBuyProperty) {
                        propertySpots.remove(spot);
                    }
                }
            });
            if (loopCount++ >= 100) {
                break;
            }
        }
    }

    public int count() {
        return playerList.size();
    }

    public void forEachOtherPLayer(Player turnPlayer, Consumer<Player> update) {
        //TODO what if cant do this update to all players? like all players cant afford giving money
        playerList.stream().filter(player -> !turnPlayer.equals(player)).forEach(update);
    }

    public boolean removePlayer(Player p) {
        return playerList.remove(p);
    }

    public Player switchTurn() {
        turnNum = turnNum % playerList.size() + 1;
        List<Player> players = playerList.stream().filter(p -> p.getTurnNumber() == turnNum).toList();
        if (players.size() == 1) {
            selectPlayer(players.get(0));
            return players.get(0);
        } else {
            log.info("Player not found, trying again {}", turnNum);
            return switchTurn();
        }
    }

    public Player getTurn() {
        if (playerList.isEmpty()) {
            return null;
        }
        List<Player> players = playerList.stream().filter(p -> p.getTurnNumber() == turnNum).toList();
        if (players.size() == 1) {
            return players.get(0);
        } else {
            log.error("Turn player not found");
            return null;
        }
    }

    /**
     * Keeps the sidebar focused on a specific player when another game mode,
     * such as debt resolution, needs that player's assets to stay visible.
     */
    public void focusPlayer(Player player) {
        selectPlayer(player);
    }

    public void draw(float topOffset, boolean showPlayerList, boolean showSelectedSummary) {
        setPlayerButtonsVisible(showPlayerList);
        if (showPlayerList) {
            drawPlayerButtons(new Coordinates(PLAYER_LIST_TOP_RIGHT_X, PLAYER_LIST_TOP_RIGHT_Y));
        }
        Coordinates startCoords = new Coordinates(0, topOffset);
        if (showSelectedSummary) {
            startCoords = drawTextInfo(startCoords);
        }
        drawDeeds(startCoords);
        drawTokens();
    }

    /**
     * Player selection buttons should only stay active when the player list is
     * visible; otherwise they can overlap other sidebar sections.
     */
    private void setPlayerButtonsVisible(boolean visible) {
        playerButtons.values().forEach(button -> {
            if (visible) {
                button.show();
            } else {
                button.hide();
            }
        });
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
        MonopolyApp p = runtime.app();
        Coordinates absoluteCoods = new Coordinates(baseCoords.x() + startCoords.x(), baseCoords.y() + startCoords.y());
        selectedPlayer = selectedPlayer != null ? selectedPlayer : getTurn();
        List<Property> visibleProperties = getVisibleDeedProperties();
        p.push();
        translate(baseCoords);
        translate(startCoords);
        p.fill(46, 72, 63);
        p.textFont(runtime.font20());
        p.text(text("sidebar.section.properties"), MARGIN, SECTION_TITLE_BASELINE);
        updateDeedPagerButtons(startCoords, selectedPlayer.getOwnedProperties().size());
        p.pop();

        // Offset under the section title before drawing deed cards.
        absoluteCoods = absoluteCoods.move(Spot.SPOT_W / 2 + MARGIN, DEEDS_TOP_OFFSET);
        for (int index = 0; index < visibleProperties.size(); index++) {
            Property property = visibleProperties.get(index);
            DeedFactor.getDeed(runtime, property).getImage().draw(absoluteCoods);
            if (index < visibleProperties.size() - 1) {
                absoluteCoods = absoluteCoods.move(Spot.SPOT_W + MARGIN, 0);
            }
        }
        return new Coordinates(0, startCoords.y() + DEEDS_TOP_OFFSET + Spot.SPOT_H + SECTION_GAP);
    }

    /**
     * Draws players
     *
     * @param startCoords starting coordinates for drawing
     * @return Y-axel of where this function last drew something
     */
    private Coordinates drawPlayerButtons(Coordinates startCoords) {
        MonopolyApp p = runtime.app();
        p.push();
        translate(baseCoords);
        translate(startCoords);
        selectedPlayer = selectedPlayer != null ? selectedPlayer : getTurn();

        p.fill(46, 72, 63);
        p.textFont(runtime.font20());
        p.text(text("sidebar.section.players"), MARGIN, SECTION_TITLE_BASELINE);

        Coordinates playerListOrigin = baseCoords.move(startCoords).move(MARGIN + PLAYER_LIST_ICON_OFFSET, PLAYER_LIST_START_Y);
        for (int index = 0; index < playerList.size(); index++) {
            Player player = playerList.get(index);
            int column = index / PLAYER_LIST_ROWS_PER_COLUMN;
            int row = index % PLAYER_LIST_ROWS_PER_COLUMN;
            Coordinates absoluteCoords = playerListOrigin.move(column * PLAYER_LIST_COLUMN_WIDTH, row * PLAYER_ROW_HEIGHT);
            drawPlayerIcon(player, absoluteCoords);
        }
        p.pop();
        int columnCount = Math.max(1, (int) Math.ceil(playerList.size() / (double) PLAYER_LIST_ROWS_PER_COLUMN));
        return new Coordinates(startCoords.x() + columnCount * PLAYER_LIST_COLUMN_WIDTH, startCoords.y() + PLAYER_LIST_START_Y + PLAYER_LIST_ROWS_PER_COLUMN * PLAYER_ROW_HEIGHT + SECTION_GAP);
    }

    /**
     * Draws players text info
     *
     * @param startCoords starting coordinates for drawing
     * @return Y-axel of where this function last drew something
     */
    private Coordinates drawTextInfo(Coordinates startCoords) {
        MonopolyApp p = runtime.app();
        p.push();
        translate(baseCoords);
        translate(startCoords);
        selectedPlayer = selectedPlayer != null ? selectedPlayer : getTurn();
        p.fill(46, 72, 63);
        p.textFont(runtime.font20());
        p.text(text("sidebar.section.selected"), MARGIN, SECTION_TITLE_BASELINE);

        if (selectedPlayer != null) {
            p.fill(0);
            p.textFont(runtime.font30());
            p.text(selectedPlayer.getName(), MARGIN, 48);
            p.textFont(runtime.font20());
            p.text(text("sidebar.selected.money", selectedPlayer.getMoneyAmount()), MARGIN, 80);
            p.text(text("sidebar.selected.liquidation", selectedPlayer.getTotalLiquidationValue()), MARGIN, 104);
            p.text(text("sidebar.selected.buildings", selectedPlayer.getTotalHouseCount(), selectedPlayer.getTotalHotelCount()), MARGIN, 128);

            int getOutOfJailCardCount = selectedPlayer.getGetOutOfJailCardCount();
            if (getOutOfJailCardCount > 0) {
                p.push();
                p.translate(MARGIN + getOutOFJailImg.getWidth() / 2, 144 + getOutOFJailImg.getHeight() / 2);
                for (int i = 0; i < getOutOfJailCardCount; i++) {
                    getOutOFJailImg.draw(null);
                    p.translate(getOutOFJailImg.getWidth() + MARGIN, 0);
                }
                p.pop();
            }
        }

        p.pop();
        return new Coordinates(0, startCoords.y() + TEXT_INFO_HEIGHT + SECTION_GAP);
    }

    private void drawPlayerIcon(Player player, Coordinates absoluteCoords) {
        MonopolyApp p = runtime.app();
        Button button = playerButtons.get("" + player.getId());
        button.setPosition(absoluteCoords.x() - PLAYER_LIST_BUTTON_DIAMETER / 2f, absoluteCoords.y() - PLAYER_LIST_BUTTON_DIAMETER / 2f);
        Coordinates buttonCenter = Coordinates.of(
                button.getPosition()[0] + button.getWidth() / 2f + PLAYER_LIST_VISUAL_CENTER_OFFSET_X,
                button.getPosition()[1] + button.getHeight() / 2f + PLAYER_LIST_VISUAL_CENTER_OFFSET_Y
        );
        if (player.equals(selectedPlayer) || player.equals(getTurn()) || player.isInJail()) {
            p.pop();
            if (player.isInJail()) {
                drawCircle(buttonCenter, 255, 0, 0, true);
            }
            if (player.equals(selectedPlayer)) {
                drawCircle(buttonCenter, 0, 0, 0, false);
            }
            if (player.equals(getTurn())) {
                drawCircle(buttonCenter, TURN_HIGHLIGHT_COLOR[0], TURN_HIGHLIGHT_COLOR[1], TURN_HIGHLIGHT_COLOR[2], false);
            }
            p.push();
        }

        p.pop();
        p.fill(0);
        p.textFont(runtime.font20());
        float textX = absoluteCoords.x() + PLAYER_LIST_BUTTON_DIAMETER / 2f + MARGIN;
        p.text(player.getName(), textX, absoluteCoords.y() - 6);
        p.text(text("format.money", player.getMoneyAmount()), textX, absoluteCoords.y() + 16);
        String status = player.equals(getTurn()) ? text("sidebar.player.turn") : player.equals(selectedPlayer) ? text("sidebar.player.selected") : "";
        if (!status.isBlank()) {
            p.fill(46, 72, 63);
            p.textFont(runtime.font10());
            p.text(status, textX, absoluteCoords.y() + 32);
            p.fill(0);
            p.textFont(runtime.font20());
        }
        p.noFill();
        p.push();
    }

    private void drawCircle(Coordinates absoluteCoords, int red, int green, int blue, boolean jailCircle) {
        MonopolyApp p = runtime.app();
        p.stroke(red, green, blue);
        p.strokeWeight(5);
        p.circle(
                absoluteCoords.x(),
                absoluteCoords.y(),
                jailCircle ? PLAYER_LIST_JAIL_HIGHLIGHT_DIAMETER : PLAYER_LIST_HIGHLIGHT_DIAMETER
        );
    }

    private void selectPlayer(Player player) {
        selectedPlayer = player;
        deedPageStartIndex = 0;
    }

    private List<Property> getVisibleDeedProperties() {
        if (selectedPlayer == null) {
            return List.of();
        }
        List<Property> sortedProperties = selectedPlayer.getOwnedProperties().stream()
                .sorted(Comparator.comparingInt(property -> property.getSpotType().ordinal()))
                .toList();
        deedPageStartIndex = Math.max(0, Math.min(deedPageStartIndex, Math.max(0, sortedProperties.size() - DEEDS_PER_ROW)));
        int endIndex = Math.min(sortedProperties.size(), deedPageStartIndex + DEEDS_PER_ROW);
        return sortedProperties.subList(deedPageStartIndex, endIndex);
    }

    private void updateDeedPagerButtons(Coordinates startCoords, int deedCount) {
        if (previousDeedsButton == null || nextDeedsButton == null) {
            return;
        }
        float baseX = baseCoords.x() + startCoords.x();
        float baseY = baseCoords.y() + startCoords.y();
        previousDeedsButton.setPosition(baseX + DEED_PAGER_LEFT_X, baseY + DEED_PAGER_BUTTON_Y);
        nextDeedsButton.setPosition(baseX + DEED_PAGER_RIGHT_X, baseY + DEED_PAGER_BUTTON_Y);

        boolean hasPreviousPage = deedPageStartIndex > 0;
        boolean hasNextPage = deedPageStartIndex + DEEDS_PER_ROW < deedCount;
        if (hasPreviousPage) {
            previousDeedsButton.show();
        } else {
            previousDeedsButton.hide();
        }
        if (hasNextPage) {
            nextDeedsButton.show();
        } else {
            nextDeedsButton.hide();
        }
    }

    private void showPreviousDeedsPage() {
        deedPageStartIndex = Math.max(0, deedPageStartIndex - DEEDS_PER_ROW);
    }

    private void showNextDeedsPage() {
        if (selectedPlayer == null) {
            return;
        }
        int maxStartIndex = Math.max(0, selectedPlayer.getOwnedProperties().size() - DEEDS_PER_ROW);
        deedPageStartIndex = Math.min(maxStartIndex, deedPageStartIndex + DEEDS_PER_ROW);
    }

    private void translate(Coordinates c) {
        MonopolyApp p = runtime.app();
        p.translate(c.x(), c.y());
    }
}
