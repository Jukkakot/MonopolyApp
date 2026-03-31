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
        dynamicDangerReserve += threateningOpponentMonopolies(view, self) * config.buildReservePerOpponentMonopoly();
        if (!self.completedSets().isEmpty()) {
            dynamicDangerReserve += config.postMonopolyCashBuffer();
        }

        int rawReserve = Math.max(reserve, dynamicDangerReserve);
        int toleranceDiscount = (int) Math.round(Math.max(0, rawReserve - config.minCashReserve()) * config.mortgageTolerance());
        return Math.max(config.minCashReserve(), rawReserve - toleranceDiscount);
    }

    private static int threateningOpponentMonopolies(GameView view, PlayerView self) {
        return view.players().stream()
                .filter(player -> player.id() != self.id())
                .mapToInt(player -> player.completedSets().size())
                .sum();
    }
}
