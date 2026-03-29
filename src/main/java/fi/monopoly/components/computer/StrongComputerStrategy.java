package fi.monopoly.components.computer;

final class StrongComputerStrategy implements ComputerTurnStrategy {
    private final StrongPropertyBuyEvaluator propertyBuyEvaluator = new StrongPropertyBuyEvaluator(StrongBotConfig.defaults());
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
        return fallbackStrategy.takeStep(context);
    }
}
