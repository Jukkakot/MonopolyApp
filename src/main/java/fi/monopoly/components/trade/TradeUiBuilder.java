package fi.monopoly.components.trade;

import fi.monopoly.components.Player;
import fi.monopoly.components.popup.ButtonAction;
import fi.monopoly.components.popup.TradePopupItem;
import fi.monopoly.components.popup.TradePopupView;
import fi.monopoly.components.popup.components.ButtonProps;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.StreetProperty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static fi.monopoly.text.UiTexts.text;

public final class TradeUiBuilder {
    private final TradeOfferEvaluator tradeOfferEvaluator;

    public TradeUiBuilder(TradeOfferEvaluator tradeOfferEvaluator) {
        this.tradeOfferEvaluator = tradeOfferEvaluator;
    }

    public TradePopupView buildPartnerSelectionView(
            Player proposer,
            List<Player> tradePartners,
            Function<Player, ButtonAction> onSelectPartner
    ) {
        return new TradePopupView(
                text("trade.choosePartnerTitle"),
                text("trade.choosePartner", proposer.getName()),
                null,
                proposer,
                List.of(TradePopupItem.empty(text("trade.choosePartnerCurrent"))),
                true,
                null,
                null,
                List.of(),
                false,
                null,
                text("trade.choosePartnerHint"),
                text("trade.choosePartnerList"),
                tradePartners.stream()
                        .map(player -> TradePopupItem.player(player, onSelectPartner.apply(player)))
                        .toList()
        );
    }

    public String buildTradeSummary(TradeOffer offer) {
        return buildTradeBoard(offer, text("trade.summary.header", offer.proposer().getName(), offer.recipient().getName()))
                + "\n" + buildTradeValueDeltaSummary(offer);
    }

    public String buildCounterOfferSummary(TradeOffer originalOffer, TradeOffer counterOffer, String counteringPlayerName) {
        return text("trade.countered", counteringPlayerName)
                + "\n\n" + text("trade.countered.original")
                + "\n" + buildTradeSummary(originalOffer)
                + "\n\n" + text("trade.countered.new")
                + "\n" + buildTradeSummary(counterOffer);
    }

    public String buildTradeEditorSummary(TradeDraft draft, boolean editingOfferSide) {
        Player editingPlayer = editingOfferSide ? draft.proposer() : draft.recipient();
        return buildTradeBoard(draft.toOffer(), text("trade.editor.header"))
                + "\n" + text("trade.editor.editing", editingPlayer.getName());
    }

    public TradePopupView buildTradePopupView(
            TradeOffer offer,
            String title,
            String subtitle,
            Boolean editingOfferSide,
            ButtonAction backAction,
            BiFunction<TradeDraft, Boolean, ButtonAction> onOpenEditor
    ) {
        TradeDraft draft = draftFromOffer(offer);
        return new TradePopupView(
                title,
                subtitle,
                backAction,
                offer.proposer(),
                describeTradeSelectionVisualItems(draft, offer.offeredToRecipient(), true, editingOfferSide, onOpenEditor),
                Boolean.TRUE.equals(editingOfferSide),
                editingOfferSide == null ? null : onOpenEditor.apply(draft, true),
                offer.recipient(),
                describeTradeSelectionVisualItems(draft, offer.requestedFromRecipient(), false, editingOfferSide, onOpenEditor),
                Boolean.FALSE.equals(editingOfferSide),
                editingOfferSide == null ? null : onOpenEditor.apply(draft, false),
                buildTradeValueDeltaSummary(offer),
                editingOfferSide == null
                        ? null
                        : text("trade.inventory.title", (editingOfferSide ? offer.proposer() : offer.recipient()).getName()),
                editingOfferSide == null
                        ? List.of()
                        : buildTradeInventoryItems(offer, editingOfferSide, onOpenEditor)
        );
    }

    public ButtonProps[] buildTradeEditorButtons(
            TradeDraft draft,
            boolean editingOfferSide,
            BiFunction<TradeDraft, Boolean, ButtonAction> onOpenEditor,
            Function<TradeDraft, ButtonAction> onConfirmTrade
    ) {
        Player editingPlayer = editingOfferSide ? draft.proposer() : draft.recipient();
        TradeSelection selection = editingOfferSide ? draft.offeredToRecipient() : draft.requestedFromRecipient();
        List<ButtonProps> buttons = new ArrayList<>();
        for (int delta : List.of(-10, -100)) {
            int nextAmount = Math.max(0, selection.moneyAmount() + delta);
            buttons.add(new ButtonProps(
                    "-" + text("format.money", Math.abs(delta)),
                    onOpenEditor.apply(updateTradeSelection(draft, editingOfferSide, selection.withMoneyAmount(nextAmount)), editingOfferSide)
            ));
        }
        for (int delta : List.of(10, 100)) {
            int cappedAmount = Math.min(editingPlayer.getMoneyAmount(), Math.max(0, selection.moneyAmount() + delta));
            buttons.add(new ButtonProps(
                    "+" + text("format.money", delta),
                    onOpenEditor.apply(updateTradeSelection(draft, editingOfferSide, selection.withMoneyAmount(cappedAmount)), editingOfferSide)
            ));
        }
        buttons.add(new ButtonProps(text("trade.button.done"), onConfirmTrade.apply(draft)));
        buttons.add(new ButtonProps(
                text("trade.button.clear"),
                onOpenEditor.apply(clearTradeSelection(draft, editingOfferSide), editingOfferSide)
        ));
        return buttons.toArray(ButtonProps[]::new);
    }

    private String buildTradeBoard(TradeOffer offer, String title) {
        return title
                + "\n" + text("trade.board.side", offer.proposer().getName(), describeTradeSelectionVisual(offer.offeredToRecipient()))
                + "\n" + text("trade.board.side", offer.recipient().getName(), describeTradeSelectionVisual(offer.requestedFromRecipient()));
    }

