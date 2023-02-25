package org.example.components.animation;

import lombok.Getter;
import org.example.components.Drawable;
import org.example.components.CallbackAction;
import org.example.components.board.Path;
import org.example.utils.Coordinates;

public class Animation {
    @Getter
    private final Drawable drawable;
    private final Path path;
    private final static float MIN_ANIM_DISTANCE = 2;
    private final static float ANIMATION_SPEED = 0.4f;
    private final CallbackAction onAnimationEnd;

    public Animation(Drawable drawable, Path path, CallbackAction onAnimationEnd) {
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
        if (path.isEmpty()) return null;

        Coordinates goalCoords = path.getNext();

        if (!isOverMinDistance(goalCoords)) {
            path.remove(goalCoords);
            goalCoords = getGoalCoords();
        }

        return goalCoords;
    }

    private boolean isOverMinDistance(Coordinates goalCoords) {
        return drawable.getCoords().getDistance(goalCoords) > MIN_ANIM_DISTANCE;
    }

    private void moveTowardsCoords(Coordinates goalCoords) {
        Coordinates currCoords = drawable.getCoords();
        float dx = goalCoords.x() - currCoords.x();
        float dy = goalCoords.y() - currCoords.y();
        Coordinates newCoords = currCoords.move(dx * ANIMATION_SPEED, dy * ANIMATION_SPEED);
        drawable.setCoords(newCoords);
    }
}
