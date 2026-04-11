package fi.monopoly.components.popup;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.popup.components.ButtonProps;
import fi.monopoly.components.properties.Property;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Supplier;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
public class PopupService {
    private static final int MAX_HISTORY_ENTRIES = 12;
    private final MonopolyRuntime runtime;
    private final Map<Class<? extends Popup>, Popup> popupInstances = new HashMap<>();
    private final Queue<PopupRequest> pendingRequests = new ArrayDeque<>();
    private final ArrayDeque<String> popupHistory = new ArrayDeque<>();
    private Popup activePopup;

    public PopupService(MonopolyRuntime runtime) {
        this.runtime = runtime;
    }

    public boolean isAnyVisible() {
        return activePopup != null;
    }

    private static String popupName(Popup popup) {
        return popup == null ? "none" : popup.getClass().getSimpleName();
    }

    public void show(String text) {
        show(text, (ButtonAction) null);
    }

    public void show(String text, ButtonAction onAccept) {
        enqueue(() -> {
            OkPopup okPopup = getInstance(OkPopup.class);
            okPopup.setOnOkAction(onAccept);
            okPopup.setPopupText(text);
            return okPopup;
        });
    }

    public void show(String text, ButtonAction onAccept, ButtonAction onDecline) {
        enqueue(() -> {
            ChoicePopup choicePopup = getInstance(ChoicePopup.class);
            choicePopup.setPopupText(text);
            choicePopup.setOnAcceptAction(onAccept);
            choicePopup.setOnDeclineAction(onDecline);
            return choicePopup;
        });
    }

    public void showPropertyOffer(Property property, String text, ButtonAction onAccept, ButtonAction onDecline) {
        if (runtime == null) {
            show(text, onAccept, onDecline);
            return;
        }
        enqueue(() -> {
            PropertyOfferPopup propertyOfferPopup = getInstance(PropertyOfferPopup.class);
            propertyOfferPopup.setPopupText(text);
            propertyOfferPopup.setOfferedProperty(property);
            propertyOfferPopup.setOnAcceptAction(onAccept);
            propertyOfferPopup.setOnDeclineAction(onDecline);
            return propertyOfferPopup;
        });
    }

    public void showPropertyAuction(
            Property property,
            String title,
            String reasonText,
            String currentLeaderLabel,
            int currentBidAmount,
            int availableCashAmount,
            String primaryLabel,
            String secondaryLabel,
            ButtonAction onAccept,
            ButtonAction onDecline
    ) {
        if (runtime == null) {
            showManualDecision(title, new ButtonProps(primaryLabel, onAccept), new ButtonProps(secondaryLabel, onDecline));
            return;
        }
        enqueue(() -> {
            PropertyAuctionPopup propertyAuctionPopup = getInstance(PropertyAuctionPopup.class);
            propertyAuctionPopup.setPopupText(title);
            propertyAuctionPopup.setOfferedProperty(property);
            propertyAuctionPopup.setAuctionInfo(reasonText, currentLeaderLabel, currentBidAmount, availableCashAmount);
            propertyAuctionPopup.setOnAcceptAction(onAccept);
            propertyAuctionPopup.setOnDeclineAction(onDecline);
            propertyAuctionPopup.setButtonLabels(primaryLabel, secondaryLabel);
            return propertyAuctionPopup;
        });
    }

    public void show(String text, ButtonProps... buttonProps) {
        showCustom(text, true, false, buttonProps);
    }

    public void showManualDecision(String text, ButtonProps... buttonProps) {
        showCustom(text, false, true, buttonProps);
    }

    private void showCustom(String text, boolean computerResolvable, boolean manualInteractionDuringComputerTurn, ButtonProps... buttonProps) {
        enqueue(() -> {
            CustomPopup customPopup = getInstance(CustomPopup.class);
            customPopup.setPopupText(text);
            customPopup.setInteractionPolicy(computerResolvable, manualInteractionDuringComputerTurn);
            customPopup.setButtons(buttonProps);
            return customPopup;
        });
    }

    public void showTrade(String text, TradePopupView tradeView, ButtonProps... buttonProps) {
        enqueue(() -> {
            TradePopup tradePopup = getInstance(TradePopup.class);
            tradePopup.setPopupText(text);
            tradePopup.setTradeView(tradeView);
            tradePopup.setButtons(buttonProps);
            return tradePopup;
        });
    }

    public void showTrade(String text, TradePopupView tradeView, ButtonAction onAccept, ButtonAction onDecline) {
        showTrade(
                text,
                tradeView,
                new ButtonProps(text("popup.choice.accept"), onAccept),
                new ButtonProps(text("popup.choice.decline"), onDecline)
        );
    }

