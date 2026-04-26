package fi.monopoly.components.turn;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.GameState;
import fi.monopoly.components.Players;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PlayerTarget;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.domain.session.TurnContinuationAction;
import fi.monopoly.domain.session.TurnContinuationState;
import fi.monopoly.domain.session.TurnContinuationType;
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

    public void execute(List<TurnEffect> effects, GameState gameState, CallbackAction onComplete) {
        executeNext(effects, 0, gameState, onComplete);
    }

    private void executeNext(List<TurnEffect> effects, int index, GameState gameState, CallbackAction onComplete) {
        if (index >= effects.size()) {
            onComplete.doAction();
            return;
        }

        TurnEffect effect = effects.get(index);
        CallbackAction next = () -> executeNext(effects, index + 1, gameState, onComplete);

        if (effect instanceof ShowMessageEffect showMessageEffect) {
            popupService.show(showMessageEffect.message(), next::doAction);
        } else if (effect instanceof AdjustPlayerMoneyEffect adjustPlayerMoneyEffect) {
            popupService.show(adjustPlayerMoneyEffect.message(), () -> {
                adjustPlayerMoneyEffect.player().addMoney(adjustPlayerMoneyEffect.amount());
                next.doAction();
            });
        } else if (effect instanceof OfferToBuyPropertyEffect offerToBuyPropertyEffect) {
            if (gameState.getPropertyPurchaseFlow() == null) {
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
                return;
            }
            gameState.getPropertyPurchaseFlow().begin(
                    offerToBuyPropertyEffect.player(),
                    offerToBuyPropertyEffect.property(),
                    offerToBuyPropertyEffect.message(),
                    propertyPurchaseContinuation(offerToBuyPropertyEffect, index, effects)
            );
        } else if (effect instanceof PayRentEffect payRentEffect) {
            popupService.show(payRentEffect.message(), () -> gameState.getPaymentHandler().requestPayment(
                    new PaymentRequest(
                            payRentEffect.fromPlayer(),
                            new PlayerTarget(payRentEffect.toPlayer()),
                            payRentEffect.amount(),
                            payRentEffect.message()
                    ),
                    rentContinuation(payRentEffect, index, effects),
                    next
            ));
        } else {
            throw new IllegalStateException("Unhandled interactive turn effect: " + effect.getClass().getSimpleName());
        }
    }

    private TurnContinuationState propertyPurchaseContinuation(
            OfferToBuyPropertyEffect effect,
            int index,
            List<TurnEffect> effects
    ) {
        return new TurnContinuationState(
                "turn-continuation:purchase:" + effect.player().getId() + ":" + effect.property().getSpotType().name() + ":" + index,
                "player-" + effect.player().getId(),
                TurnContinuationType.RESUME_TURN_FOLLOW_UP,
                TurnContinuationAction.APPLY_TURN_FOLLOW_UP,
                effect.property().getSpotType().name(),
                "resume-turn-follow-up"
        );
    }

    private TurnContinuationState rentContinuation(
            PayRentEffect effect,
            int index,
            List<TurnEffect> effects
    ) {
        return new TurnContinuationState(
                "turn-continuation:rent:" + effect.fromPlayer().getId() + ":" + effect.toPlayer().getId() + ":" + index,
                "player-" + effect.fromPlayer().getId(),
                TurnContinuationType.RESUME_AFTER_DEBT,
                TurnContinuationAction.APPLY_TURN_FOLLOW_UP,
                null,
                "resume-turn-follow-up-after-debt"
        );
    }
}
