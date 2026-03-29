package fi.monopoly.components.computer;

import java.util.List;

public record PopupView(
        String type,
        String message,
        List<String> actions,
        PropertyView offeredProperty
) {
    public PopupView {
        actions = List.copyOf(actions);
    }
}