    public void hideAll() {
        log.debug("Hiding all popups. activePopup={}, pendingRequests={}", popupName(activePopup), pendingRequests.size());
        pendingRequests.clear();
        if (activePopup != null) {
            Popup popupToHide = activePopup;
            activePopup = null;
            popupToHide.hide();
        }
        popupInstances.values().forEach(Popup::hide);
    }

    public void showNextPending() {
        showNextIfPossible();
    }

    public List<String> recentPopupMessages() {
        return List.copyOf(popupHistory);
    }

    public boolean resolveForComputer(ComputerPlayerProfile profile) {
        if (activePopup == null) {
            return false;
        }
        return activePopup.onComputerAction(profile);
    }

    public String activePopupKind() {
        return activePopup == null ? null : activePopup.getPopupKind();
    }

    public String activePopupMessage() {
        return activePopup == null ? null : activePopup.getPopupText();
    }

    public List<String> activePopupActions() {
        return activePopup == null ? List.of() : activePopup.getVisibleActionLabels();
    }

    public boolean triggerPrimaryAction() {
        return activePopup != null && activePopup.triggerPrimaryAction(PopupActionTrigger.MANUAL);
    }

    public boolean triggerSecondaryAction() {
        return activePopup != null && activePopup.triggerSecondaryAction(PopupActionTrigger.MANUAL);
    }

    public boolean triggerPrimaryComputerAction() {
        return activePopup != null && activePopup.triggerPrimaryAction(PopupActionTrigger.COMPUTER);
    }

    public boolean triggerSecondaryComputerAction() {
        return activePopup != null && activePopup.triggerSecondaryAction(PopupActionTrigger.COMPUTER);
    }

    public Property activeOfferedProperty() {
        if (activePopup instanceof PropertyOfferPopup propertyOfferPopup) {
            return propertyOfferPopup.getOfferedProperty();
        }
        return null;
    }

    private <T extends Popup> T getInstance(Class<T> clazz) {
        return clazz.cast(popupInstances.computeIfAbsent(clazz, this::createPopup));
    }

    public void onPopupClosed(Popup popup) {
        if (popup != activePopup) {
            log.trace("Ignoring popup close for non-active popup {}", popupName(popup));
            return;
        }
        log.trace("Popup closed: {}", popupName(popup));
        activePopup = null;
    }

    private void enqueue(PopupRequest request) {
        pendingRequests.add(request);
        log.trace("Queued popup request. activePopup={}, pendingRequests={}", popupName(activePopup), pendingRequests.size());
        showNextIfPossible();
    }

    private Popup createPopup(Class<? extends Popup> clazz) {
        Map<Class<? extends Popup>, Supplier<? extends Popup>> factories = Map.of(
                OkPopup.class, () -> new OkPopup(runtime),
                ChoicePopup.class, () -> new ChoicePopup(runtime),
                PropertyOfferPopup.class, () -> new PropertyOfferPopup(runtime),
                PropertyAuctionPopup.class, () -> new PropertyAuctionPopup(runtime),
                CustomPopup.class, () -> new CustomPopup(runtime),
                TradePopup.class, () -> new TradePopup(runtime)
        );
        Supplier<? extends Popup> supplier = factories.get(clazz);
        if (supplier == null) {
            throw new IllegalArgumentException("Unsupported popup type: " + clazz.getName());
        }
        return supplier.get();
    }

    @FunctionalInterface
    private interface PopupRequest {
        Popup prepare();
    }

    private void showNextIfPossible() {
        if (activePopup != null) {
            log.trace("Popup queue blocked by active popup {}", popupName(activePopup));
            return;
        }
        PopupRequest nextRequest = pendingRequests.poll();
        if (nextRequest == null) {
            return;
        }
        activePopup = nextRequest.prepare();
        recordShownPopup(activePopup);
        log.trace("Showing popup {}. pendingRequests={}", popupName(activePopup), pendingRequests.size());
        activePopup.show();
    }

    private void recordShownPopup(Popup popup) {
        if (popup == null || popup.getPopupText() == null) {
            return;
        }
        String message = popup.getPopupText().trim();
        if (message.isEmpty()) {
            return;
        }
        String turnPrefix = activeTurnPrefix();
        popupHistory.addFirst(turnPrefix == null ? message : turnPrefix + ": " + message);
        while (popupHistory.size() > MAX_HISTORY_ENTRIES) {
            popupHistory.removeLast();
        }
    }

    private String activeTurnPrefix() {
        if (runtime == null || runtime.gameSessionOrNull() == null || runtime.gameSessionOrNull().players() == null) {
            return null;
        }
        var turnPlayer = runtime.gameSessionOrNull().players().getTurn();
        if (turnPlayer == null) {
            return null;
        }
        String turnName = turnPlayer.getName();
        return turnName == null || turnName.isBlank() ? null : turnName;
    }
}
