package fi.monopoly.presentation.session.auction;

import fi.monopoly.application.command.FinishAuctionResolutionCommand;
import fi.monopoly.application.command.PassAuctionCommand;
import fi.monopoly.application.command.PlaceAuctionBidCommand;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.domain.session.AuctionState;
import fi.monopoly.domain.session.AuctionStatus;
import fi.monopoly.domain.session.SeatKind;
import fi.monopoly.domain.session.SeatState;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.types.SpotType;
import javafx.scene.paint.Color;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

import static fi.monopoly.text.UiTexts.text;

@RequiredArgsConstructor
public final class AuctionViewAdapter {
    private final String sessionId;
    private final SessionCommandPort sessionApplicationService;
    private final PopupService popupService;
    private String renderedAuctionSignature;
    private String renderedResolutionAuctionId;

    public void sync() {
        SessionState state = sessionApplicationService.currentState();
        AuctionState auctionState = state.auctionState();
        if (auctionState == null) {
            renderedAuctionSignature = null;
            renderedResolutionAuctionId = null;
            return;
        }
        if (auctionState.status() == AuctionStatus.WON_PENDING_RESOLUTION) {
            syncResolutionPopup(auctionState, state);
            return;
        }
        renderedResolutionAuctionId = null;
        SeatState actorSeat = seatByPlayerId(auctionState.currentActorPlayerId(), state);
        if (actorSeat == null || actorSeat.seatKind() == SeatKind.BOT) {
            renderedAuctionSignature = null;
            return;
        }
        String signature = auctionSignature(auctionState);
        if (Objects.equals(renderedAuctionSignature, signature) && "propertyAuction".equals(popupService.activePopupKind())) {
            return;
        }
        Property property = propertyById(auctionState.propertyId());
        if (property == null) {
            return;
        }
        SeatState leaderSeat = seatByPlayerId(auctionState.leadingPlayerId(), state);
        popupService.showPropertyAuction(
                property,
                text("property.auction.prompt", actorSeat.displayName(), property.getDisplayName()),
                leaderSeat != null ? leaderSeat.displayName() : null,
                leaderSeat != null ? colorFromHex(leaderSeat.tokenColorHex()) : null,
                auctionState.currentBid(),
                text("property.auction.bid", auctionState.minimumNextBid()),
                text("property.auction.pass"),
                () -> handleResult(sessionApplicationService.handle(new PlaceAuctionBidCommand(
                        sessionId,
                        auctionState.currentActorPlayerId(),
                        auctionState.auctionId(),
                        auctionState.minimumNextBid()
                ))),
                () -> handleResult(sessionApplicationService.handle(new PassAuctionCommand(
                        sessionId,
                        auctionState.currentActorPlayerId(),
                        auctionState.auctionId()
                )))
        );
        renderedAuctionSignature = signature;
    }

    private void syncResolutionPopup(AuctionState auctionState, SessionState state) {
        renderedAuctionSignature = null;
        if (Objects.equals(renderedResolutionAuctionId, auctionState.auctionId()) && popupService.isAnyVisible()) {
            return;
        }
        Property property = propertyById(auctionState.propertyId());
        SeatState winnerSeat = seatByPlayerId(auctionState.winningPlayerId(), state);
        if (property == null || winnerSeat == null) {
            return;
        }
        popupService.show(
                text("property.auction.won", winnerSeat.displayName(), property.getDisplayName(), auctionState.winningBid()),
                () -> handleResult(sessionApplicationService.handle(new FinishAuctionResolutionCommand(
                        sessionId,
                        auctionState.auctionId()
                )))
        );
        renderedResolutionAuctionId = auctionState.auctionId();
    }

    private void handleResult(CommandResult result) {
        sync();
        if (result.accepted() || result.rejections().isEmpty()) {
            return;
        }
        popupService.show(result.rejections().get(0).message());
    }

    private String auctionSignature(AuctionState auctionState) {
        return auctionState.auctionId()
                + "|" + auctionState.currentActorPlayerId()
                + "|" + auctionState.leadingPlayerId()
                + "|" + auctionState.currentBid()
                + "|" + auctionState.minimumNextBid()
                + "|" + auctionState.passedPlayerIds().hashCode();
    }

    private static SeatState seatByPlayerId(String playerId, SessionState state) {
        if (playerId == null || state == null) {
            return null;
        }
        for (SeatState seat : state.seats()) {
            if (playerId.equals(seat.playerId())) {
                return seat;
            }
        }
        return null;
    }

    private static Color colorFromHex(String hex) {
        if (hex == null || hex.length() != 7 || !hex.startsWith("#")) {
            return null;
        }
        try {
            return Color.web(hex);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Property propertyById(String propertyId) {
        if (propertyId == null) {
            return null;
        }
        return PropertyFactory.getProperty(SpotType.valueOf(propertyId));
    }
}
