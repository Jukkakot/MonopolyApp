package fi.monopoly.components.computer;

final class StrongReservePolicy {
    private StrongReservePolicy() {
    }

    static int requiredReserve(StrongBotConfig config, GameView view, PlayerView self) {
        int reserve = self.boardDangerScore() >= config.dangerCashReserve() || view.unownedPropertyCount() <= 10
                ? config.dangerCashReserve()
                : config.minCashReserve();

        int dynamicDangerReserve = config.minCashReserve();
        if (self.boardDangerScore() > config.minCashReserve()) {
            dynamicDangerReserve += (int) Math.round((self.boardDangerScore() - config.minCashReserve()) * 0.6);
        }
        if (view.unownedPropertyCount() <= 10) {
            dynamicDangerReserve += 100;
        }
        if (view.unownedPropertyCount() <= 5) {
            dynamicDangerReserve += 100;
        }

        return Math.max(reserve, dynamicDangerReserve);
    }
}
