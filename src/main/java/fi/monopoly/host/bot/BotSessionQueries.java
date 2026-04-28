package fi.monopoly.host.bot;

import fi.monopoly.components.Player;
import fi.monopoly.domain.session.SessionState;

/**
 * Query port used by bot turn logic to resolve session participants.
 *
 * <p>Defined in the host layer so bot code does not depend on presentation-layer
 * query helpers. The presentation assembly implements this via {@code GameSessionQueries}.</p>
 */
public interface BotSessionQueries {
    Player findPlayerById(String playerId);

    String resolveTradeActorId(SessionState sessionState);
}
