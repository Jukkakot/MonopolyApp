package fi.monopoly.presentation.game.desktop.assembly;

import fi.monopoly.components.computer.ComputerDecision;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.computer.GameView;
import fi.monopoly.components.computer.PlayerView;
import fi.monopoly.host.bot.HostBotInteractionAdapter;
import fi.monopoly.presentation.session.trade.TradeController;
import lombok.RequiredArgsConstructor;

/**
 * Desktop-local implementation of the host bot interaction bridge.
 *
 * <p>This adapts popup handling, trade automation, and projected view access from the embedded
 * desktop runtime into the narrow host-bot port used by the embedded local host loop.</p>
 */
@RequiredArgsConstructor
public final class DesktopHostBotInteractionAdapter implements HostBotInteractionAdapter {
    private final PopupHooks popupHooks;
    private final TradeController tradeController;
    private final java.util.function.Function<String, GameView> currentGameViewFactory;
    private final java.util.function.Function<String, PlayerView> currentPlayerViewFactory;

    @Override
    public boolean popupVisible() {
        return popupHooks.popupVisible();
    }

    @Override
    public boolean resolveVisiblePopupFor(ComputerPlayerProfile profile) {
        return popupHooks.resolveVisiblePopupFor(profile);
    }

    @Override
    public boolean acceptActivePopup() {
        return popupHooks.acceptActivePopup();
    }

    @Override
    public boolean declineActivePopup() {
        return popupHooks.declineActivePopup();
    }

    @Override
    public boolean handleComputerTradeTurn(String actorId, ComputerPlayerProfile profile) {
        return tradeController.handleComputerTradeTurn(actorId, profile);
    }

    @Override
    public ComputerDecision tryInitiateComputerTrade(String proposerId, ComputerPlayerProfile profile) {
        return tradeController.tryInitiateComputerTrade(proposerId, profile);
    }

    @Override
    public GameView currentGameView(String playerId) {
        return currentGameViewFactory.apply(playerId);
    }

    @Override
    public PlayerView currentPlayerView(String playerId) {
        return currentPlayerViewFactory.apply(playerId);
    }
    public interface PopupHooks {
        boolean popupVisible();

        boolean resolveVisiblePopupFor(ComputerPlayerProfile profile);

        boolean acceptActivePopup();

        boolean declineActivePopup();
    }
}
