package fi.monopoly.presentation.game;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.utils.LayoutMetrics;
import fi.monopoly.utils.UiTokens;

public final class GameControlLayout {
    private final MonopolyRuntime runtime;
    private final MonopolyButton endRoundButton;
    private final MonopolyButton retryDebtButton;
    private final MonopolyButton declareBankruptcyButton;
    private final MonopolyButton debugGodModeButton;
    private final MonopolyButton pauseButton;
    private final MonopolyButton tradeButton;
    private final MonopolyButton botSpeedButton;
    private final MonopolyButton languageButton;
    private final Dices dices;

    private float historyHeight;
    private float historyPanelY;
    private float reservedTop;
    private int lastSidebarLayoutHash = Integer.MIN_VALUE;

    public GameControlLayout(
            MonopolyRuntime runtime,
            MonopolyButton endRoundButton,
            MonopolyButton retryDebtButton,
            MonopolyButton declareBankruptcyButton,
            MonopolyButton debugGodModeButton,
            MonopolyButton pauseButton,
            MonopolyButton tradeButton,
            MonopolyButton botSpeedButton,
            MonopolyButton languageButton,
            Dices dices
    ) {
        this.runtime = runtime;
        this.endRoundButton = endRoundButton;
        this.retryDebtButton = retryDebtButton;
        this.declareBankruptcyButton = declareBankruptcyButton;
        this.debugGodModeButton = debugGodModeButton;
        this.pauseButton = pauseButton;
        this.tradeButton = tradeButton;
        this.botSpeedButton = botSpeedButton;
        this.languageButton = languageButton;
        this.dices = dices;
    }

    public LayoutMetrics updateFrameLayoutMetrics() {
        LayoutMetrics frameLayoutMetrics = LayoutMetrics.fromWindow(runtime.app().width, runtime.app().height);
        reservedTop = frameLayoutMetrics.sidebarReservedTop(MonopolyApp.DEBUG_MODE);
        float availableHistoryHeight = runtime.app().height - reservedTop - frameLayoutMetrics.sidebarHistoryBottomMargin() - UiTokens.sidebarHistoryTopMargin();
        historyHeight = Math.max(UiTokens.sidebarHistoryMinHeight(), Math.min(UiTokens.sidebarHistoryPreferredHeight(), availableHistoryHeight));
        historyPanelY = runtime.app().height - historyHeight - frameLayoutMetrics.sidebarHistoryBottomMargin();
        return frameLayoutMetrics;
    }

    public void updateSidebarControlPositions(LayoutMetrics layoutMetrics) {
        int layoutHash = java.util.Objects.hash(
                layoutMetrics.hasSidebarSpace(),
                Math.round(layoutMetrics.sidebarX()),
                Math.round(layoutMetrics.sidebarRight()),
                Math.round(layoutMetrics.sidebarPrimaryButtonY()),
                Math.round(layoutMetrics.sidebarDebugButtonRow1Y()),
                Math.round(historyHeight),
                Math.round(historyPanelY),
                MonopolyApp.DEBUG_MODE,
                pauseButton.getWidth(),
                tradeButton.getWidth(),
                botSpeedButton.getWidth(),
                languageButton.getWidth(),
                declareBankruptcyButton.getWidth()
        );
        if (layoutHash == lastSidebarLayoutHash) {
            dices.updateLayout(layoutMetrics);
            return;
        }
        lastSidebarLayoutHash = layoutHash;
        if (!layoutMetrics.hasSidebarSpace()) {
            layoutOverlayControls(layoutMetrics);
            dices.updateLayout(layoutMetrics);
            return;
        }

        float sidebarLeftX = layoutMetrics.sidebarX() + UiTokens.spacingMd();
        float sidebarRightAlignedX = layoutMetrics.sidebarRight() - UiTokens.spacingMd();
        float primaryButtonY = layoutMetrics.sidebarPrimaryButtonY();
        float debugRow1Y = layoutMetrics.sidebarDebugButtonRow1Y();
        float controlRow1Y = historyPanelY + historyHeight + UiTokens.spacingSm();
        boolean showBotSpeed = MonopolyApp.DEBUG_MODE;
        float controlRow2Y = controlRow1Y + pauseButton.getHeight() + UiTokens.spacingXs();
        float lowestControlBottom = (showBotSpeed ? controlRow2Y : controlRow1Y) + languageButton.getHeight();
        if (lowestControlBottom > runtime.app().height - UiTokens.spacingMd()) {
            layoutOverlayControls(layoutMetrics);
            dices.updateLayout(layoutMetrics);
            return;
        }

        endRoundButton.setPosition(sidebarLeftX, primaryButtonY);
        retryDebtButton.setPosition(sidebarLeftX, primaryButtonY);
        declareBankruptcyButton.setPosition(sidebarRightAlignedX - declareBankruptcyButton.getWidth(), primaryButtonY);
        debugGodModeButton.setPosition(sidebarLeftX, debugRow1Y);
        if (showBotSpeed) {
            pauseButton.setPosition(sidebarRightAlignedX - pauseButton.getWidth(), controlRow1Y);
            tradeButton.setPosition(pauseButton.getPosition()[0] - tradeButton.getWidth() - UiTokens.spacingXs(), controlRow1Y);
            languageButton.setPosition(sidebarRightAlignedX - languageButton.getWidth(), controlRow2Y);
            botSpeedButton.setPosition(languageButton.getPosition()[0] - botSpeedButton.getWidth() - UiTokens.spacingXs(), controlRow2Y);
        } else {
            languageButton.setPosition(sidebarRightAlignedX - languageButton.getWidth(), controlRow1Y);
            pauseButton.setPosition(languageButton.getPosition()[0] - pauseButton.getWidth() - UiTokens.spacingXs(), controlRow1Y);
            tradeButton.setPosition(pauseButton.getPosition()[0] - tradeButton.getWidth() - UiTokens.spacingXs(), controlRow1Y);
        }
        dices.updateLayout(layoutMetrics);
    }

