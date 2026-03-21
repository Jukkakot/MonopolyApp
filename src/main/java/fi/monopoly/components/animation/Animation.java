package fi.monopoly.components.animation;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Drawable;
import fi.monopoly.utils.Coordinates;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Animation {
    @Getter
    private final Drawable drawable;
    private final AnimationPath path;
    private final static float MIN_ANIM_DISTANCE = 2;
    private final static float ANIMATION_SPEED = 0.4f;
    private final CallbackAction onAnimationEnd;

    public Animation(Drawable drawable, AnimationPath path, CallbackAction onAnimationEnd) {
        this.drawable = drawable;
        this.path = path;
        this.onAnimationEnd = onAnimationEnd;
    }

    public void finishAnimation() {
        if (!path.isEmpty()) {
            drawable.setCoords(path.getLast());
        }
        if (onAnimationEnd != null) {
            onAnimationEnd.doAction();
        }
    }

    public boolean updateAnimation() {
        if (path.isEmpty()) {
            finishAnimation();
            return false;
        }

        Coordinates goalCoords = getGoalCoords();
        if (goalCoords != null) {
            moveTowardsCoords(goalCoords);
        } else {
            finishAnimation();
            return false;
        }

        return true;
    }

    private Coordinates getGoalCoords() {
        if (path.isEmpty()) {
            return null;
        }

        Coordinates goalCoords = path.getNext();
        if (!isOverMinDistance(goalCoords)) {
            path.removePrevious();
            goalCoords = getGoalCoords();
        }

        return goalCoords;
    }

    private boolean isOverMinDistance(Coordinates goalCoords) {
        return drawable.getCoords().getDistance(goalCoords) > MIN_ANIM_DISTANCE;
    }

    private void moveTowardsCoords(Coordinates goalCoords) {
        Coordinates newCoords = getNextAnimationCoords(drawable.getCoords(), goalCoords);
        drawable.setCoords(newCoords);
    }

    static Coordinates getNextAnimationCoords(Coordinates currCoords, Coordinates goalCoords) {
        float dx = goalCoords.x() - currCoords.x();
        float dy = goalCoords.y() - currCoords.y();
        if (Math.abs(dx) <= MIN_ANIM_DISTANCE && Math.abs(dy) <= MIN_ANIM_DISTANCE) {
//            log.warn("No movement?");
        }
        return currCoords.move(dx * ANIMATION_SPEED, dy * ANIMATION_SPEED);
    }
}
