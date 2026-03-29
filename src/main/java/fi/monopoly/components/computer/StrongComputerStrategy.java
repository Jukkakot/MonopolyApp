package fi.monopoly.components.computer;

final class StrongComputerStrategy implements ComputerTurnStrategy {
    private final StrongBotConfig config = StrongBotConfig.defaults();
    private final StrongPropertyBuyEvaluator propertyBuyEvaluator = new StrongPropertyBuyEvaluator(config);
    private final StrongBuildingEvaluator buildingEvaluator = new StrongBuildingEvaluator(config);
    private final StrongDebtResolver debtResolver = new StrongDebtResolver();
    private final SmokeTestComputerStrategy fallbackStrategy = new SmokeTestComputerStrategy();

    @Override
    public boolean takeStep(ComputerTurnContext context) {
        GameView view = context.gameView();
        PlayerView self = context.currentPlayerView();
        PopupView popup = view.popup();
        if (popup != null && popup.offeredProperty() != null) {
            if (propertyBuyEvaluator.shouldBuy(view, self, popup.offeredProperty())) {
                return context.acceptActivePopup();
            }
            return context.declineActivePopup();
        }
        if (view.debt() != null) {
            return debtResolver.resolve(context, view, self);
        }
        if (view.visibleActions().endTurnVisible()) {
            PropertyView buildTarget = buildingEvaluator.selectBuildTarget(view, self);
            if (buildTarget != null && context.buyBuildingRound(buildTarget.spotType())) {
                return true;
            }
        }
        return fallbackStrategy.takeStep(context);
    }
}
