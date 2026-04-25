package fi.monopoly.presentation.session.trade;

import fi.monopoly.application.command.*;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.components.Player;
import fi.monopoly.components.popup.ButtonAction;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.components.popup.TradePopupView;
import fi.monopoly.components.popup.components.ButtonProps;
import fi.monopoly.components.trade.TradeDraft;
import fi.monopoly.components.trade.TradeOffer;
import fi.monopoly.components.trade.TradeUiBuilder;
import fi.monopoly.domain.session.TradeEditPatch;
import fi.monopoly.domain.session.TradeOfferState;
import fi.monopoly.domain.session.TradeState;
import fi.monopoly.domain.session.TradeStatus;
import fi.monopoly.presentation.legacy.session.trade.LegacyTradeGateway;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import static fi.monopoly.text.UiTexts.text;

public final class TradeViewAdapter {
    private final String sessionId;
    private final SessionCommandPort sessionApplicationService;
    private final PopupService popupService;
    private final LegacyTradeGateway legacyTradeGateway;
    private final TradeUiBuilder tradeUiBuilder;
    private final BooleanSupplier isComputerTurnSupplier;
    private String renderedSignature;

    public TradeViewAdapter(
            String sessionId,
            SessionCommandPort sessionApplicationService,
            PopupService popupService,
            LegacyTradeGateway legacyTradeGateway,
            TradeUiBuilder tradeUiBuilder,
            BooleanSupplier isComputerTurnSupplier
    ) {
        this.sessionId = sessionId;
        this.sessionApplicationService = sessionApplicationService;
        this.popupService = popupService;
        this.legacyTradeGateway = legacyTradeGateway;
        this.tradeUiBuilder = tradeUiBuilder;
        this.isComputerTurnSupplier = isComputerTurnSupplier;
    }

    public void sync() {
        TradeState tradeState = sessionApplicationService.currentState().tradeState();
        if (tradeState == null) {
            renderedSignature = null;
            return;
        }
        Player editingPlayer = legacyTradeGateway.playerById(tradeState.editingPlayerId());
        if (editingPlayer != null && editingPlayer.isComputerControlled()) {
            renderedSignature = null;
            return;
        }
        String signature = signature(tradeState);
        if (Objects.equals(renderedSignature, signature) && "trade".equals(popupService.activePopupKind())) {
            return;
        }
        TradeOffer offer = legacyTradeGateway.toLegacyOffer(tradeState.currentOffer());
        if (offer == null) {
            return;
        }
        if (tradeState.status() == TradeStatus.EDITING) {
            showEditor(tradeState, offer);
        } else {
            showReview(tradeState, offer);
        }
        renderedSignature = signature;
    }

    private void showEditor(TradeState tradeState, TradeOffer offer) {
        boolean editingOfferSide = tradeState.editingOfferedSide();
        TradeDraft draft = new TradeDraft(offer.proposer(), offer.recipient(), offer.offeredToRecipient(), offer.requestedFromRecipient());
        Player editingPlayer = editingOfferSide ? offer.proposer() : offer.recipient();
        String notice = tradeState.status() == TradeStatus.COUNTERED
                ? text("trade.editor.countering", editingPlayer.getName(), offer.recipient().getName())
                : null;
        popupService.showTrade(
                tradeUiBuilder.buildTradeEditorSummary(draft, editingOfferSide, notice),
                tradeUiBuilder.buildTradePopupView(
                        offer,
                        text("trade.editor.header"),
                        buildEditorSubtitle(tradeState, offer, editingOfferSide, notice),
                        editingOfferSide,
                        false,
                        canBackToReview(tradeState) ? () -> handleResult(sessionApplicationService.handle(new CancelTradeCommand(sessionId, tradeState.editingPlayerId(), tradeState.tradeId()))) : null,
                        (nextDraft, nextOfferSide) -> () -> applyDraftEdit(tradeState, nextDraft, nextOfferSide)
                ),
                buildEditorButtons(tradeState, draft, editingOfferSide)
        );
    }

