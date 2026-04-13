package fi.monopoly.presentation.game;

import fi.monopoly.MonopolyApp;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.spots.Spot;
import lombok.extern.slf4j.Slf4j;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.util.List;
import java.util.Locale;

import static fi.monopoly.text.UiTexts.text;
import static processing.event.MouseEvent.CLICK;

@Slf4j
public class GameUiController {
    private final MonopolyButton endRoundButton;
    private final MonopolyButton retryDebtButton;
    private final MonopolyButton declareBankruptcyButton;
    private final MonopolyButton debugGodModeButton;
    private final MonopolyButton pauseButton;
    private final MonopolyButton tradeButton;
    private final MonopolyButton botSpeedButton;
    private final MonopolyButton languageButton;
    private final List<Locale> supportedLocales;
    private final Hooks hooks;

    public GameUiController(
            MonopolyButton endRoundButton,
            MonopolyButton retryDebtButton,
            MonopolyButton declareBankruptcyButton,
            MonopolyButton debugGodModeButton,
            MonopolyButton pauseButton,
            MonopolyButton tradeButton,
            MonopolyButton botSpeedButton,
            MonopolyButton languageButton,
            List<Locale> supportedLocales,
            Hooks hooks
    ) {
        this.endRoundButton = endRoundButton;
        this.retryDebtButton = retryDebtButton;
        this.declareBankruptcyButton = declareBankruptcyButton;
        this.debugGodModeButton = debugGodModeButton;
        this.pauseButton = pauseButton;
        this.tradeButton = tradeButton;
        this.botSpeedButton = botSpeedButton;
        this.languageButton = languageButton;
        this.supportedLocales = supportedLocales;
        this.hooks = hooks;
    }

    public void bindButtonActions() {
        endRoundButton.addListener(() -> hooks.endRound());
        retryDebtButton.addListener(() -> hooks.payDebt());
        declareBankruptcyButton.addListener(() -> hooks.declareBankruptcy());
        debugGodModeButton.addListener(() -> hooks.openGodModeMenu());
        pauseButton.addListener(() -> hooks.togglePause());
        tradeButton.addListener(() -> hooks.openTradeMenu());
        botSpeedButton.addListener(() -> hooks.cycleBotSpeedMode());
        languageButton.addListener(this::cycleLanguage);
    }

    public void refreshLabels(boolean paused, BotTurnScheduler.SpeedMode botSpeedMode) {
        endRoundButton.setLabel(text("game.button.endRound"));
        retryDebtButton.setLabel(text("game.button.retryDebt"));
        declareBankruptcyButton.setLabel(text("game.button.bankrupt"));
        debugGodModeButton.setLabel(text("game.button.godMode"));
        pauseButton.setLabel(paused ? text("game.button.resume") : text("game.button.pause"));
        tradeButton.setLabel(text("game.button.trade"));
        botSpeedButton.setLabel(text("game.button.botSpeed", text(botSpeedMode.labelKey())));
        languageButton.setLabel(text("language.button.current", text("language.name.current")));
    }

    public void updatePersistentButtons(boolean gameOver) {
        if (gameOver) {
            debugGodModeButton.hide();
            pauseButton.hide();
            tradeButton.hide();
            botSpeedButton.hide();
            languageButton.show();
            return;
        }
        pauseButton.show();
        tradeButton.show();
        languageButton.show();
        if (!MonopolyApp.DEBUG_MODE) {
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
        if (key == 'p') {
            hooks.togglePause();
            return true;
        }
        if (key == 'b') {
            hooks.cycleBotSpeedMode();
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
        if (MonopolyApp.DEBUG_MODE && key == 'e') {
            log.debug("Ending round");
            hooks.finishAllAnimations();
            hooks.endRound();
            return true;
        }
        if (MonopolyApp.DEBUG_MODE && key == 'g') {
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
        return hoveredSpot != null && MonopolyApp.DEBUG_MODE && hooks.debugFlyToHoveredSpot(hoveredSpot);
    }

    private void cycleLanguage() {
        Locale currentLocale = hooks.currentLocale();
        int currentIndex = supportedLocales.indexOf(currentLocale);
        int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % supportedLocales.size();
        hooks.switchLanguage(supportedLocales.get(nextIndex));
    }

    public interface Hooks {
        boolean gameOver();

        boolean popupVisible();

        boolean debtActive();

        boolean canEndTurn();

        void togglePause();

        void cycleBotSpeedMode();

        void openTradeMenu();

        void payDebt();

        void declareBankruptcy();

        void endRound();

        void openGodModeMenu();

        void finishAllAnimations();

        void toggleSkipAnimations();

        Spot hoveredSpot();

        boolean debugFlyToHoveredSpot(Spot hoveredSpot);

        Locale currentLocale();

        void switchLanguage(Locale locale);
    }
}
