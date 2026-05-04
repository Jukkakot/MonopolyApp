package fi.monopoly.presentation.game.desktop.assembly;

import fi.monopoly.components.Player;
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
    private final java.util.function.Function<Player, GameView> currentGameViewFactory;
    private final java.util.function.Function<Player, PlayerView> currentPlayerViewFactory;

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
    public boolean handleComputerTradeTurn(Player player) {
        return tradeController.handleComputerTradeTurn(player);
    }

    @Override
    public ComputerDecision tryInitiateComputerTrade(Player player) {
        return tradeController.tryInitiateComputerTrade(player);
    }

    @Override
    public GameView currentGameView(Player player) {
        return currentGameViewFactory.apply(player);
    }

    @Override
    public PlayerView currentPlayerView(Player player) {
        return currentPlayerViewFactory.apply(player);
    }
    public interface PopupHooks {
        boolean popupVisible();

        boolean resolveVisiblePopupFor(ComputerPlayerProfile profile);

        boolean acceptActivePopup();

        boolean declineActivePopup();
    }
}
