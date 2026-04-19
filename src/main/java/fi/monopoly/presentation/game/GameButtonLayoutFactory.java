package fi.monopoly.presentation.game;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.utils.LayoutMetrics;
import fi.monopoly.utils.UiTokens;

/**
 * Applies the default sidebar layout and initial visibility for the desktop game buttons.
 */
public final class GameButtonLayoutFactory {
    public void apply(MonopolyRuntime runtime, Buttons buttons) {
        LayoutMetrics defaultLayout = LayoutMetrics.defaultWindow();

        buttons.endRoundButton().setPosition(defaultLayout.sidebarX() + UiTokens.sidebarValueX(), defaultLayout.sidebarPrimaryButtonY());
        buttons.endRoundButton().setSize(150, 44);
        buttons.endRoundButton().setAutoWidth(100, 28, 180);

        buttons.retryDebtButton().setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), defaultLayout.sidebarPrimaryButtonY());
        buttons.retryDebtButton().setSize(140, 40);
        buttons.retryDebtButton().setAutoWidth(140, 28, 220);

        buttons.declareBankruptcyButton().setPosition(defaultLayout.sidebarX() + UiTokens.sidebarValueX(), defaultLayout.sidebarPrimaryButtonY());
        buttons.declareBankruptcyButton().setSize(140, 40);
        buttons.declareBankruptcyButton().setAutoWidth(140, 28, 220);

        buttons.debugGodModeButton().setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), defaultLayout.sidebarDebugButtonRow1Y());
        buttons.debugGodModeButton().setSize(300, 36);
        buttons.debugGodModeButton().setAutoWidth(180, 28, 300);

        buttons.pauseButton().setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), runtime.app().height - 96);
        buttons.pauseButton().setSize(140, 36);
        buttons.pauseButton().setAutoWidth(120, 28, 180);
        buttons.pauseButton().setAllowedDuringComputerTurn(true);

        buttons.tradeButton().setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), runtime.app().height - 96);
        buttons.tradeButton().setSize(140, 36);
        buttons.tradeButton().setAutoWidth(120, 28, 220);

        buttons.saveButton().setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), runtime.app().height - 48);
        buttons.saveButton().setSize(120, 36);
        buttons.saveButton().setAutoWidth(100, 28, 180);
        buttons.saveButton().setAllowedDuringComputerTurn(true);

        buttons.loadButton().setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), runtime.app().height - 48);
        buttons.loadButton().setSize(120, 36);
        buttons.loadButton().setAutoWidth(100, 28, 180);
        buttons.loadButton().setAllowedDuringComputerTurn(true);

        buttons.botSpeedButton().setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), runtime.app().height - 48);
        buttons.botSpeedButton().setSize(140, 36);
        buttons.botSpeedButton().setAutoWidth(120, 28, 220);
        buttons.botSpeedButton().setAllowedDuringComputerTurn(true);

        buttons.languageButton().setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), runtime.app().height - 48);
        buttons.languageButton().setSize(220, 36);
        buttons.languageButton().setAutoWidth(180, 28, 280);
        buttons.languageButton().setAllowedDuringComputerTurn(true);

        buttons.endRoundButton().hide();
        buttons.retryDebtButton().hide();
        buttons.declareBankruptcyButton().hide();
        buttons.debugGodModeButton().hide();
        buttons.pauseButton().hide();
        buttons.tradeButton().hide();
        buttons.saveButton().hide();
        buttons.loadButton().hide();
        buttons.botSpeedButton().hide();
    }

    public record Buttons(
            MonopolyButton endRoundButton,
            MonopolyButton retryDebtButton,
            MonopolyButton declareBankruptcyButton,
            MonopolyButton debugGodModeButton,
            MonopolyButton pauseButton,
            MonopolyButton tradeButton,
            MonopolyButton saveButton,
            MonopolyButton loadButton,
            MonopolyButton botSpeedButton,
            MonopolyButton languageButton
    ) {
    }
}