    private ButtonProps[] buildEditorButtons(TradeState tradeState, TradeDraft draft, boolean editingOfferSide) {
        return tradeUiBuilder.buildTradeEditorButtons(
                draft,
                editingOfferSide,
                (nextDraft, nextOfferSide) -> () -> applyDraftEdit(tradeState, nextDraft, nextOfferSide),
                nextDraft -> () -> {
                    applyDraftEdit(tradeState, nextDraft, editingOfferSide);
                    CommandResult result = tradeState.status() == TradeStatus.COUNTERED
                            ? sessionApplicationService.handle(new CounterTradeCommand(sessionId, tradeState.editingPlayerId(), tradeState.tradeId()))
                            : sessionApplicationService.handle(new SubmitTradeOfferCommand(sessionId, tradeState.editingPlayerId(), tradeState.tradeId()));
                    handleResult(result);
                }
        );
    }

    private void showReview(TradeState tradeState, TradeOffer offer) {
        TradeDraft draft = new TradeDraft(offer.proposer(), offer.recipient(), offer.offeredToRecipient(), offer.requestedFromRecipient());
        String subtitle = tradeState.status() == TradeStatus.COUNTERED
                ? text("trade.review.countered", offer.proposer().getName(), offer.recipient().getName())
                : text("trade.review", offer.recipient().getName());
        TradePopupView view = tradeUiBuilder.buildTradePopupView(
                offer,
                text("trade.review.header"),
                subtitle,
                null,
                tradeState.decisionRequiredFromPlayerId() != null && isComputerTurnSupplier.getAsBoolean(),
                () -> {
                    TradeOfferState reversed = tradeState.currentOffer().reversePerspective();
                    handleResult(sessionApplicationService.handle(new EditTradeOfferCommand(
                            sessionId,
                            tradeState.decisionRequiredFromPlayerId(),
                            tradeState.tradeId(),
                            new TradeEditPatch(true, true, null, List.of(), List.of(), null)
                    )));
                },
                (nextDraft, nextOfferSide) -> () -> applyDraftEdit(tradeState, nextDraft, nextOfferSide)
        );
        popupService.showTrade(
                subtitle + "\n" + tradeUiBuilder.buildTradeSummary(offer),
                view,
                new ButtonProps(text("popup.choice.accept"), () -> handleResult(sessionApplicationService.handle(new AcceptTradeCommand(
                        sessionId,
                        tradeState.decisionRequiredFromPlayerId(),
                        tradeState.tradeId()
                )))),
                new ButtonProps(text("popup.choice.decline"), () -> handleResult(sessionApplicationService.handle(new DeclineTradeCommand(
                        sessionId,
                        tradeState.decisionRequiredFromPlayerId(),
                        tradeState.tradeId()
                )))),
                new ButtonProps(text("trade.button.counterOffer"), () -> {
                    handleResult(sessionApplicationService.handle(new EditTradeOfferCommand(
                            sessionId,
                            tradeState.decisionRequiredFromPlayerId(),
                            tradeState.tradeId(),
                            new TradeEditPatch(true, true, null, List.of(), List.of(), null)
                    )));
                })
        );
    }

    private boolean canBackToReview(TradeState tradeState) {
        return tradeState.status() == TradeStatus.COUNTERED || tradeState.decisionRequiredFromPlayerId() != null;
    }

    private void applyDraftEdit(TradeState tradeState, TradeDraft nextDraft, boolean nextOfferSide) {
        TradeOfferState nextState = legacyTradeGateway.toState(nextDraft.toOffer());
        TradeOfferState currentState = tradeState.currentOffer();
        handleEditDelta(tradeState, currentState.offeredToRecipient().propertyIds(), nextState.offeredToRecipient().propertyIds(), true, nextOfferSide);
        handleEditDelta(tradeState, currentState.requestedFromRecipient().propertyIds(), nextState.requestedFromRecipient().propertyIds(), false, nextOfferSide);
        handleMoneyEdit(tradeState, nextState, nextOfferSide);
        handleJailCardEdit(tradeState, nextState, nextOfferSide);
    }

