package fi.monopoly.presentation.session;

import fi.monopoly.application.command.BuyPropertyCommand;
import fi.monopoly.application.command.DeclinePropertyCommand;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.application.session.PropertyPurchaseFlow;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.components.properties.Property;
import fi.monopoly.domain.decision.PendingDecision;

public final class PendingDecisionPopupAdapter implements PropertyPurchaseFlow {
    private final String sessionId;
    private final SessionApplicationService sessionApplicationService;
    private final PopupService popupService;
    private final LegacyPropertyPurchaseDecisionSupport propertyPurchaseDecisionSupport;
    private final Runnable postHandleSync;

    public PendingDecisionPopupAdapter(
            String sessionId,
            SessionApplicationService sessionApplicationService,
            PopupService popupService,
            LegacyPropertyPurchaseDecisionSupport propertyPurchaseDecisionSupport,
            Runnable postHandleSync
    ) {
        this.sessionId = sessionId;
        this.sessionApplicationService = sessionApplicationService;
        this.popupService = popupService;
        this.propertyPurchaseDecisionSupport = propertyPurchaseDecisionSupport;
        this.postHandleSync = postHandleSync;
    }

    @Override
    public void begin(fi.monopoly.components.Player player, Property property, String message, CallbackAction onComplete) {
        PendingDecision pendingDecision = propertyPurchaseDecisionSupport.openDecision(player, property, message, onComplete);
        popupService.showPropertyOffer(
                property,
                pendingDecision.summaryText(),
                () -> handleResult(sessionApplicationService.handle(new BuyPropertyCommand(
                        sessionId,
                        pendingDecision.actorPlayerId(),
                        pendingDecision.decisionId(),
                        property.getSpotType().name()
                ))),
                () -> handleResult(sessionApplicationService.handle(new DeclinePropertyCommand(
                        sessionId,
                        pendingDecision.actorPlayerId(),
                        pendingDecision.decisionId(),
                        property.getSpotType().name()
                )))
        );
    }

    private void handleResult(CommandResult result) {
        postHandleSync.run();
        if (result.accepted() || result.rejections().isEmpty()) {
            return;
        }
        popupService.show(result.rejections().get(0).message());
    }

    public interface LegacyPropertyPurchaseDecisionSupport {
        PendingDecision openDecision(fi.monopoly.components.Player player, Property property, String message, CallbackAction onComplete);
    }
}
