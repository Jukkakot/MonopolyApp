package fi.monopoly.application.session.purchase;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;
import fi.monopoly.domain.session.TurnContinuationState;

public interface PropertyPurchaseFlow {
    void begin(Player player, Property property, String message, TurnContinuationState continuationState, CallbackAction onComplete);
}
