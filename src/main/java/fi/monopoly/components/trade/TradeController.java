package fi.monopoly.components.trade;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.Player;
import fi.monopoly.components.computer.StrongBotConfig;
import fi.monopoly.components.popup.ButtonAction;
import fi.monopoly.components.popup.components.ButtonProps;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
public final class TradeController {
    private static final StrongBotConfig STRONG_CONFIG = StrongBotConfig.defaults();
    private final MonopolyRuntime runtime;
    private final BooleanSupplier canOpenTrade;
    private final Supplier<Player> currentPlayerSupplier;
    private final Supplier<List<Player>> playersSupplier;
    private final TradeOfferEvaluator tradeOfferEvaluator = new TradeOfferEvaluator();
    private final TradeUiBuilder tradeUiBuilder = new TradeUiBuilder(tradeOfferEvaluator);
    private final StrongTradePlanner strongTradePlanner = new StrongTradePlanner(STRONG_CONFIG);
    private Player lastProactiveTradePlayer;

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
                        player -> () -> openTradeEditor(
                                new TradeDraft(proposer, player, TradeSelection.NONE, TradeSelection.NONE),
                                true,
                                null,
                                this::openTradeMenu,
                                null
                        )
                )
        );
    }

    public fi.monopoly.components.computer.ComputerDecision tryInitiateComputerTrade(Player proposer) {
        if (!canOpenTrade.getAsBoolean() || proposer == null || !proposer.isComputerControlled()) {
            return null;
        }
        if (currentPlayerSupplier.get() != proposer || lastProactiveTradePlayer == proposer) {
            return null;
        }
        lastProactiveTradePlayer = proposer;
        if (proposer.getComputerProfile() != fi.monopoly.components.computer.ComputerPlayerProfile.STRONG) {
            return null;
        }
        StrongTradePlanner.TradePlan plan = strongTradePlanner.plan(proposer, playersSupplier.get());
        if (plan == null) {
            return null;
        }
        submitTradeOffer(
                plan.offer(),
                text("trade.review.proposed", plan.offer().proposer().getName(), plan.offer().recipient().getName()),
                plan.offer().recipient().getComputerProfile() == fi.monopoly.components.computer.ComputerPlayerProfile.HUMAN
        );
        return plan.decision();
    }

    private void openTradeEditor(
            TradeDraft draft,
            boolean editingOfferSide,
            String editorNotice,
            ButtonAction backAction,
            String reviewSubtitle
    ) {
        runtime.popupService().showTrade(
                tradeUiBuilder.buildTradeEditorSummary(draft, editingOfferSide, editorNotice),
                tradeUiBuilder.buildTradePopupView(
                        draft.toOffer(),
                        text("trade.editor.header"),
                        buildTradeEditorSubtitle(draft, editingOfferSide, editorNotice),
                        editingOfferSide,
                        false,
                        backAction,
                        (nextDraft, nextOfferSide) -> () -> openTradeEditor(nextDraft, nextOfferSide, editorNotice, backAction, reviewSubtitle)
                ),
                tradeUiBuilder.buildTradeEditorButtons(
                        draft,
                        editingOfferSide,
                        (nextDraft, nextOfferSide) -> () -> openTradeEditor(nextDraft, nextOfferSide, editorNotice, backAction, reviewSubtitle),
                        nextDraft -> () -> confirmTradeOffer(nextDraft, reviewSubtitle)
                )
        );
    }

    private void confirmTradeOffer(TradeDraft draft, String reviewSubtitle) {
        TradeOffer offer = draft.toOffer();
        if (!offer.isValid()) {
            runtime.popupService().show(text("trade.invalid"));
            return;
        }
        submitTradeOffer(offer, reviewSubtitle != null ? reviewSubtitle : text("trade.review", offer.recipient().getName()), false);
    }

    private void submitTradeOffer(TradeOffer offer, String reviewSubtitle, boolean allowHumanResponseDuringComputerTurn) {
        if (offer.recipient().isComputerControlled()) {
            BotTradeProfile tradeProfile = resolveTradeProfile(offer.recipient());
            StrongBotConfig strongConfig = resolveStrongTradeConfig(offer.recipient());
            TradeDecision decision = tradeOfferEvaluator.evaluateForRecipient(offer, tradeProfile, strongConfig);
            log.info("Bot trade decision: player={}, accept={}, score={}, reason={}",
                    offer.recipient().getName(),
                    decision.accept(),
                    Math.round(decision.score() * 10.0) / 10.0,
                    decision.reason());
            if (decision.accept()) {
                applyTradeOffer(offer);
                runtime.popupService().show(text("trade.accepted", offer.recipient().getName()) + "\n" + tradeUiBuilder.buildTradeSummary(offer));
            } else {
                TradeOffer counterOffer = tradeOfferEvaluator.proposeCounterOffer(offer, tradeProfile, strongConfig);
                if (counterOffer != null) {
                    if (offer.proposer().isComputerControlled()) {
                        handleComputerCounterOffer(counterOffer, offer);
                    } else {
                        showHumanTradeReview(
                                counterOffer,
                                draftFromOffer(counterOffer),
                                text("trade.review.countered", offer.recipient().getName(), counterOffer.recipient().getName()),
                                false
                        );
                    }
                } else {
                    runtime.popupService().show(text("trade.declined", offer.recipient().getName()) + "\n" + decision.reason());
                }
            }
            return;
        }
        showHumanTradeReview(offer, draftFromOffer(offer), reviewSubtitle, allowHumanResponseDuringComputerTurn);
    }

    private void applyTradeOffer(TradeOffer offer) {
        if (!offer.apply()) {
            runtime.popupService().show(text("trade.invalid"));
        }
    }

    private void handleComputerCounterOffer(TradeOffer counterOffer, TradeOffer originalOffer) {
        BotTradeProfile proposerProfile = resolveTradeProfile(originalOffer.proposer());
        StrongBotConfig proposerStrongConfig = resolveStrongTradeConfig(originalOffer.proposer());
        TradeDecision proposerDecision = tradeOfferEvaluator.evaluateForRecipient(
                counterOffer.reversePerspective(),
                proposerProfile,
                proposerStrongConfig
        );
        log.info("Bot counter-trade decision: player={}, accept={}, score={}, reason={}",
                originalOffer.proposer().getName(),
                proposerDecision.accept(),
                Math.round(proposerDecision.score() * 10.0) / 10.0,
                proposerDecision.reason());
        if (proposerDecision.accept()) {
            applyTradeOffer(counterOffer);
            runtime.popupService().show(text("trade.accepted", originalOffer.proposer().getName()) + "\n" + tradeUiBuilder.buildTradeSummary(counterOffer));
            return;
        }
        runtime.popupService().show(text("trade.declined", originalOffer.recipient().getName()) + "\n" + proposerDecision.reason());
    }

    private void showHumanTradeReview(TradeOffer offer, TradeDraft draft, String subtitle, boolean allowHumanResponseDuringComputerTurn) {
        runtime.popupService().showTrade(
                subtitle + "\n" + tradeUiBuilder.buildTradeSummary(offer),
                tradeUiBuilder.buildTradePopupView(
                        offer,
                        text("trade.review.header"),
                        subtitle,
                        null,
                        allowHumanResponseDuringComputerTurn,
                        () -> openTradeEditor(draft, false, null, this::openTradeMenu, subtitle),
                        (nextDraft, nextOfferSide) -> () -> openTradeEditor(nextDraft, nextOfferSide, null, this::openTradeMenu, subtitle)
                ),
                new ButtonProps(text("popup.choice.accept"), () -> {
                    applyTradeOffer(offer);
                    runtime.popupService().show(text("trade.accepted", offer.recipient().getName()));
                }),
                new ButtonProps(text("popup.choice.decline"), () -> runtime.popupService().show(text("trade.declined", offer.recipient().getName()))),
                new ButtonProps(
                        text("trade.button.counterOffer"),
                        () -> openTradeEditor(
                                draft.asCounterOffer(),
                                true,
                                text("trade.editor.countering", offer.recipient().getName(), offer.proposer().getName()),
                                () -> showHumanTradeReview(offer, draft, subtitle, allowHumanResponseDuringComputerTurn),
                                text("trade.review.countered", offer.recipient().getName(), offer.proposer().getName())
                        )
                )
        );
    }

    private BotTradeProfile resolveTradeProfile(Player player) {
        return switch (player.getComputerProfile()) {
            case SMOKE_TEST -> BotTradeProfile.CAUTIOUS;
            case STRONG -> BotTradeProfile.BALANCED;
            case HUMAN -> BotTradeProfile.AGGRESSIVE;
        };
    }

    private StrongBotConfig resolveStrongTradeConfig(Player player) {
        return player.getComputerProfile() == fi.monopoly.components.computer.ComputerPlayerProfile.STRONG ? STRONG_CONFIG : null;
    }

    private TradeDraft draftFromOffer(TradeOffer offer) {
        return new TradeDraft(offer.proposer(), offer.recipient(), offer.offeredToRecipient(), offer.requestedFromRecipient());
    }

    private String buildTradeEditorSubtitle(TradeDraft draft, boolean editingOfferSide, String editorNotice) {
        String subtitle = text("trade.editor.editing", (editingOfferSide ? draft.proposer() : draft.recipient()).getName());
        if (editorNotice == null || editorNotice.isBlank()) {
            return subtitle;
        }
        return subtitle + "\n" + editorNotice;
    }
}
