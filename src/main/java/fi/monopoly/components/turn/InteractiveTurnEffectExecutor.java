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

        switch (effect) {
            case ShowMessageEffect e -> popupService.show(e.message(), next::doAction);
            case AdjustPlayerMoneyEffect e -> popupService.show(e.message(), () -> {
                e.player().addMoney(e.amount());
                next.doAction();
            });
            case OfferToBuyPropertyEffect e -> {
                if (gameState.getPropertyPurchaseFlow() == null) {
                    popupService.showPropertyOffer(e.property(), e.message(), () -> {
                        boolean couldBuy = e.player().buyProperty(e.property());
                        if (!couldBuy) {
                            popupService.show(text("property.buy.notEnough", e.property().getDisplayName()), () ->
                                    propertyAuctionResolver.resolve(
                                            e.player(), e.property(),
                                            PropertyAuctionResolver.AuctionReason.PLAYER_COULD_NOT_PAY, next
                                    )
                            );
                            return;
                        }
                        next.doAction();
                    }, () -> propertyAuctionResolver.resolve(
                            e.player(), e.property(),
                            PropertyAuctionResolver.AuctionReason.PLAYER_DECLINED, next
                    ));
                    return;
                }
                gameState.getPropertyPurchaseFlow().begin(
                        "player-" + e.player().getId(),
                        e.property().getSpotType().name(),
                        e.property().getDisplayName(),
                        e.property().getPrice(),
                        e.message(),
                        propertyPurchaseContinuation(e, index, effects)
                );
            }
            case PayRentEffect e -> popupService.show(e.message(), () -> gameState.getPaymentHandler().requestPayment(
                    new PaymentRequest(
                            e.fromPlayer(),
                            new PlayerTarget(e.toPlayer()),
                            e.amount(),
                            e.message()
                    ),
                    rentContinuation(e, index, effects),
                    next
            ));
            default -> throw new IllegalStateException("Unhandled interactive turn effect: " + effect.getClass().getSimpleName());
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
