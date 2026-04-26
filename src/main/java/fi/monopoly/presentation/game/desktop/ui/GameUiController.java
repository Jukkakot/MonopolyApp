package fi.monopoly.presentation.game.desktop.ui;

import fi.monopoly.client.desktop.DesktopClientSettings;
import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.host.bot.BotTurnScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.util.List;
import java.util.Locale;

import static fi.monopoly.text.UiTexts.text;
import static processing.event.MouseEvent.CLICK;

@Slf4j
@RequiredArgsConstructor
public class GameUiController {
    private final MonopolyButton endRoundButton;
    private final MonopolyButton retryDebtButton;
    private final MonopolyButton declareBankruptcyButton;
    private final MonopolyButton debugGodModeButton;
    private final MonopolyButton pauseButton;
    private final MonopolyButton tradeButton;
    private final MonopolyButton saveButton;
    private final MonopolyButton loadButton;
    private final MonopolyButton botSpeedButton;
    private final MonopolyButton languageButton;
    private final GameUiSessionControls sessionControls;
    private final Hooks hooks;

    public void bindButtonActions() {
        endRoundButton.addListener(hooks::endRound);
        retryDebtButton.addListener(hooks::payDebt);
        declareBankruptcyButton.addListener(hooks::declareBankruptcy);
        debugGodModeButton.addListener(hooks::openGodModeMenu);
        pauseButton.addListener(sessionControls::togglePause);
        tradeButton.addListener(hooks::openTradeMenu);
        saveButton.addListener(sessionControls::saveSession);
        loadButton.addListener(sessionControls::loadSession);
        botSpeedButton.addListener(sessionControls::cycleBotSpeedMode);
        languageButton.addListener(this::cycleLanguage);
    }

    public void refreshLabels(boolean paused, BotTurnScheduler.SpeedMode botSpeedMode) {
        endRoundButton.setLabel(text("game.button.endRound"));
        retryDebtButton.setLabel(text("game.button.retryDebt"));
        declareBankruptcyButton.setLabel(text("game.button.bankrupt"));
        debugGodModeButton.setLabel(text("game.button.godMode"));
        pauseButton.setLabel(paused ? text("game.button.resume") : text("game.button.pause"));
        tradeButton.setLabel(text("game.button.trade"));
        saveButton.setLabel(text("game.button.save"));
        loadButton.setLabel(text("game.button.load"));
        botSpeedButton.setLabel(text("game.button.botSpeed", text(botSpeedMode.labelKey())));
        languageButton.setLabel(text("language.button.current", text("language.name.current")));
    }

    public void updatePersistentButtons(boolean gameOver) {
        if (gameOver) {
            debugGodModeButton.hide();
            pauseButton.hide();
            tradeButton.hide();
            saveButton.hide();
            loadButton.hide();
            botSpeedButton.hide();
            languageButton.show();
            return;
        }
        pauseButton.show();
        tradeButton.show();
        saveButton.show();
        loadButton.show();
        languageButton.show();
        if (!DesktopClientSettings.debugMode()) {
            debugGodModeButton.hide();
            botSpeedButton.hide();
            return;
        }
        debugGodModeButton.show();
        botSpeedButton.show();
    }

    public boolean handleEvent(Event event) {
        if (event instanceof KeyEvent keyEvent) {
            return handleKeyEvent(keyEvent);
        }
        if (event instanceof MouseEvent mouseEvent) {
            return handleMouseEvent(mouseEvent);
        }
        return false;
    }

    private boolean handleKeyEvent(KeyEvent keyEvent) {
        char key = Character.toLowerCase(keyEvent.getKey());
        if (hooks.gameOver()) {
            return false;
        }
        if (keyEvent.isControlDown() && key == 's') {
            sessionControls.saveSession();
            return true;
        }
        if (keyEvent.isControlDown() && key == 'l') {
            sessionControls.loadSession();
            return true;
        }
        if (key == 'p') {
            sessionControls.togglePause();
            return true;
        }
        if (key == 'b') {
            sessionControls.cycleBotSpeedMode();
            return true;
        }
        if (hooks.popupVisible()) {
            return false;
        }
        if (hooks.debtActive()) {
            if (key == MonopolyApp.SPACE || key == MonopolyApp.ENTER) {
                hooks.payDebt();
                return true;
            }
            return false;
        }
        if (key == 't') {
            hooks.openTradeMenu();
            return true;
        }
        if (hooks.canEndTurn() && (key == MonopolyApp.SPACE || key == MonopolyApp.ENTER)) {
            hooks.endRound();
            return true;
        }
        if (DesktopClientSettings.debugMode() && key == 'e') {
            log.debug("Ending round");
            hooks.finishAllAnimations();
            hooks.endRound();
            return true;
        }
        if (DesktopClientSettings.debugMode() && key == 'g') {
            hooks.openGodModeMenu();
            return true;
        }
        if (key == 'a') {
            hooks.toggleSkipAnimations();
            return true;
        }
        return false;
    }

    private boolean handleMouseEvent(MouseEvent mouseEvent) {
        if (mouseEvent.getAction() != CLICK || hooks.popupVisible()) {
            return false;
        }
        Spot hoveredSpot = hooks.hoveredSpot();
        return hoveredSpot != null && DesktopClientSettings.debugMode() && hooks.debugFlyToHoveredSpot(hoveredSpot);
    }

    private void cycleLanguage() {
        List<Locale> supportedLocales = sessionControls.supportedLocales();
        Locale currentLocale = sessionControls.currentLocale();
        int currentIndex = supportedLocales.indexOf(currentLocale);
        int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % supportedLocales.size();
        sessionControls.switchLanguage(supportedLocales.get(nextIndex));
    }

    public interface Hooks {
        boolean gameOver();

        boolean popupVisible();

        boolean debtActive();

        boolean canEndTurn();

        void openTradeMenu();

        void payDebt();

        void declareBankruptcy();

        void endRound();

        void openGodModeMenu();

        void finishAllAnimations();

        void toggleSkipAnimations();

        Spot hoveredSpot();

        boolean debugFlyToHoveredSpot(Spot hoveredSpot);

    }
}
