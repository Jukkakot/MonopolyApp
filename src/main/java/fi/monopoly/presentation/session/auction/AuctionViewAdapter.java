package fi.monopoly.presentation.session.auction;

import fi.monopoly.application.command.FinishAuctionResolutionCommand;
import fi.monopoly.application.command.PassAuctionCommand;
import fi.monopoly.application.command.PlaceAuctionBidCommand;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.domain.session.AuctionState;
import fi.monopoly.domain.session.AuctionStatus;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.types.SpotType;

import java.util.Objects;

import static fi.monopoly.text.UiTexts.text;

public final class AuctionViewAdapter {
    private final String sessionId;
    private final SessionCommandPort sessionApplicationService;
    private final PopupService popupService;
    private final Players players;
    private String renderedAuctionSignature;
    private String renderedResolutionAuctionId;

    public AuctionViewAdapter(
            String sessionId,
            SessionCommandPort sessionApplicationService,
            PopupService popupService,
            Players players
    ) {
        this.sessionId = sessionId;
        this.sessionApplicationService = sessionApplicationService;
        this.popupService = popupService;
        this.players = players;
    }

    public void sync() {
        SessionState state = sessionApplicationService.currentState();
        AuctionState auctionState = state.auctionState();
        if (auctionState == null) {
            renderedAuctionSignature = null;
            renderedResolutionAuctionId = null;
            return;
        }
        if (auctionState.status() == AuctionStatus.WON_PENDING_RESOLUTION) {
            syncResolutionPopup(auctionState);
            return;
        }
        renderedResolutionAuctionId = null;
        Player actor = playerById(auctionState.currentActorPlayerId());
        if (actor == null || actor.isComputerControlled()) {
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
        popupService.showPropertyAuction(
                property,
                text("property.auction.prompt", actor.getName(), property.getDisplayName()),
                playerById(auctionState.leadingPlayerId()),
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

    private void syncResolutionPopup(AuctionState auctionState) {
        renderedAuctionSignature = null;
        if (Objects.equals(renderedResolutionAuctionId, auctionState.auctionId()) && popupService.isAnyVisible()) {
            return;
        }
        Property property = propertyById(auctionState.propertyId());
        Player winner = playerById(auctionState.winningPlayerId());
        if (property == null || winner == null) {
            return;
        }
        popupService.show(
                text("property.auction.won", winner.getName(), property.getDisplayName(), auctionState.winningBid()),
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

    private Player playerById(String playerId) {
        if (playerId == null || players == null) {
            return null;
        }
        for (Player player : players.getPlayers()) {
            if (playerId.equals("player-" + player.getId())) {
                return player;
            }
        }
        return null;
    }

    private Property propertyById(String propertyId) {
        if (propertyId == null) {
            return null;
        }
        return PropertyFactory.getProperty(SpotType.valueOf(propertyId));
    }
}
