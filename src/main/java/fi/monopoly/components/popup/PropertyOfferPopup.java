package fi.monopoly.components.popup;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.properties.Property;

public class PropertyOfferPopup extends ChoicePopup {
    private Property offeredProperty;

    protected PropertyOfferPopup(MonopolyRuntime runtime) {
        super(runtime);
    }

    public void setOfferedProperty(Property offeredProperty) {
        this.offeredProperty = offeredProperty;
    }

    public Property getOfferedProperty() {
        return offeredProperty;
    }

    @Override
    protected void hide() {
        super.hide();
        offeredProperty = null;
    }
}
