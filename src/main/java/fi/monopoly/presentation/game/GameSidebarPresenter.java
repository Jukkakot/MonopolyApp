package fi.monopoly.presentation.game;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.Player;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.utils.LayoutMetrics;
import fi.monopoly.utils.TextWrapUtils;
import fi.monopoly.utils.UiTokens;
import processing.core.PConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;

import static fi.monopoly.text.UiTexts.text;

public final class GameSidebarPresenter {
    private final MonopolyRuntime runtime;
    private final Map<HistoryEntryCacheKey, HistoryEntryLayout> historyLayoutCache = new HashMap<>();

    public GameSidebarPresenter(MonopolyRuntime runtime) {
        this.runtime = runtime;
    }

    public void drawSidebarPanel(LayoutMetrics layoutMetrics, SidebarState state, LongConsumer historyTimingRecorder) {
        float sidebarX = layoutMetrics.sidebarX();
        float sidebarWidth = layoutMetrics.sidebarWidth();
        if (!layoutMetrics.hasSidebarSpace()) {
            return;
        }
        MonopolyApp app = runtime.app();
        app.push();
        app.noStroke();
        app.fill(245, 239, 221);
        app.rect(sidebarX, 0, sidebarWidth, app.height);

        app.stroke(193, 178, 140);
        app.strokeWeight(2);
        app.line(sidebarX, 0, sidebarX, app.height);
        app.line(sidebarX + UiTokens.spacingMd(), layoutMetrics.sidebarHeaderHeight(), sidebarX + sidebarWidth - UiTokens.spacingMd(), layoutMetrics.sidebarHeaderHeight());
        if (state.debtState() != null) {
            app.line(sidebarX + UiTokens.spacingMd(), debtSectionBottom(layoutMetrics, state.debtState()), sidebarX + sidebarWidth - UiTokens.spacingMd(), debtSectionBottom(layoutMetrics, state.debtState()));
        }

        Player turnPlayer = state.turnPlayer();
        app.fill(46, 72, 63);
        app.textAlign(PConstants.LEFT);
        app.textFont(runtime.font30());
        app.text(text("sidebar.title"), sidebarX + UiTokens.spacingMd(), layoutMetrics.sidebarTitleY());

        app.fill(0);
        app.textFont(runtime.font20());
        app.text(text("sidebar.currentPlayer"), sidebarX + UiTokens.sidebarLabelX(), layoutMetrics.sidebarHeaderRow1Y());
        app.text(text("sidebar.turnPhase"), sidebarX + UiTokens.sidebarLabelX(), layoutMetrics.sidebarHeaderRow2Y());
        app.text(text("sidebar.currentSpot"), sidebarX + UiTokens.sidebarLabelX(), layoutMetrics.sidebarHeaderRow3Y());

        app.fill(46, 72, 63);
        float currentPlayerValueX = sidebarX + UiTokens.sidebarValueX();
        float currentPlayerValueY = layoutMetrics.sidebarHeaderRow1Y();
        app.text(turnPlayer != null ? turnPlayer.getName() : text("sidebar.none"), currentPlayerValueX, currentPlayerValueY);
        if (turnPlayer != null && turnPlayer.isComputerControlled()) {
            drawComputerBadge(app, currentPlayerValueX + app.textWidth(turnPlayer.getName()) + 12, currentPlayerValueY - 14);
        }
        app.text(state.currentTurnPhase(), sidebarX + UiTokens.sidebarValueX(), layoutMetrics.sidebarHeaderRow2Y());
        app.text(turnPlayer != null && turnPlayer.getSpot() != null ? turnPlayer.getSpot().getName() : text("sidebar.none"), sidebarX + UiTokens.sidebarValueX(), layoutMetrics.sidebarHeaderRow3Y());
        drawPersistenceNotice(app, layoutMetrics, state.persistenceNotice());
        drawPopupHistoryPanel(app, layoutMetrics, state, historyTimingRecorder);
        app.pop();
    }

    public void drawDebtState(LayoutMetrics layoutMetrics, DebtState debtState) {
        if (debtState == null) {
            return;
        }
        MonopolyApp app = runtime.app();
        app.push();
        app.fill(46, 72, 63);
        app.textFont(runtime.font20());
        app.text(text("sidebar.section.debt"), layoutMetrics.sidebarX() + UiTokens.spacingMd(), layoutMetrics.debtSectionTitleY());

        app.fill(0);
        app.textFont(runtime.font20());
        app.textAlign(PConstants.LEFT);
        app.text(buildDebtSidebarText(debtState.paymentRequest()), layoutMetrics.sidebarX() + UiTokens.spacingMd(), layoutMetrics.debtTextY());
        app.pop();
    }