    public float historyHeight(LayoutMetrics layoutMetrics) {
        return historyHeight > 0 ? historyHeight : Math.max(
                UiTokens.sidebarHistoryMinHeight(),
                Math.min(
                        UiTokens.sidebarHistoryPreferredHeight(),
                        runtime.app().height - reservedTop(layoutMetrics) - layoutMetrics.sidebarHistoryBottomMargin() - UiTokens.sidebarHistoryTopMargin()
                )
        );
    }

    public float historyPanelY(LayoutMetrics layoutMetrics) {
        return historyPanelY > 0 ? historyPanelY : runtime.app().height - historyHeight(layoutMetrics) - layoutMetrics.sidebarHistoryBottomMargin();
    }

    public float reservedTop(LayoutMetrics layoutMetrics) {
        return reservedTop > 0 ? reservedTop : layoutMetrics.sidebarReservedTop(MonopolyApp.DEBUG_MODE);
    }

    private void layoutOverlayControls(LayoutMetrics layoutMetrics) {
        float leftX = UiTokens.overlayMargin();
        float rightX = layoutMetrics.boardWidth() - UiTokens.overlayMargin();
        float overlayTopRowY = UiTokens.overlaySecondaryRow3Y();
        boolean showBotSpeed = MonopolyApp.DEBUG_MODE;
        float overlayBottomRowY = Math.min(
                overlayTopRowY + languageButton.getHeight() + UiTokens.spacingXs(),
                runtime.app().height - languageButton.getHeight() - UiTokens.spacingMd()
        );

        endRoundButton.setPosition(leftX, UiTokens.overlayPrimaryButtonY());
        retryDebtButton.setPosition(leftX, UiTokens.overlayPrimaryButtonY());
        declareBankruptcyButton.setPosition(rightX - declareBankruptcyButton.getWidth(), UiTokens.overlayPrimaryButtonY());
        debugGodModeButton.setPosition(leftX, UiTokens.overlaySecondaryRow1Y());
        if (showBotSpeed) {
            languageButton.setPosition(Math.max(leftX, rightX - languageButton.getWidth()), overlayBottomRowY);
            botSpeedButton.setPosition(
                    Math.max(leftX, languageButton.getPosition()[0] - botSpeedButton.getWidth() - UiTokens.spacingXs()),
                    overlayBottomRowY
            );
            pauseButton.setPosition(Math.max(leftX, rightX - pauseButton.getWidth()), overlayTopRowY);
            tradeButton.setPosition(
                    Math.max(leftX, pauseButton.getPosition()[0] - tradeButton.getWidth() - UiTokens.spacingXs()),
                    overlayTopRowY
            );
        } else {
            languageButton.setPosition(Math.max(leftX, rightX - languageButton.getWidth()), overlayTopRowY);
            pauseButton.setPosition(
                    Math.max(leftX, languageButton.getPosition()[0] - pauseButton.getWidth() - UiTokens.spacingXs()),
                    overlayTopRowY
            );
            tradeButton.setPosition(
                    Math.max(leftX, pauseButton.getPosition()[0] - tradeButton.getWidth() - UiTokens.spacingXs()),
                    overlayTopRowY
            );
        }
    }
}
