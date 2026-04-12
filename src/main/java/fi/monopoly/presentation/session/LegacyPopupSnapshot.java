package fi.monopoly.presentation.session;

import fi.monopoly.components.popup.PopupService;

import java.util.List;

public record LegacyPopupSnapshot(
        String kind,
        String message,
        List<String> actions
) {
    public LegacyPopupSnapshot {
        actions = List.copyOf(actions);
    }

    public static LegacyPopupSnapshot fromPopupService(PopupService popupService) {
        if (popupService == null || !popupService.isAnyVisible()) {
            return null;
        }
        return new LegacyPopupSnapshot(
                popupService.activePopupKind(),
                popupService.activePopupMessage(),
                popupService.activePopupActions()
        );
    }
}