    public float contentTop(LayoutMetrics layoutMetrics, SidebarState state) {
        if (state.debtState() != null) {
            return debtSectionBottom(layoutMetrics, state.debtState()) + 20;
        }
        float availableTop = state.historyPanelY() - UiTokens.sidebarHistoryTopMargin();
        return Math.max(UiTokens.sidebarMinContentTop(), Math.min(state.reservedTop(), availableTop));
    }

    public int debtSectionBottom(LayoutMetrics layoutMetrics, DebtState debtState) {
        if (debtState == null) {
            return Math.round(layoutMetrics.debtTextY());
        }
        int lineCount = buildDebtSidebarText(debtState.paymentRequest()).split("\\R").length;
        return Math.round(layoutMetrics.debtTextY() + lineCount * UiTokens.sidebarLineHeight());
    }

    private void drawComputerBadge(MonopolyApp app, float x, float y) {
        app.pushStyle();
        app.rectMode(PConstants.CORNER);
        app.noStroke();
        app.fill(46, 72, 63);
        app.rect(x, y, 42, 18, 8);
        app.fill(255);
        app.textFont(runtime.font10());
        app.textAlign(PConstants.CENTER, PConstants.TOP);
        app.text("BOT", x + 21, y + 4);
        app.popStyle();
    }

    private void drawPersistenceNotice(MonopolyApp app, LayoutMetrics layoutMetrics, String persistenceNotice) {
        if (persistenceNotice == null || persistenceNotice.isBlank()) {
            return;
        }
        app.pushStyle();
        app.fill(94, 102, 90);
        app.textFont(runtime.font10());
        app.textAlign(PConstants.LEFT, PConstants.TOP);
        app.text(
                persistenceNotice,
                layoutMetrics.sidebarX() + UiTokens.spacingMd(),
                layoutMetrics.sidebarHeaderHeight() + 8,
                layoutMetrics.sidebarWidth() - UiTokens.spacingMd() * 2,
                18
        );
        app.popStyle();
    }

    private void drawPopupHistoryPanel(MonopolyApp app, LayoutMetrics layoutMetrics, SidebarState state, LongConsumer historyTimingRecorder) {
        long historyStart = System.nanoTime();
        float panelX = layoutMetrics.sidebarX() + UiTokens.spacingMd();
        float panelW = layoutMetrics.sidebarWidth() - UiTokens.spacingMd() * 2;

        app.noStroke();
        app.fill(255, 249, 233);
        app.rect(panelX, state.historyPanelY(), panelW, state.historyHeight(), 16);

        app.stroke(193, 178, 140);
        app.strokeWeight(2);
        app.line(panelX, state.historyPanelY() + UiTokens.sidebarHistoryHeaderHeight(), panelX + panelW, state.historyPanelY() + UiTokens.sidebarHistoryHeaderHeight());

        app.fill(46, 72, 63);
        app.textFont(runtime.font20());
        app.text(text("sidebar.section.history"), panelX + UiTokens.sidebarHistoryTextInset(), state.historyPanelY() + 24);

        app.fill(0);
        app.textFont(runtime.font20());
        app.textAlign(PConstants.LEFT, PConstants.TOP);

        if (state.recentMessages().isEmpty()) {
            app.text(
                    text("sidebar.history.empty"),
                    panelX + UiTokens.sidebarHistoryTextInset(),
                    state.historyPanelY() + 56,
                    panelW - UiTokens.sidebarHistoryTextInset() * 2,
                    state.historyHeight() - 48
            );
            return;
        }

        float maxTextWidth = panelW - UiTokens.sidebarHistoryTextInset() * 2;
        float currentBottomY = state.historyPanelY() + state.historyHeight() - 20;
        for (String message : state.recentMessages()) {
            HistoryEntryLayout layout = buildHistoryEntryLayout(app, message, maxTextWidth, state.players());
            if (layout == null) {
                continue;
            }
            float nextTopY = currentBottomY - layout.height();
            if (nextTopY < state.historyPanelY() + UiTokens.sidebarHistoryHeaderHeight() + 8) {
                break;
            }
            drawHistoryEntry(app, layout, panelX + UiTokens.sidebarHistoryTextInset(), nextTopY);
            currentBottomY = nextTopY - 8;
        }
        historyTimingRecorder.accept(System.nanoTime() - historyStart);
    }

