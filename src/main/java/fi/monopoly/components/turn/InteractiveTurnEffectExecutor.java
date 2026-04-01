package fi.monopoly.components.turn;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Players;
import fi.monopoly.components.payment.PaymentHandler;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PlayerTarget;
import fi.monopoly.components.popup.PopupService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
public class InteractiveTurnEffectExecutor {
    private final PopupService popupService;
    private final PropertyAuctionResolver propertyAuctionResolver;

    public InteractiveTurnEffectExecutor(PopupService popupService) {
        this(popupService, null);
    }

    public InteractiveTurnEffectExecutor(PopupService popupService, Players players) {
        this.popupService = popupService;
        this.propertyAuctionResolver = new PropertyAuctionResolver(popupService, players);
    }

    public void execute(List<TurnEffect> effects, PaymentHandler paymentHandler, CallbackAction onComplete) {
        executeNext(effects, 0, paymentHandler, onComplete);
    }

    private void executeNext(List<TurnEffect> effects, int index, PaymentHandler paymentHandler, CallbackAction onComplete) {
        if (index >= effects.size()) {
            onComplete.doAction();
            return;
        }

        TurnEffect effect = effects.get(index);
        CallbackAction next = () -> executeNext(effects, index + 1, paymentHandler, onComplete);

        if (effect instanceof ShowMessageEffect showMessageEffect) {
            popupService.show(showMessageEffect.message(), next::doAction);
        } else if (effect instanceof AdjustPlayerMoneyEffect adjustPlayerMoneyEffect) {
            popupService.show(adjustPlayerMoneyEffect.message(), () -> {
                adjustPlayerMoneyEffect.player().addMoney(adjustPlayerMoneyEffect.amount());
                next.doAction();
            });
        } else if (effect instanceof OfferToBuyPropertyEffect offerToBuyPropertyEffect) {
            popupService.showPropertyOffer(offerToBuyPropertyEffect.property(), offerToBuyPropertyEffect.message(), () -> {
                boolean couldBuy = offerToBuyPropertyEffect.player().buyProperty(offerToBuyPropertyEffect.property());
                if (!couldBuy) {
                    popupService.show(text("property.buy.notEnough", offerToBuyPropertyEffect.property().getDisplayName()), () ->
                            propertyAuctionResolver.resolve(
                                    offerToBuyPropertyEffect.player(),
                                    offerToBuyPropertyEffect.property(),
                                    PropertyAuctionResolver.AuctionReason.PLAYER_COULD_NOT_PAY,
                                    next
                            )
                    );
                    return;
                }
                next.doAction();
            }, () -> propertyAuctionResolver.resolve(
                    offerToBuyPropertyEffect.player(),
                    offerToBuyPropertyEffect.property(),
                    PropertyAuctionResolver.AuctionReason.PLAYER_DECLINED,
                    next
            ));
        } else if (effect instanceof PayRentEffect payRentEffect) {
            popupService.show(payRentEffect.message(), () -> {
                paymentHandler.requestPayment(new PaymentRequest(
                        payRentEffect.fromPlayer(),
                        new PlayerTarget(payRentEffect.toPlayer()),
                        payRentEffect.amount(),
                        payRentEffect.message()
                ), next);
            });
        } else {
            throw new IllegalStateException("Unhandled interactive turn effect: " + effect.getClass().getSimpleName());
        }
    }
}
