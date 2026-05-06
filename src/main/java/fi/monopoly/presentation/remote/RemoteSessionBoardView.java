package fi.monopoly.presentation.remote;

import fi.monopoly.application.command.*;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.client.session.desktop.DesktopClientSessionModel;
import fi.monopoly.client.session.desktop.DesktopSessionRenderView;
import fi.monopoly.domain.session.*;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;
import fi.monopoly.utils.UiTokens;
import lombok.extern.slf4j.Slf4j;
import processing.core.PApplet;
import processing.core.PFont;

import java.util.*;

/**
 * Processing-based board renderer driven entirely by incoming {@link SessionState} snapshots.
 *
 * <p>This view is used in HTTP-backed remote mode. It draws the board and sidebar from the latest
 * {@link SessionState} received via SSE and routes all player actions through the HTTP command
 * port, without any local game-loop authority.</p>
 */
@Slf4j
public final class RemoteSessionBoardView implements DesktopSessionRenderView, MouseInteractiveView {

    private static final int CELLS = 11;

    private final PApplet app;
    private final DesktopClientSessionModel sessionModel;
    private final SessionCommandPort commandPort;
    private final String sessionId;

    private final List<ActionButton> buttons = new ArrayList<>();
    private long lastClickMs = 0;

    public RemoteSessionBoardView(
            PApplet app,
            DesktopClientSessionModel sessionModel,
            SessionCommandPort commandPort,
            String sessionId) {
        this.app = app;
        this.sessionModel = sessionModel;
        this.commandPort = commandPort;
        this.sessionId = sessionId;
    }

    // -------------------------------------------------------------------------
    // DesktopSessionRenderView
    // -------------------------------------------------------------------------

    @Override
    public void draw() {
        SessionState state = sessionModel.sessionState();
        buttons.clear();

        int cell = UiTokens.remoteBoardCellSize();
        int boardSize = CELLS * cell;
        int boardX = UiTokens.remoteBoardLeftMargin();
        int boardY = Math.max(UiTokens.remoteBoardLeftMargin(), (app.height - boardSize) / 2);
        int sidebarX = boardX + boardSize + UiTokens.remoteBoardSidebarGap();

        if (state == null) {
            drawWaiting(boardX, boardY, boardSize, sidebarX);
            return;
        }

        Map<String, int[]> playerColors = buildPlayerColors(state);
        Map<String, String> propertyOwners = buildPropertyOwners(state);
        Map<String, int[]> propertyBuildings = buildPropertyBuildings(state);
        Map<Integer, List<PlayerSnapshot>> tokensOnSpot = buildTokensOnSpot(state);

        drawBoard(state, boardX, boardY, cell, playerColors, propertyOwners, propertyBuildings, tokensOnSpot);
        drawSidebar(state, sidebarX, boardY, playerColors);
    }

    @Override
    public List<String> debugPerformanceLines(float fps) {
        SessionState state = sessionModel.sessionState();
        List<String> lines = new ArrayList<>();
        lines.add("Remote HTTP mode");
        lines.add(String.format("FPS: %.0f", fps));
        if (state != null) {
            lines.add("v" + state.version() + " " + (state.turn() != null ? state.turn().phase() : "?"));
            lines.add("sid:" + sessionId.substring(0, Math.min(8, sessionId.length())) + "…");
        } else {
            lines.add("Odotetaan tilaa…");
        }
        return lines;
    }

    // -------------------------------------------------------------------------
    // MouseInteractiveView
    // -------------------------------------------------------------------------

