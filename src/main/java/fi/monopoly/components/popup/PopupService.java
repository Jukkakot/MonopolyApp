package fi.monopoly.components.popup;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.popup.components.ButtonProps;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Supplier;

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
        enqueue(() -> {
            OkPopup okPopup = getInstance(OkPopup.class);
            okPopup.setOnOkAction(null);
            okPopup.setPopupText(text);
            return okPopup;
        });
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

    public void show(String text, ButtonProps... buttonProps) {
        enqueue(() -> {
            CustomPopup customPopup = getInstance(CustomPopup.class);
            customPopup.setPopupText(text);
            customPopup.setButtons(buttonProps);
            return customPopup;
        });
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
                CustomPopup.class, () -> new CustomPopup(runtime)
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
        popupHistory.addFirst(message);
        while (popupHistory.size() > MAX_HISTORY_ENTRIES) {
            popupHistory.removeLast();
        }
    }
}
