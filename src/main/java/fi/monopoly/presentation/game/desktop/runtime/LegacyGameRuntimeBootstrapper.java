package fi.monopoly.presentation.game.desktop.runtime;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.Players;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.infrastructure.persistence.session.LegacySessionRuntimeRestorer;
import fi.monopoly.infrastructure.persistence.session.RestoredLegacySessionRuntime;

/**
 * Creates the live desktop runtime objects for either a fresh game or a restored session.
 *
 * <p>This keeps the legacy board/player bootstrap path out of {@code Game} so the composition root
 * no longer needs to know whether runtime objects were freshly created or reconstructed from an
 * authoritative session snapshot.</p>
 */
public final class LegacyGameRuntimeBootstrapper {
    private final LegacySessionRuntimeRestorer legacySessionRuntimeRestorer;

    public LegacyGameRuntimeBootstrapper() {
        this(new LegacySessionRuntimeRestorer());
    }

    LegacyGameRuntimeBootstrapper(LegacySessionRuntimeRestorer legacySessionRuntimeRestorer) {
        this.legacySessionRuntimeRestorer = legacySessionRuntimeRestorer;
    }

    public LegacyGameRuntimeState bootstrap(MonopolyRuntime runtime, SessionState restoredSessionState) {
        if (restoredSessionState == null) {
            PropertyFactory.resetState();
            JailSpot.jailTimeLeftMap.clear();
            return new LegacyGameRuntimeState(new Board(runtime), new Players(runtime), false);
        }
        RestoredLegacySessionRuntime restoredRuntime = legacySessionRuntimeRestorer.restore(runtime, restoredSessionState);
        return new LegacyGameRuntimeState(restoredRuntime.board(), restoredRuntime.players(), true);
    }

    public record LegacyGameRuntimeState(Board board, Players players, boolean restoredSession) {
    }
}
