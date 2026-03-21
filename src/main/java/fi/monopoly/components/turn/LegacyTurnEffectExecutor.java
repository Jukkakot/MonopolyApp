package fi.monopoly.components.turn;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.popup.PopupService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class LegacyTurnEffectExecutor {
    private final PopupService popupService;

    public LegacyTurnEffectExecutor(PopupService popupService) {
        this.popupService = popupService;
    }

    public void execute(List<TurnEffect> effects, CallbackAction onComplete) {
        executeNext(effects, 0, onComplete);
    }

    private void executeNext(List<TurnEffect> effects, int index, CallbackAction onComplete) {
        if (index >= effects.size()) {
            onComplete.doAction();
            return;
        }

        TurnEffect effect = effects.get(index);
        CallbackAction next = () -> executeNext(effects, index + 1, onComplete);

        if (effect instanceof ShowMessageEffect showMessageEffect) {
            popupService.show(showMessageEffect.message(), next::doAction);
        } else if (effect instanceof AdjustPlayerMoneyEffect adjustPlayerMoneyEffect) {
            popupService.show(adjustPlayerMoneyEffect.message(), () -> {
                adjustPlayerMoneyEffect.player().addMoney(adjustPlayerMoneyEffect.amount());
                next.doAction();
            });
        } else if (effect instanceof OfferToBuyPropertyEffect offerToBuyPropertyEffect) {
            popupService.show(offerToBuyPropertyEffect.message(), () -> {
                boolean couldBuy = offerToBuyPropertyEffect.player().buyProperty(offerToBuyPropertyEffect.property());
                if (!couldBuy) {
                    popupService.show("You don't have enough money to buy " + offerToBuyPropertyEffect.property().getDisplayName(), next::doAction);
                    return;
                }
                next.doAction();
            }, next::doAction);
        } else if (effect instanceof PayRentEffect payRentEffect) {
            popupService.show(payRentEffect.message(), () -> {
                boolean success = payRentEffect.toPlayer().giveMoney(payRentEffect.fromPlayer(), payRentEffect.amount());
                if (!success) {
                    log.warn("{} did not have enough money to pay M{} rent", payRentEffect.fromPlayer().getName(), payRentEffect.amount());
                }
                next.doAction();
            });
        } else {
            throw new IllegalStateException("Unhandled legacy turn effect: " + effect.getClass().getSimpleName());
        }
    }
}