    @Override
    public void handleMousePressed(int mx, int my) {
        long now = System.currentTimeMillis();
        if (now - lastClickMs < 350) return;
        for (ActionButton btn : List.copyOf(buttons)) {
            if (btn.contains(mx, my)) {
                lastClickMs = now;
                SessionCommand cmd = btn.command();
                log.debug("Remote action: {}", cmd.getClass().getSimpleName());
                commandPort.handle(cmd);
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Board drawing
    // -------------------------------------------------------------------------

    private void drawBoard(
            SessionState state, int bx, int by, int cell,
            Map<String, int[]> playerColors,
            Map<String, String> propertyOwners,
            Map<String, int[]> propertyBuildings,
            Map<Integer, List<PlayerSnapshot>> tokensOnSpot) {

        // Board background
        app.fill(13, 17, 23);
        app.stroke(48, 54, 61);
        app.strokeWeight(1);
        app.rect(bx, by, CELLS * cell, CELLS * cell);

        List<SpotType> spots = SpotType.SPOT_TYPES;
        for (int idx = 0; idx < spots.size(); idx++) {
            SpotType spot = spots.get(idx);
            int[] rc = boardIndexToRowCol(idx);
            int row = rc[0], col = rc[1];
            int cx = bx + col * cell;
            int cy = by + row * cell;

            int[] bgColor = cellBgColor(spot.streetType);
            app.fill(bgColor[0], bgColor[1], bgColor[2]);
            app.stroke(48, 54, 61);
            app.strokeWeight(1);
            app.rect(cx, cy, cell, cell);

            // Color strip
            int[] stripColor = stripColor(spot.streetType);
            app.fill(stripColor[0], stripColor[1], stripColor[2]);
            app.noStroke();
            int stripH = UiTokens.remoteBoardColorStripHeight();
            app.rect(cx + 1, cy + 1, cell - 2, stripH);

            // Owner indicator (bottom border)
            String owner = propertyOwners.get(spot.name());
            if (owner != null) {
                int[] pc = playerColors.getOrDefault(owner, new int[]{170, 170, 170});
                app.fill(pc[0], pc[1], pc[2]);
                app.noStroke();
                app.rect(cx + 1, cy + cell - 4, cell - 2, 3);
            }

            // Building dots
            int[] buildings = propertyBuildings.get(spot.name());
            if (buildings != null) {
                drawBuildings(cx, cy, cell, buildings[0], buildings[1]);
            }

            // Player tokens
            List<PlayerSnapshot> here = tokensOnSpot.getOrDefault(idx, List.of());
            drawTokens(cx, cy, cell, here, playerColors);
        }

        // Spot name labels (small text in non-corner cells)
        app.textSize(7);
        for (int idx = 0; idx < spots.size(); idx++) {
            SpotType spot = spots.get(idx);
            int[] rc = boardIndexToRowCol(idx);
            int cx = bx + rc[1] * cell;
            int cy = by + rc[0] * cell;
            app.fill(180, 180, 180);
            app.noStroke();
            app.textAlign(PApplet.CENTER, PApplet.CENTER);
            int stripH = UiTokens.remoteBoardColorStripHeight();
            String label = shortLabel(spot);
            app.text(label, cx + cell / 2f, cy + stripH + (cell - stripH) / 2f - 4);
        }
    }

    private void drawBuildings(int cx, int cy, int cell, int houseCount, int hotelCount) {
        int stripH = UiTokens.remoteBoardColorStripHeight();
        if (hotelCount > 0) {
            app.fill(230, 50, 50);
            app.noStroke();
            app.rect(cx + cell / 2f - 5, cy + stripH + 2, 10, 6, 2);
        } else if (houseCount > 0) {
            app.fill(50, 200, 80);
            app.noStroke();
            for (int h = 0; h < houseCount; h++) {
                app.rect(cx + 3 + h * 8, cy + stripH + 2, 6, 5, 1);
            }
        }
    }

    private void drawTokens(int cx, int cy, int cell, List<PlayerSnapshot> players,
                             Map<String, int[]> playerColors) {
        if (players.isEmpty()) return;
        int r = UiTokens.remoteBoardTokenRadius();
        int total = players.size();
        int startX = cx + cell / 2 - (total * (r * 2 + 2)) / 2 + r;
        int tokenY = cy + cell - r - 6;
        for (int i = 0; i < total; i++) {
            PlayerSnapshot p = players.get(i);
            int[] col = playerColors.getOrDefault(p.playerId(), new int[]{170, 170, 170});
            app.fill(col[0], col[1], col[2]);
            app.stroke(255, 255, 255, 180);
            app.strokeWeight(1.5f);
            app.circle(startX + i * (r * 2 + 2), tokenY, r * 2);
        }
    }

    // -------------------------------------------------------------------------
    // Sidebar drawing
    // -------------------------------------------------------------------------

    private void drawSidebar(SessionState state, int sx, int sy, Map<String, int[]> playerColors) {
        int sw = UiTokens.remoteBoardSidebarWidth();
        int cell = UiTokens.remoteBoardCellSize();
        int boardBottom = sy + CELLS * cell;

        // Header
        app.fill(22, 30, 22, 220);
        app.noStroke();
        app.rect(sx, sy, sw, boardBottom - sy, 8);

        int y = sy + 12;

        // Turn section
        TurnState turn = state.turn();
        if (turn != null && turn.activePlayerId() != null) {
            String activeId = turn.activePlayerId();
            String activeName = playerName(state, activeId);
            int[] col = playerColors.getOrDefault(activeId, new int[]{200, 200, 200});

            app.fill(col[0], col[1], col[2]);
            app.textSize(13);
            app.textAlign(PApplet.LEFT, PApplet.TOP);
            app.noStroke();
            app.text("Vuoro: " + activeName, sx + 10, y);
            y += 18;

            app.fill(120, 180, 255);
            app.textSize(10);
            app.text(phaseName(turn.phase()), sx + 10, y);
            y += 20;
        }

        // Player cards
        y += 6;
        app.fill(120, 140, 120);
        app.textSize(9);
        app.text("PELAAJAT", sx + 10, y);
        y += 14;

        int cardH = UiTokens.remoteBoardPlayerCardHeight();
        int cardGap = UiTokens.remoteBoardPlayerCardGap();
        String activePlayerId = turn != null ? turn.activePlayerId() : null;

        for (PlayerSnapshot p : state.players()) {
            boolean isActive = p.playerId().equals(activePlayerId);
            boolean bankrupt = p.bankrupt() || p.eliminated();

            // Card background
            int[] col = playerColors.getOrDefault(p.playerId(), new int[]{100, 100, 100});
            if (isActive) {
                app.fill(col[0] / 4, col[1] / 4, col[2] / 4, 230);
                app.stroke(col[0], col[1], col[2]);
                app.strokeWeight(2);
            } else {
                app.fill(18, 25, 18, 200);
                app.stroke(48, 54, 61);
                app.strokeWeight(1);
            }
            app.rect(sx + 6, y, sw - 12, cardH, 4);

            // Player dot
            app.fill(col[0], col[1], col[2]);
            app.noStroke();
            app.circle(sx + 18, y + cardH / 2f, 12);

            // Name
            app.fill(bankrupt ? 90 : 220, bankrupt ? 90 : 220, bankrupt ? 90 : 220);
            app.textSize(11);
            app.textAlign(PApplet.LEFT, PApplet.CENTER);
            String nameLabel = bankrupt ? p.name() + " (konkurssi)" : p.name();
            app.text(nameLabel, sx + 28, y + 14);

            // Spot name
            app.fill(130, 160, 130);
            app.textSize(9);
            int spotIdx = p.boardIndex();
            String spotLabel = spotIdx >= 0 && spotIdx < SpotType.SPOT_TYPES.size()
                    ? shortLabel(SpotType.SPOT_TYPES.get(spotIdx))
                    : "#" + spotIdx;
            if (p.inJail()) spotLabel += " 🔒";
            app.text(spotLabel, sx + 28, y + 28);

            // Cash
            app.fill(63, 185, 80);
            app.textSize(11);
            app.textAlign(PApplet.RIGHT, PApplet.CENTER);
            app.text("€" + p.cash(), sx + sw - 10, y + cardH / 2f);

            app.textAlign(PApplet.LEFT, PApplet.TOP);
            y += cardH + cardGap;
        }

        // Action buttons
        y += 10;
        app.fill(120, 140, 120);
        app.textSize(9);
        app.noStroke();
        app.text("TOIMINNOT", sx + 10, y);
        y += 14;

        y = drawActionButtons(state, sx, y, sw, activePlayerId);

        // Session ID at bottom
        if (y < boardBottom - 20) {
            app.fill(60, 80, 60);
            app.textSize(8);
            app.textAlign(PApplet.LEFT, PApplet.BOTTOM);
            app.text("sid: " + sessionId.substring(0, Math.min(12, sessionId.length())) + "…", sx + 8, boardBottom - 6);
        }
    }

    private int drawActionButtons(SessionState state, int sx, int y, int sw, String activeId) {
        TurnState turn = state.turn();
        if (turn == null || activeId == null) return y;

        TurnPhase phase = turn.phase();
        int bh = UiTokens.remoteBoardActionButtonHeight();
        int gap = UiTokens.remoteBoardActionButtonGap();

        if (phase == TurnPhase.WAITING_FOR_ROLL) {
            y = addButton(sx, y, sw, bh, gap, "🎲 Heitä nopat",
                    new RollDiceCommand(sessionId, activeId), new int[]{35, 134, 54});
        }

        if (phase == TurnPhase.WAITING_FOR_END_TURN) {
            y = addManagementButtons(state, sx, y, sw, bh, gap, activeId);
            y = addButton(sx, y, sw, bh, gap, "✅ Lopeta vuoro",
                    new EndTurnCommand(sessionId, activeId), new int[]{35, 134, 54});
        }

        if (phase == TurnPhase.WAITING_FOR_DECISION && state.pendingDecision() != null) {
            var dec = state.pendingDecision();
            String propId = extractPropertyId(dec);
            y = addButton(sx, y, sw, bh, gap, "💰 Osta kiinteistö",
                    new BuyPropertyCommand(sessionId, activeId, dec.decisionId(), propId),
                    new int[]{35, 100, 200});
            y = addButton(sx, y, sw, bh, gap, "❌ Ohita",
                    new DeclinePropertyCommand(sessionId, activeId, dec.decisionId(), propId),
                    new int[]{100, 50, 50});
        }

        if (phase == TurnPhase.WAITING_FOR_AUCTION && state.auctionState() != null) {
            var auction = state.auctionState();
            int nextBid = auction.minimumNextBid() > 0 ? auction.minimumNextBid() : auction.currentBid() + 10;
            y = addButton(sx, y, sw, bh, gap, "📢 Huuto +" + nextBid,
                    new PlaceAuctionBidCommand(sessionId, activeId, auction.auctionId(), nextBid),
                    new int[]{35, 100, 200});
            y = addButton(sx, y, sw, bh, gap, "🚫 Passi",
                    new PassAuctionCommand(sessionId, activeId, auction.auctionId()),
                    new int[]{100, 50, 50});
        }

        if (phase == TurnPhase.RESOLVING_DEBT && state.activeDebt() != null) {
            var debt = state.activeDebt();
            if (debt.allowedActions().contains(DebtAction.PAY_DEBT_NOW)) {
                y = addButton(sx, y, sw, bh, gap, "💸 Maksa velka",
                        new PayDebtCommand(sessionId, activeId, debt.debtId()),
                        new int[]{35, 100, 200});
            }
            if (debt.allowedActions().contains(DebtAction.MORTGAGE_PROPERTY)) {
                for (PropertyStateSnapshot prop : debtorUnmortgagedProperties(state, debt.debtorPlayerId())) {
                    String label = "📜 Kiinnitä " + shortLabel(SpotType.valueOf(prop.propertyId()));
                    y = addButton(sx, y, sw, bh, gap, label,
                            new MortgagePropertyForDebtCommand(sessionId, activeId, debt.debtId(), prop.propertyId()),
                            new int[]{100, 80, 20});
                }
            }
            if (debt.allowedActions().contains(DebtAction.SELL_BUILDING)) {
                for (PropertyStateSnapshot prop : debtorPropertiesWithBuildings(state, debt.debtorPlayerId())) {
                    int buildingCount = prop.hotelCount() > 0 ? prop.hotelCount() : prop.houseCount();
                    String type = prop.hotelCount() > 0 ? "hotelli" : "talo";
                    String label = "🏠 Myy " + type + " (" + buildingCount + ") " + shortLabel(SpotType.valueOf(prop.propertyId()));
                    y = addButton(sx, y, sw, bh, gap, label,
                            new SellBuildingForDebtCommand(sessionId, activeId, debt.debtId(), prop.propertyId(), 1),
                            new int[]{80, 100, 20});
                }
            }
            if (debt.allowedActions().contains(DebtAction.DECLARE_BANKRUPTCY)) {
                y = addButton(sx, y, sw, bh, gap, "☠ Konkurssi",
                        new DeclareBankruptcyCommand(sessionId, activeId, debt.debtId()),
                        new int[]{160, 30, 30});
            }
        }

        if (phase == TurnPhase.GAME_OVER && state.winnerPlayerId() != null) {
            app.fill(220, 200, 60);
            app.textSize(14);
            app.textAlign(PApplet.CENTER, PApplet.TOP);
            String winnerName = playerName(state, state.winnerPlayerId());
            app.text("🏆 " + winnerName + " voitti!", sx + sw / 2f, y + 6);
            app.textAlign(PApplet.LEFT, PApplet.TOP);
            y += 30;
        }

        return y;
    }

    private int addButton(int sx, int y, int sw, int bh, int gap, String label,
                           SessionCommand cmd, int[] color) {
        int bx = sx + 6;
        int bw = sw - 12;
        boolean hover = app.mouseX >= bx && app.mouseX <= bx + bw
                && app.mouseY >= y && app.mouseY <= y + bh;

        app.fill(hover ? color[0] + 20 : color[0],
                hover ? color[1] + 20 : color[1],
                hover ? color[2] + 20 : color[2]);
        app.stroke(hover ? 255 : 80, hover ? 255 : 80, hover ? 255 : 80, hover ? 200 : 80);
        app.strokeWeight(1);
        app.rect(bx, y, bw, bh, 5);

        app.fill(230, 230, 230);
        app.textSize(11);
        app.textAlign(PApplet.CENTER, PApplet.CENTER);
        app.noStroke();
        app.text(label, bx + bw / 2f, y + bh / 2f);
        app.textAlign(PApplet.LEFT, PApplet.TOP);

        buttons.add(new ActionButton(bx, y, bw, bh, cmd));
        return y + bh + gap;
    }

    // -------------------------------------------------------------------------
    // Waiting screen
    // -------------------------------------------------------------------------

    private void drawWaiting(int bx, int by, int boardSize, int sidebarX) {
        app.fill(22, 30, 22);
        app.noStroke();
        app.rect(bx, by, boardSize, boardSize, 8);
        app.fill(120, 170, 120);
        app.textSize(16);
        app.textAlign(PApplet.CENTER, PApplet.CENTER);
        app.text("Odotetaan palvelinta…", bx + boardSize / 2f, by + boardSize / 2f);
        app.textSize(11);
        app.fill(80, 100, 80);
        app.text("sid: " + sessionId.substring(0, Math.min(12, sessionId.length())) + "…",
                bx + boardSize / 2f, by + boardSize / 2f + 28);
        app.textAlign(PApplet.LEFT, PApplet.TOP);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static int[] boardIndexToRowCol(int idx) {
        if (idx <= 10) return new int[]{10, 10 - idx};
        if (idx <= 19) return new int[]{20 - idx, 0};
        if (idx <= 30) return new int[]{0, idx - 20};
        return new int[]{idx - 30, 10};
    }

    private static int[] cellBgColor(StreetType type) {
        return switch (type) {
            case CORNER -> new int[]{28, 42, 28};
            default -> new int[]{13, 17, 23};
        };
    }

    private static int[] stripColor(StreetType type) {
        return switch (type) {
            case BROWN -> new int[]{107, 58, 42};
            case LIGHT_BLUE -> new int[]{79, 163, 199};
            case PURPLE -> new int[]{179, 71, 160};
            case ORANGE -> new int[]{224, 123, 0};
            case RED -> new int[]{204, 34, 34};
            case YELLOW -> new int[]{201, 168, 0};
            case GREEN -> new int[]{26, 122, 60};
            case DARK_BLUE -> new int[]{26, 63, 160};
            case RAILROAD -> new int[]{68, 68, 68};
            case UTILITY -> new int[]{102, 102, 102};
            case TAX -> new int[]{122, 26, 26};
            case COMMUNITY -> new int[]{26, 95, 160};
            case CHANCE -> new int[]{201, 122, 0};
            case CORNER -> new int[]{28, 42, 28};
        };
    }

    private static String shortLabel(SpotType spot) {
        return switch (spot) {
            case GO_SPOT -> "GO";
            case JAIL -> "Vanki";
            case FREE_PARKING -> "Vapaa";
            case GO_TO_JAIL -> "→Jail";
            case COMMUNITY1, COMMUNITY2, COMMUNITY3 -> "YK";
            case CHANCE1, CHANCE2, CHANCE3 -> "?";
            case TAX1, TAX2 -> "Vero";
            default -> spot.name().replaceAll("\\d+$", "");
        };
    }

    private static String phaseName(TurnPhase phase) {
        return switch (phase) {
            case WAITING_FOR_ROLL -> "Odottaa noppaa";
            case WAITING_FOR_END_TURN -> "Odottaa vuoron lopetusta";
            case WAITING_FOR_DECISION -> "Odottaa päätöstä";
            case WAITING_FOR_AUCTION -> "Huutokauppa";
            case RESOLVING_DEBT -> "Velkakriisi";
            case GAME_OVER -> "Peli päättyi";
            default -> phase.name();
        };
    }

    private static String playerName(SessionState state, String playerId) {
        if (playerId == null) return "?";
        return state.players().stream()
                .filter(p -> playerId.equals(p.playerId()))
                .map(PlayerSnapshot::name)
                .findFirst()
                .orElse(playerId);
    }

    private static Map<String, int[]> buildPlayerColors(SessionState state) {
        Map<String, int[]> map = new HashMap<>();
        for (SeatState seat : state.seats()) {
            String hex = seat.tokenColorHex();
            if (hex != null && hex.startsWith("#") && hex.length() >= 7) {
                try {
                    int rgb = Integer.parseUnsignedInt(hex.substring(1, 7), 16);
                    map.put(seat.playerId(), new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF});
                } catch (NumberFormatException ignored) {}
            }
            if (!map.containsKey(seat.playerId())) {
                map.put(seat.playerId(), new int[]{150, 150, 150});
            }
        }
        return map;
    }

    private static Map<String, String> buildPropertyOwners(SessionState state) {
        Map<String, String> map = new HashMap<>();
        for (PropertyStateSnapshot prop : state.properties()) {
            if (prop.ownerPlayerId() != null) {
                map.put(prop.propertyId(), prop.ownerPlayerId());
            }
        }
        return map;
    }

    private static Map<String, int[]> buildPropertyBuildings(SessionState state) {
        Map<String, int[]> map = new HashMap<>();
        for (PropertyStateSnapshot prop : state.properties()) {
            if (prop.houseCount() > 0 || prop.hotelCount() > 0) {
                map.put(prop.propertyId(), new int[]{prop.houseCount(), prop.hotelCount()});
            }
        }
        return map;
    }

    private static Map<Integer, List<PlayerSnapshot>> buildTokensOnSpot(SessionState state) {
        Map<Integer, List<PlayerSnapshot>> map = new HashMap<>();
        for (PlayerSnapshot p : state.players()) {
            if (!p.bankrupt() && !p.eliminated()) {
                map.computeIfAbsent(p.boardIndex(), k -> new ArrayList<>()).add(p);
            }
        }
        return map;
    }

    private static String extractPropertyId(fi.monopoly.domain.decision.PendingDecision dec) {
        if (dec == null || dec.payload() == null) return "";
        if (dec.payload() instanceof fi.monopoly.domain.decision.PropertyPurchaseDecisionPayload p) {
            return p.propertyId();
        }
        return "";
    }

    private int addManagementButtons(SessionState state, int sx, int y, int sw, int bh, int gap, String activeId) {
        for (PropertyStateSnapshot prop : state.properties()) {
            if (!activeId.equals(prop.ownerPlayerId())) continue;
            SpotType spotType = SpotType.valueOf(prop.propertyId());
            // Build: only street properties, not mortgaged
            if (!prop.mortgaged() && spotType.streetType.placeType == fi.monopoly.types.PlaceType.STREET) {
                String label = "🏠 Rakenna " + shortLabel(spotType);
                y = addButton(sx, y, sw, bh, gap, label,
                        new BuyBuildingRoundCommand(sessionId, activeId, prop.propertyId()),
                        new int[]{50, 80, 140});
            }
            // Mortgage toggle
            String mortgageLabel = prop.mortgaged()
                    ? "💰 Lunasta " + shortLabel(spotType)
                    : "📜 Kiinnitä " + shortLabel(spotType);
            int[] mortgageColor = prop.mortgaged() ? new int[]{80, 120, 40} : new int[]{100, 80, 20};
            y = addButton(sx, y, sw, bh, gap, mortgageLabel,
                    new ToggleMortgageCommand(sessionId, activeId, prop.propertyId()),
                    mortgageColor);
        }
        return y;
    }

    private static List<PropertyStateSnapshot> debtorUnmortgagedProperties(SessionState state, String debtorPlayerId) {
        return state.properties().stream()
                .filter(p -> debtorPlayerId.equals(p.ownerPlayerId()) && !p.mortgaged())
                .toList();
    }

    private static List<PropertyStateSnapshot> debtorPropertiesWithBuildings(SessionState state, String debtorPlayerId) {
        return state.properties().stream()
                .filter(p -> debtorPlayerId.equals(p.ownerPlayerId()) && (p.houseCount() > 0 || p.hotelCount() > 0))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    private record ActionButton(int x, int y, int w, int h, SessionCommand command) {
        boolean contains(int mx, int my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }
}
