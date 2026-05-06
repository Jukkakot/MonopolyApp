package fi.monopoly.support;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.GameSession;
import fi.monopoly.components.Players;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.dices.Dices;

public final class TestGameSessions {
    private TestGameSessions() {
    }

    public static GameSession install(MonopolyRuntime runtime, Players players, Dices dices, Animations animations) {
        GameSession session = new GameSession(players, dices, animations);
        runtime.setGameSession(session);
        return session;
    }

    public static Players players(MonopolyRuntime runtime) {
        return runtime.gameSession().players();
    }

    public static Dices dices(MonopolyRuntime runtime) {
        return runtime.gameSession().dices();
    }

    public static Animations animations(MonopolyRuntime runtime) {
        return runtime.gameSession().animations();
    }
}
