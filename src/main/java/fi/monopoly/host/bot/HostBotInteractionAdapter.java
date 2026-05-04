package fi.monopoly.host.bot;

import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerDecision;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.computer.GameView;
import fi.monopoly.components.computer.PlayerView;

/**
 * Host-bot adapter over desktop-local popup, trade, and projected-view collaborators.
 *
 * <p>This keeps the host-owned bot runtime from depending directly on the current desktop runtime
 * shell or presentation trade controller classes. Embedded local mode can still implement this
 * adapter with in-process desktop collaborators, but the host-side bot package only sees one
 * narrow bridge.</p>
 */
public interface HostBotInteractionAdapter {
    boolean popupVisible();

    boolean resolveVisiblePopupFor(ComputerPlayerProfile profile);

    boolean acceptActivePopup();

    boolean declineActivePopup();

    boolean handleComputerTradeTurn(Player player);

    ComputerDecision tryInitiateComputerTrade(Player player);

    GameView currentGameView(String playerId);

    PlayerView currentPlayerView(String playerId);
}
