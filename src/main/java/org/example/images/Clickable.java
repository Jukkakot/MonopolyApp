package org.example.images;

import lombok.Getter;
import lombok.ToString;
import org.example.MonopolyApp;
import org.example.components.event.MonopolyEventListener;
import org.example.types.SpotType;
import processing.event.Event;
import processing.event.MouseEvent;

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
        boolean eventConsumed = false;
        if (image.isHovered() && event instanceof MouseEvent mouseEvent) {
            if (MouseEvent.CLICK == mouseEvent.getAction()) {
                onClick();
                eventConsumed = true;
            }
        }
        return eventConsumed;
    }

    public abstract void onClick();
}
