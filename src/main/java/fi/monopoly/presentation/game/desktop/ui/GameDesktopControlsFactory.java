package fi.monopoly.presentation.game.desktop.ui;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import java.util.List;

/**
 * Creates and lays out the persistent desktop game buttons as one reusable bundle.
 */
public final class GameDesktopControlsFactory {

    public GameDesktopControls create(MonopolyRuntime runtime) {
        MonopolyButton endRoundButton = new MonopolyButton(runtime, "endRound");
        MonopolyButton retryDebtButton = new MonopolyButton(runtime, "retryDebt");
        MonopolyButton declareBankruptcyButton = new MonopolyButton(runtime, "declareBankruptcy");
        MonopolyButton debugGodModeButton = new MonopolyButton(runtime, "debugGodMode");
        MonopolyButton pauseButton = new MonopolyButton(runtime, "pause");
        MonopolyButton tradeButton = new MonopolyButton(runtime, "trade");
        MonopolyButton saveButton = new MonopolyButton(runtime, "save");
        MonopolyButton loadButton = new MonopolyButton(runtime, "load");
        MonopolyButton botSpeedButton = new MonopolyButton(runtime, "botSpeed");
        MonopolyButton languageButton = new MonopolyButton(runtime, "language");

        GameButtonLayoutFactory.Buttons buttons = new GameButtonLayoutFactory.Buttons(
                endRoundButton,
                retryDebtButton,
                declareBankruptcyButton,
                debugGodModeButton,
                pauseButton,
                tradeButton,
                saveButton,
                loadButton,
                botSpeedButton,
                languageButton
        );
        new GameButtonLayoutFactory().apply(runtime, buttons);
        return new GameDesktopControls(
                buttons,
                List.of(
                        endRoundButton,
                        retryDebtButton,
                        declareBankruptcyButton,
                        debugGodModeButton,
                        pauseButton,
                        tradeButton,
                        saveButton,
                        loadButton,
                        botSpeedButton,
                        languageButton
                )
        );
    }

    public record GameDesktopControls(
            GameButtonLayoutFactory.Buttons buttons,
            List<MonopolyButton> allButtons
    ) {
    }
}
