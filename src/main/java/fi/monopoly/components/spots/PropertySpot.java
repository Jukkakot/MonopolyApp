package fi.monopoly.components.spots;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.GameState;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.turn.LegacyTurnEffectExecutor;
import fi.monopoly.components.turn.PropertyTurnResolver;
import fi.monopoly.images.SpotImage;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.TurnResult;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
public class PropertySpot extends Spot {
    private static final PropertyTurnResolver TURN_RESOLVER = new PropertyTurnResolver();
    @Getter
    private final Property property;

    public PropertySpot(SpotImage spotImage, SpotType spotType) {
        super(spotImage);
        this.property = PropertyFactory.getProperty(spotType);
    }

    @Override
    public TurnResult handleTurn(GameState gameState, CallbackAction callbackAction) {
        LegacyTurnEffectExecutor turnEffectExecutor = new LegacyTurnEffectExecutor(runtime.popupService(), gameState.getPlayers());
        turnEffectExecutor.execute(TURN_RESOLVER.resolve(gameState, getName(), property), gameState.getPaymentHandler(), callbackAction);
        return null;
    }
}