    private String buildTradeValueDeltaSummary(TradeOffer offer) {
        int recipientDelta = offer.isEmpty() ? 0 : tradeOfferEvaluator.estimateNetDeltaForRecipient(offer);
        int proposerDelta = offer.isEmpty() ? 0 : tradeOfferEvaluator.estimateNetDeltaForRecipient(offer.reversePerspective());
        return text(
                "trade.summary.delta",
                offer.proposer().getName(),
                formatTradeDelta(proposerDelta),
                offer.recipient().getName(),
                formatTradeDelta(recipientDelta)
        );
    }

    private String formatTradeDelta(int value) {
        return (value >= 0 ? "+" : "") + value;
    }

    private String describeTradeSelectionVisual(TradeSelection selection) {
        return describeTradeSelectionVisualItems(selection).stream()
                .map(TradePopupItem::label)
                .reduce((left, right) -> left + " " + right)
                .orElse(text("trade.option.nothingVisual"));
    }

    private List<TradePopupItem> describeTradeSelectionVisualItems(TradeSelection selection) {
        if (selection.isEmpty()) {
            return List.of(TradePopupItem.empty(text("trade.option.nothingVisual")));
        }
        List<TradePopupItem> parts = new ArrayList<>();
        if (selection.moneyAmount() > 0) {
            parts.add(TradePopupItem.money(text("trade.option.moneyVisual", selection.moneyAmount())));
        }
        for (Property property : selection.properties()) {
            parts.add(TradePopupItem.property(property, false, null));
        }
        if (selection.jailCard()) {
            parts.add(TradePopupItem.jailCard(""));
        }
        return parts;
    }

    private List<TradePopupItem> describeTradeSelectionVisualItems(
            TradeDraft draft,
            TradeSelection selection,
            boolean offerSide,
            Boolean editingOfferSide,
            BiFunction<TradeDraft, Boolean, ButtonAction> onOpenEditor
    ) {
        if (selection.isEmpty()) {
            return List.of(TradePopupItem.empty(text("trade.option.nothingVisual")));
        }
        List<TradePopupItem> parts = new ArrayList<>();
        if (selection.moneyAmount() > 0) {
            parts.add(TradePopupItem.money(text("trade.option.moneyVisual", selection.moneyAmount())));
        }
        for (Property property : selection.properties()) {
            parts.add(TradePopupItem.property(
                    property,
                    true,
                    editingOfferSide == null ? null : onOpenEditor.apply(removeTradeProperty(draft, offerSide, property), offerSide)
            ));
        }
        if (selection.jailCard()) {
            parts.add(TradePopupItem.jailCard(
                    "",
                    true,
                    editingOfferSide == null ? null : onOpenEditor.apply(toggleTradeJailCard(draft, offerSide), offerSide)
            ));
        }
        return parts;
    }

    private List<TradePopupItem> buildTradeInventoryItems(
            TradeOffer offer,
            boolean editingOfferSide,
            BiFunction<TradeDraft, Boolean, ButtonAction> onOpenEditor
    ) {
        Player editingPlayer = editingOfferSide ? offer.proposer() : offer.recipient();
        TradeSelection selection = editingOfferSide ? offer.offeredToRecipient() : offer.requestedFromRecipient();
        List<TradePopupItem> items = new ArrayList<>(editingPlayer.getOwnedProperties().stream()
                .sorted(Comparator.comparingInt(property -> property.getSpotType().ordinal()))
                .filter(this::isTradableProperty)
                .filter(property -> !selection.containsProperty(property))
                .map(property -> TradePopupItem.property(
                        property,
                        onOpenEditor.apply(
                                updateTradeSelection(draftFromOffer(offer), editingOfferSide, selection.withAddedProperty(property)),
                                editingOfferSide
                        )
                ))
                .toList());
        if (editingPlayer.hasGetOutOfJailCard() && !selection.jailCard()) {
            items.add(TradePopupItem.jailCard(
                    text("trade.option.jailCardVisual"),
                    onOpenEditor.apply(
                            updateTradeSelection(draftFromOffer(offer), editingOfferSide, selection.toggleJailCard()),
                            editingOfferSide
                    )
            ));
        }
        return items;
    }

    private boolean isTradableProperty(Property property) {
        return !(property instanceof StreetProperty streetProperty) || streetProperty.getBuildingLevel() == 0;
    }

    private TradeDraft clearTradeSelection(TradeDraft draft, boolean editingOfferSide) {
        return editingOfferSide ? draft.withOfferedToRecipient(TradeSelection.NONE) : draft.withRequestedFromRecipient(TradeSelection.NONE);
    }

    private TradeDraft updateTradeSelection(TradeDraft draft, boolean editingOfferSide, TradeSelection selection) {
        return editingOfferSide ? draft.withOfferedToRecipient(selection) : draft.withRequestedFromRecipient(selection);
    }

    private TradeDraft draftFromOffer(TradeOffer offer) {
        return new TradeDraft(offer.proposer(), offer.recipient(), offer.offeredToRecipient(), offer.requestedFromRecipient());
    }

    private TradeDraft removeTradeProperty(TradeDraft draft, boolean offerSide, Property property) {
        TradeSelection selection = offerSide ? draft.offeredToRecipient() : draft.requestedFromRecipient();
        return updateTradeSelection(draft, offerSide, selection.withRemovedProperty(property));
    }

    private TradeDraft toggleTradeJailCard(TradeDraft draft, boolean offerSide) {
        TradeSelection selection = offerSide ? draft.offeredToRecipient() : draft.requestedFromRecipient();
        return updateTradeSelection(draft, offerSide, selection.toggleJailCard());
    }
}
