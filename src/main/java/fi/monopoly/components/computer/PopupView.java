package fi.monopoly.components.computer;

import java.util.List;

public record PopupView(
        String type,
        String message,
        List<String> actions
) {
    public PopupView {
        actions = List.copyOf(actions);
    }
}
