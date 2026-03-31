package fi.monopoly.images;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.GameSession;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.types.SpotType;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import processing.event.Event;
import processing.event.MouseEvent;

@Slf4j
@ToString(callSuper = true)
public abstract class AbstractClickable implements MonopolyEventListener, Clickable {
    protected final MonopolyRuntime runtime;
    @Getter
    protected final Image image;

    public AbstractClickable(MonopolyRuntime runtime, SpotType spotType) {
        this(runtime, ImageFactory.getImage(runtime, null, spotType));
    }

    public AbstractClickable(Image image) {
        this(image.runtime, image);
    }

    public AbstractClickable(MonopolyRuntime runtime, Image image) {
        this.runtime = runtime;
        runtime.eventBus().addListener(this);
        this.image = image;
    }

    @Override
    public final boolean onEvent(Event event) {
        GameSession session = runtime.gameSessionOrNull();
        if (session != null && session.animations() != null && session.animations().isRunning()) {
            return false;
        }
        if (runtime.popupService().isAnyVisible()) {
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
