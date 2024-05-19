package org.example.images;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.example.MonopolyApp;
import org.example.components.Game;
import org.example.components.event.MonopolyEventListener;
import org.example.components.popup.Popup;
import org.example.types.SpotType;
import processing.event.Event;
import processing.event.MouseEvent;

@Slf4j
@ToString(callSuper = true)
public abstract class Clickable implements MonopolyEventListener {
    @Getter
    protected final Image image;

    public Clickable(SpotType spotType) {
        this(ImageFactory.getImage(null, spotType));
    }

    public Clickable(Image image) {
        MonopolyApp.addListener(this);
        this.image = image;
    }

    @Override
    public final boolean onEvent(Event event) {
        if (Game.animations.isRunning()) {
            return false;
        }
        if (Popup.isAnyVisible()) {
            return false;
        }
        boolean eventConsumed = false;
        if (image.isHovered() && event instanceof MouseEvent mouseEvent) {
            if (MouseEvent.CLICK == mouseEvent.getAction()) {
                onClick();
                eventConsumed = true;
            }
        }
        return eventConsumed;
    }

    public void onClick() {
        log.debug("Clicked {}", getClass().getSimpleName());
    }
}
