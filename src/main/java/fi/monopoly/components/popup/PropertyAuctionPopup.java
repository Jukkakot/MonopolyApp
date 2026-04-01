package fi.monopoly.components.popup;

import fi.monopoly.MonopolyRuntime;

public class PropertyAuctionPopup extends PropertyOfferPopup {

    protected PropertyAuctionPopup(MonopolyRuntime runtime) {
        super(runtime, "propertyAuction");
    }

    public void setButtonLabels(String primaryLabel, String secondaryLabel) {
        setActionLabels(primaryLabel, secondaryLabel);
    }

    @Override
    protected boolean onComputerAction(fi.monopoly.components.computer.ComputerPlayerProfile profile) {
        return false;
    }

    @Override
    protected boolean allowManualInteractionDuringComputerTurn() {
        return true;
    }
}
