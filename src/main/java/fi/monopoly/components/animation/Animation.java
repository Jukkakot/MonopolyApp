package fi.monopoly.components.animation;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Drawable;
import fi.monopoly.utils.Coordinates;
import lombok.Getter;

public class Animation {
    public static final float REFERENCE_FRAME_SECONDS = 1f / 60f;
    @Getter
    private final Drawable drawable;
    private final AnimationPath path;
    private final static float MIN_ANIM_DISTANCE = 2;
    // Larger value moves farther per reference frame and makes animations faster; smaller value slows them down.
    private final static float ANIMATION_SPEED = 0.3f;
    private final CallbackAction onAnimationEnd;
    private boolean finished;

    public Animation(Drawable drawable, AnimationPath path, CallbackAction onAnimationEnd) {
        this.drawable = drawable;
        this.path = path;
        this.onAnimationEnd = onAnimationEnd;
    }

    public void finishAnimation() {
        if (finished) {
            return;
        }
        finished = true;
        if (!path.isEmpty()) {
            drawable.setCoords(path.getLast());
        }
        if (onAnimationEnd != null) {
            onAnimationEnd.doAction();
        }
    }

    public boolean updateAnimation() {
        return updateAnimation(REFERENCE_FRAME_SECONDS);
    }

    public boolean updateAnimation(float deltaSeconds) {
        if (finished) {
            return false;
        }
        if (path.isEmpty()) {
            finishAnimation();
            return false;
        }

        Coordinates goalCoords = getGoalCoords();
        if (goalCoords != null) {
            moveTowardsCoords(goalCoords, deltaSeconds);
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

    private void moveTowardsCoords(Coordinates goalCoords, float deltaSeconds) {
        Coordinates newCoords = getNextAnimationCoords(drawable.getCoords(), goalCoords, deltaSeconds);
        drawable.setCoords(newCoords);
    }

    static Coordinates getNextAnimationCoords(Coordinates currCoords, Coordinates goalCoords) {
        return getNextAnimationCoords(currCoords, goalCoords, REFERENCE_FRAME_SECONDS);
    }

    static Coordinates getNextAnimationCoords(Coordinates currCoords, Coordinates goalCoords, float deltaSeconds) {
        float dx = goalCoords.x() - currCoords.x();
        float dy = goalCoords.y() - currCoords.y();
        if (Math.abs(dx) <= MIN_ANIM_DISTANCE && Math.abs(dy) <= MIN_ANIM_DISTANCE) {
//            log.warn("No movement?");
        }
        float speedFactor = Math.max(0f, deltaSeconds / REFERENCE_FRAME_SECONDS);
        return currCoords.move(dx * ANIMATION_SPEED * speedFactor, dy * ANIMATION_SPEED * speedFactor);
    }
}
