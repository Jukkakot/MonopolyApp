package fi.monopoly.components.trade;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.Player;
import fi.monopoly.components.popup.components.ButtonProps;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
public final class TradeController {
    private final MonopolyRuntime runtime;
    private final BooleanSupplier canOpenTrade;
    private final Supplier<Player> currentPlayerSupplier;
    private final Supplier<List<Player>> playersSupplier;
    private final TradeOfferEvaluator tradeOfferEvaluator = new TradeOfferEvaluator();
    private final TradeUiBuilder tradeUiBuilder = new TradeUiBuilder(tradeOfferEvaluator);

    public TradeController(
            MonopolyRuntime runtime,
            BooleanSupplier canOpenTrade,
            Supplier<Player> currentPlayerSupplier,
            Supplier<List<Player>> playersSupplier
    ) {
        this.runtime = runtime;
        this.canOpenTrade = canOpenTrade;
        this.currentPlayerSupplier = currentPlayerSupplier;
        this.playersSupplier = playersSupplier;
    }

    public void openTradeMenu() {
        if (!canOpenTrade.getAsBoolean()) {
            return;
        }
        Player proposer = currentPlayerSupplier.get();
        if (proposer == null) {
            return;
        }
        List<Player> tradePartners = playersSupplier.get().stream()
                .filter(player -> player != proposer)
                .sorted(Comparator.comparingInt(Player::getTurnNumber))
                .toList();
        if (tradePartners.isEmpty()) {
            runtime.popupService().show(text("trade.noPartners"));
            return;
        }
        runtime.popupService().showTrade(
                text("trade.choosePartner", proposer.getName()),
                tradeUiBuilder.buildPartnerSelectionView(
                        proposer,
                        tradePartners,
                        player -> () -> openTradeEditor(new TradeDraft(proposer, player, TradeSelection.NONE, TradeSelection.NONE), true)
                )
        );
    }

    private void openTradeEditor(TradeDraft draft, boolean editingOfferSide) {
        runtime.popupService().showTrade(
                tradeUiBuilder.buildTradeEditorSummary(draft, editingOfferSide),
                tradeUiBuilder.buildTradePopupView(
                        draft.toOffer(),
                        text("trade.editor.header"),
                        text("trade.editor.editing", (editingOfferSide ? draft.proposer() : draft.recipient()).getName()),
                        editingOfferSide,
                        this::openTradeMenu,
                        (nextDraft, nextOfferSide) -> () -> openTradeEditor(nextDraft, nextOfferSide)
                ),
                tradeUiBuilder.buildTradeEditorButtons(
                        draft,
                        editingOfferSide,
                        (nextDraft, nextOfferSide) -> () -> openTradeEditor(nextDraft, nextOfferSide),
                        nextDraft -> () -> confirmTradeOffer(nextDraft)
                )
        );
    }

    private void confirmTradeOffer(TradeDraft draft) {
        TradeOffer offer = draft.toOffer();
        if (!offer.isValid()) {
            runtime.popupService().show(text("trade.invalid"));
            return;
        }
        String summary = tradeUiBuilder.buildTradeSummary(offer);
        if (offer.recipient().isComputerControlled()) {
            BotTradeProfile tradeProfile = resolveTradeProfile(offer.recipient());
            TradeDecision decision = tradeOfferEvaluator.evaluateForRecipient(offer, tradeProfile);
            log.info("Bot trade decision: player={}, accept={}, score={}, reason={}",
                    offer.recipient().getName(),
                    decision.accept(),
                    Math.round(decision.score() * 10.0) / 10.0,
                    decision.reason());
            if (decision.accept()) {
                applyTradeOffer(offer);
                runtime.popupService().show(text("trade.accepted", offer.recipient().getName()) + "\n" + summary);
            } else {
                TradeOffer counterOffer = tradeOfferEvaluator.proposeCounterOffer(offer, tradeProfile);
                if (counterOffer != null) {
                    presentBotCounterOffer(offer, counterOffer, offer.recipient().getName());
                } else {
                    runtime.popupService().show(text("trade.declined", offer.recipient().getName()) + "\n" + decision.reason());
                }
            }
            return;
        }
        runtime.popupService().showTrade(
                text("trade.review", offer.recipient().getName()) + "\n" + summary,
                tradeUiBuilder.buildTradePopupView(
                        offer,
                        text("trade.review.header"),
                        text("trade.review", offer.recipient().getName()),
                        null,
                        () -> openTradeEditor(draft, false),
                        (nextDraft, nextOfferSide) -> () -> openTradeEditor(nextDraft, nextOfferSide)
                ),
                new ButtonProps(text("popup.choice.accept"), () -> {
                    applyTradeOffer(offer);
                    runtime.popupService().show(text("trade.accepted", offer.recipient().getName()));
                }),
                new ButtonProps(text("popup.choice.decline"), () -> runtime.popupService().show(text("trade.declined", offer.recipient().getName()))),
                new ButtonProps(text("trade.button.counterOffer"), () -> openTradeEditor(draft.asCounterOffer(), true))
        );
    }

    private void applyTradeOffer(TradeOffer offer) {
        if (!offer.apply()) {
            runtime.popupService().show(text("trade.invalid"));
        }
    }

    private void presentBotCounterOffer(TradeOffer originalOffer, TradeOffer counterOffer, String counteringPlayerName) {
        String summary = tradeUiBuilder.buildCounterOfferSummary(originalOffer, counterOffer, counteringPlayerName);
        if (counterOffer.proposer().isComputerControlled()) {
            BotTradeProfile proposerTradeProfile = resolveTradeProfile(counterOffer.proposer());
            TradeDecision proposerDecision = tradeOfferEvaluator.evaluateForRecipient(counterOffer.reversePerspective(), proposerTradeProfile);
            if (proposerDecision.accept()) {
                applyTradeOffer(counterOffer);
                runtime.popupService().show(text("trade.accepted", counterOffer.proposer().getName()) + "\n" + summary);
            } else {
                runtime.popupService().show(text("trade.declined", counterOffer.proposer().getName()) + "\n" + proposerDecision.reason());
            }
            return;
        }
        runtime.popupService().showTrade(
                summary,
                tradeUiBuilder.buildTradePopupView(
                        counterOffer,
                        text("trade.countered", counteringPlayerName),
                        text("trade.countered.new"),
                        null,
                        () -> openTradeEditor(draftFromOffer(counterOffer), false),
                        (nextDraft, nextOfferSide) -> () -> openTradeEditor(nextDraft, nextOfferSide)
                ),
                new ButtonProps(text("popup.choice.accept"), () -> {
                    applyTradeOffer(counterOffer);
                    runtime.popupService().show(text("trade.accepted", counterOffer.proposer().getName()));
                }),
                new ButtonProps(text("popup.choice.decline"), () -> runtime.popupService().show(text("trade.declined", counterOffer.proposer().getName()))),
                new ButtonProps(text("trade.button.counterOffer"), () -> openTradeEditor(draftFromOffer(counterOffer), true))
        );
    }

    private BotTradeProfile resolveTradeProfile(Player player) {
        return switch (player.getComputerProfile()) {
            case SMOKE_TEST -> BotTradeProfile.CAUTIOUS;
            case STRONG -> BotTradeProfile.BALANCED;
            case HUMAN -> BotTradeProfile.AGGRESSIVE;
        };
    }

    private TradeDraft draftFromOffer(TradeOffer offer) {
        return new TradeDraft(offer.proposer(), offer.recipient(), offer.offeredToRecipient(), offer.requestedFromRecipient());
    }
}
