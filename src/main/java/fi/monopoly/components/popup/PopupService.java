package fi.monopoly.components.popup;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.popup.components.ButtonProps;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.function.Supplier;

public class PopupService {
    private final MonopolyRuntime runtime;
    private final Map<Class<? extends Popup>, Popup> popupInstances = new HashMap<>();
    private final Queue<PopupRequest> pendingRequests = new ArrayDeque<>();
    private Popup activePopup;

    public PopupService(MonopolyRuntime runtime) {
        this.runtime = runtime;
    }

    public boolean isAnyVisible() {
        return activePopup != null;
    }

    public void hideAll() {
        pendingRequests.clear();
        if (activePopup != null) {
            Popup popupToHide = activePopup;
            activePopup = null;
            popupToHide.hide();
        }
        popupInstances.values().forEach(Popup::hide);
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

    public void onPopupClosed(Popup popup) {
        if (popup != activePopup) {
            return;
        }
        activePopup = null;
    }

    public void showNextPending() {
        showNextIfPossible();
    }

    private <T extends Popup> T getInstance(Class<T> clazz) {
        return clazz.cast(popupInstances.computeIfAbsent(clazz, this::createPopup));
    }

    private void enqueue(PopupRequest request) {
        pendingRequests.add(request);
        showNextIfPossible();
    }

    private void showNextIfPossible() {
        if (activePopup != null) {
            return;
        }
        PopupRequest nextRequest = pendingRequests.poll();
        if (nextRequest == null) {
            return;
        }
        activePopup = nextRequest.prepare();
        activePopup.show();
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
}
