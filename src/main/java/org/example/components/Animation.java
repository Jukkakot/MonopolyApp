package org.example.components;

import lombok.Getter;
import org.example.Drawable;
import org.example.CallbackAction;
import org.example.utils.Coordinates;

import java.util.List;

public class Animation {
    @Getter
    private final Drawable drawable;
    private final List<Coordinates> path;
    private final static float MIN_ANIM_DISTANCE = 2;
    private final static double ANIMATION_SPEED = 0.4;
    private final CallbackAction endCallback;

    public Animation(Drawable drawable, List<Coordinates> path, CallbackAction endCallback) {
        this.drawable = drawable;
        this.path = path;
        this.endCallback = endCallback;
    }

    public void finishAnimation() {
        if (path.size() >= 1) {
            drawable.setCoords(path.get(path.size() - 1));
        }
        if (endCallback != null) {
            endCallback.doAction();
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

        Coordinates goalCoords = path.get(0);

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
        drawable.setCoords(Coordinates.of(currCoords.x() + (dx * ANIMATION_SPEED), currCoords.y() + (dy * ANIMATION_SPEED)));
    }
}
