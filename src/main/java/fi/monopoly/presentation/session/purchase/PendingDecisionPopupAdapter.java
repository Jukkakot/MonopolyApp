package fi.monopoly.presentation.session.purchase;

import fi.monopoly.application.command.BuyPropertyCommand;
import fi.monopoly.application.command.DeclinePropertyCommand;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.application.session.purchase.PropertyPurchaseFlow;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.domain.decision.PendingDecision;
import fi.monopoly.domain.decision.PropertyPurchaseDecisionPayload;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.TurnContinuationState;
import fi.monopoly.types.SpotType;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
public final class PendingDecisionPopupAdapter implements PropertyPurchaseFlow {
    private final String sessionId;
    private final SessionCommandPort sessionApplicationService;
    private final PopupService popupService;
    private final LegacyPropertyPurchaseDecisionSupport propertyPurchaseDecisionSupport;
    private final Runnable postHandleSync;
    private String renderedDecisionId;

    @Override
    public void begin(
            String playerId,
            String propertyId,
            String displayName,
            int price,
            String message,
            TurnContinuationState continuationState
    ) {
        PendingDecision pendingDecision = propertyPurchaseDecisionSupport.openDecision(playerId, propertyId, displayName, price, message, continuationState);
        renderedDecisionId = pendingDecision.decisionId();
        showPropertyOffer(pendingDecision, propertyId);
    }

    public void sync() {
        SessionState state = sessionApplicationService.currentState();
        PendingDecision pendingDecision = state.pendingDecision();
        if (pendingDecision == null) {
            clearRenderedDecision();
            return;
        }
        if (!(pendingDecision.payload() instanceof PropertyPurchaseDecisionPayload payload)) {
            clearRenderedDecision();
            return;
        }
        if (pendingDecision.actorPlayerId() == null) {
            clearRenderedDecision();
            return;
        }
        if (Objects.equals(renderedDecisionId, pendingDecision.decisionId())
                && "propertyOffer".equals(popupService.activePopupKind())) {
            return;
        }
        renderedDecisionId = pendingDecision.decisionId();
        showPropertyOffer(pendingDecision, payload.propertyId());
    }

    private void showPropertyOffer(PendingDecision pendingDecision, String propertyId) {
        Property property = PropertyFactory.getProperty(SpotType.valueOf(propertyId));
        if (property == null) {
            clearRenderedDecision();
            return;
        }
        popupService.showPropertyOffer(
                property,
                pendingDecision.summaryText(),
                () -> handleResult(sessionApplicationService.handle(new BuyPropertyCommand(
                        sessionId,
                        pendingDecision.actorPlayerId(),
                        pendingDecision.decisionId(),
                        propertyId
                ))),
                () -> handleResult(sessionApplicationService.handle(new DeclinePropertyCommand(
                        sessionId,
                        pendingDecision.actorPlayerId(),
                        pendingDecision.decisionId(),
                        propertyId
                )))
        );
    }

    private void handleResult(CommandResult result) {
        renderedDecisionId = null;
        postHandleSync.run();
        if (result.accepted() || result.rejections().isEmpty()) {
            return;
        }
        popupService.show(result.rejections().get(0).message());
    }

    private void clearRenderedDecision() {
        renderedDecisionId = null;
        popupService.dismissActivePopup("propertyOffer");
    }

    public interface LegacyPropertyPurchaseDecisionSupport {
        PendingDecision openDecision(
                String playerId,
                String propertyId,
                String displayName,
                int price,
                String message,
                TurnContinuationState continuationState
        );
    }
}
