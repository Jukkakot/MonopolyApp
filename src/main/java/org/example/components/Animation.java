package org.example.components;

import lombok.Getter;
import org.example.utils.Coordinates;

import java.util.List;

public class Animation {
    @Getter
    private final Drawable drawable;
    private final List<Coordinates> path;
    private final static float MIN_ANIM_DISTANCE = 2;
    private final static double ANIMATION_SPEED = 0.2;
    private Action endAction;
    public Animation(Drawable drawable, List<Coordinates> path, Action endAction) {
        this.drawable = drawable;
        this.path = path;
        this.endAction = endAction;
    }

    public void finishAnimation() {
        if (path.size() >= 1) {
            drawable.setCoords(path.get(path.size() - 1));
        }
        endAction.doAction();
    }

    public boolean updateAnimation() {
        if (path.isEmpty()){
            finishAnimation();
            return false;
        }

        Coordinates currCoords = drawable.getCoords();
        Coordinates goalCoords = path.get(0);
        float dx = goalCoords.x() - currCoords.x();
        float dy = goalCoords.y() - currCoords.y();

        if (currCoords.getDistance(goalCoords) < MIN_ANIM_DISTANCE) {
            path.remove(goalCoords);
            updateAnimation();
        } else {
            drawable.setCoords(new Coordinates((float) (currCoords.x() + (dx * ANIMATION_SPEED)), (float) (currCoords.y() + (dy * ANIMATION_SPEED))));
        }
        if (path.isEmpty()){
            finishAnimation();
            return false;
        }
        return true;
    }
}
