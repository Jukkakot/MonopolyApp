package fi.monopoly.images;

import org.slf4j.LoggerFactory;

public interface Clickable {
    default void onClick() {
        LoggerFactory.getLogger(this.getClass()).trace("Default click action {}", getClass());
    }
}
