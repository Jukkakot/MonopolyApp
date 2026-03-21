package fi.monopoly.images;

import fi.monopoly.MonopolyApp;
import fi.monopoly.components.Game;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.popup.Popup;
import fi.monopoly.types.SpotType;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import processing.event.Event;
import processing.event.MouseEvent;

@Slf4j
@ToString(callSuper = true)
public abstract class AbstractClickable implements MonopolyEventListener, Clickable {
    @Getter
    protected final Image image;

    public AbstractClickable(SpotType spotType) {
        this(ImageFactory.getImage(null, spotType));
    }

    public AbstractClickable(Image image) {
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


}