    private HistoryEntryLayout buildHistoryEntryLayout(MonopolyApp app, String message, float maxTextWidth, List<Player> players) {
        String condensedMessage = condenseHistoryMessage(message);
        if (condensedMessage.isEmpty()) {
            return null;
        }
        HistoryEntryCacheKey cacheKey = new HistoryEntryCacheKey(condensedMessage, Math.round(maxTextWidth));
        HistoryEntryLayout cachedLayout = historyLayoutCache.get(cacheKey);
        if (cachedLayout != null) {
            return cachedLayout;
        }
        int separatorIndex = condensedMessage.indexOf(": ");
        if (separatorIndex <= 0) {
            List<String> lines = TextWrapUtils.wrapText(app.g, "- " + condensedMessage, maxTextWidth, "history.generic");
            HistoryEntryLayout layout = new HistoryEntryLayout(null, null, lines, lines.size() * 22f);
            historyLayoutCache.put(cacheKey, layout);
            return layout;
        }

        String playerName = condensedMessage.substring(0, separatorIndex).trim();
        String body = condensedMessage.substring(separatorIndex + 2).trim();
        Player messagePlayer = findPlayerByName(players, playerName);
        if (messagePlayer == null) {
            List<String> lines = TextWrapUtils.wrapText(app.g, "- " + condensedMessage, maxTextWidth, "history.generic");
            HistoryEntryLayout layout = new HistoryEntryLayout(null, null, lines, lines.size() * 22f);
            historyLayoutCache.put(cacheKey, layout);
            return layout;
        }

        String prefix = "- " + playerName + ":";
        float prefixWidth = app.textWidth(prefix + " ");
        List<String> wrappedBodyLines = TextWrapUtils.wrapText(app.g, body, Math.max(40, maxTextWidth - prefixWidth), "history.player");
        if (wrappedBodyLines.isEmpty()) {
            wrappedBodyLines = List.of("");
        }
        HistoryEntryLayout layout = new HistoryEntryLayout(messagePlayer, prefix, wrappedBodyLines, wrappedBodyLines.size() * 22f);
        historyLayoutCache.put(cacheKey, layout);
        return layout;
    }

    private void drawHistoryEntry(MonopolyApp app, HistoryEntryLayout layout, float startX, float startY) {
        if (layout.player() == null || layout.prefix() == null) {
            drawWrappedHistoryLines(app, layout.lines(), startX, startY);
            return;
        }
        float prefixWidth = app.textWidth(layout.prefix() + " ");
        app.fill(colorComponent(layout.player().getColor().getRed()),
                colorComponent(layout.player().getColor().getGreen()),
                colorComponent(layout.player().getColor().getBlue()));
        app.text(layout.prefix(), startX, startY);

        app.fill(0);
        app.text(layout.lines().get(0), startX + prefixWidth, startY);
        float currentY = startY;
        for (int i = 1; i < layout.lines().size(); i++) {
            currentY += 22;
            app.text(layout.lines().get(i), startX + 18, currentY);
        }
    }

    private void drawWrappedHistoryLines(MonopolyApp app, List<String> wrappedLines, float startX, float startY) {
        float currentY = startY;
        for (String line : wrappedLines) {
            app.text(line, startX, currentY);
            currentY += 22;
        }
    }

    private String condenseHistoryMessage(String message) {
        return message.replaceAll("\\R+", " / ").replaceAll("\\s{2,}", " ").trim();
    }

    private Player findPlayerByName(List<Player> players, String playerName) {
        if (players == null || playerName == null || playerName.isBlank()) {
            return null;
        }
        for (Player player : players) {
            if (player.getName().equals(playerName)) {
                return player;
            }
        }
        return null;
    }

    private int colorComponent(double component) {
        return (int) Math.round(component * 255);
    }

    private String buildDebtSidebarText(PaymentRequest request) {
        return text(
                "sidebar.debt.summary",
                request.debtor().getName(),
                request.amount(),
                request.target().getDisplayName(),
                request.debtor().getMoneyAmount(),
                request.debtor().getTotalLiquidationValue()
        );
    }

    public record SidebarState(
            Player turnPlayer,
            String currentTurnPhase,
            List<Player> players,
            List<String> recentMessages,
            DebtState debtState,
            String persistenceNotice,
            float historyPanelY,
            float historyHeight,
            float reservedTop
    ) {
    }

    private record HistoryEntryLayout(
            Player player,
            String prefix,
            List<String> lines,
            float height
    ) {
    }

    private record HistoryEntryCacheKey(
            String message,
            int width
    ) {
    }
}