    private void handleEditDelta(TradeState tradeState, List<String> currentIds, List<String> nextIds, boolean offeredSide, boolean nextOfferSide) {
        if (Objects.equals(currentIds, nextIds) && tradeState.editingOfferedSide() == nextOfferSide) {
            return;
        }
        List<String> idsToAdd = nextIds.stream().filter(id -> !currentIds.contains(id)).toList();
        List<String> idsToRemove = currentIds.stream().filter(id -> !nextIds.contains(id)).toList();
        handleResult(sessionApplicationService.handle(new EditTradeOfferCommand(
                sessionId,
                tradeState.editingPlayerId(),
                tradeState.tradeId(),
                new TradeEditPatch(null, offeredSide, null, idsToAdd, idsToRemove, null)
        )));
        if (tradeState.editingOfferedSide() != nextOfferSide) {
            handleResult(sessionApplicationService.handle(new EditTradeOfferCommand(
                    sessionId,
                    tradeState.editingPlayerId(),
                    tradeState.tradeId(),
                    new TradeEditPatch(null, nextOfferSide, null, List.of(), List.of(), null)
            )));
        }
    }

    private void handleMoneyEdit(TradeState tradeState, TradeOfferState nextState, boolean nextOfferSide) {
        int currentMoney = nextOfferSide ? tradeState.currentOffer().offeredToRecipient().moneyAmount() : tradeState.currentOffer().requestedFromRecipient().moneyAmount();
        int nextMoney = nextOfferSide ? nextState.offeredToRecipient().moneyAmount() : nextState.requestedFromRecipient().moneyAmount();
        if (currentMoney == nextMoney) {
            return;
        }
        handleResult(sessionApplicationService.handle(new EditTradeOfferCommand(
                sessionId,
                tradeState.editingPlayerId(),
                tradeState.tradeId(),
                new TradeEditPatch(null, nextOfferSide, nextMoney, List.of(), List.of(), null)
        )));
    }

    private void handleJailCardEdit(TradeState tradeState, TradeOfferState nextState, boolean nextOfferSide) {
        int currentCards = nextOfferSide ? tradeState.currentOffer().offeredToRecipient().jailCardCount() : tradeState.currentOffer().requestedFromRecipient().jailCardCount();
        int nextCards = nextOfferSide ? nextState.offeredToRecipient().jailCardCount() : nextState.requestedFromRecipient().jailCardCount();
        if (currentCards == nextCards) {
            return;
        }
        handleResult(sessionApplicationService.handle(new EditTradeOfferCommand(
                sessionId,
                tradeState.editingPlayerId(),
                tradeState.tradeId(),
                new TradeEditPatch(null, nextOfferSide, null, List.of(), List.of(), true)
        )));
    }

    private void handleResult(CommandResult result) {
        sync();
        if (result.accepted() || result.rejections().isEmpty()) {
            return;
        }
        popupService.show(result.rejections().get(0).message());
    }

    private String signature(TradeState tradeState) {
        return tradeState.tradeId()
                + "|" + tradeState.status()
                + "|" + tradeState.currentOffer().hashCode()
                + "|" + tradeState.editingPlayerId()
                + "|" + tradeState.editingOfferedSide()
                + "|" + tradeState.decisionRequiredFromPlayerId();
    }

    private String buildEditorSubtitle(TradeState tradeState, TradeOffer offer, boolean editingOfferSide, String notice) {
        String subtitle = text("trade.editor.editing", (editingOfferSide ? offer.proposer() : offer.recipient()).getName());
        if (notice == null || notice.isBlank()) {
            return subtitle;
        }
        return subtitle + "\n" + notice;
    }
}
