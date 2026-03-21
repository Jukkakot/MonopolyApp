package fi.monopoly.components.animation;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Drawable;
import fi.monopoly.utils.Coordinates;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnimationTest {

    @Test
    void getNextAnimationCoordsMovesFortyPercentTowardsGoal() {
        Coordinates next = Animation.getNextAnimationCoords(new Coordinates(0, 0), new Coordinates(10, 20));

        assertEquals(new Coordinates(4, 8, 0), next);
    }

    @Test
    void updateAnimationMovesDrawableTowardsCurrentGoal() {
        TestDrawable drawable = new TestDrawable(new Coordinates(0, 0));
        TestAnimationPath path = new TestAnimationPath(List.of(new Coordinates(10, 0)), new Coordinates(10, 0));
        Animation animation = new Animation(drawable, path, null);

        boolean running = animation.updateAnimation();

        assertTrue(running);
        assertEquals(new Coordinates(4, 0, 0), drawable.getCoords());
    }

    @Test
    void updateAnimationSkipsReachedPointsAndContinuesToNextGoal() {
        TestDrawable drawable = new TestDrawable(new Coordinates(0, 0));
        TestAnimationPath path = new TestAnimationPath(List.of(new Coordinates(1, 1), new Coordinates(10, 0)), new Coordinates(10, 0));
        Animation animation = new Animation(drawable, path, null);

        boolean running = animation.updateAnimation();

        assertTrue(running);
        assertEquals(1, path.removePreviousCalls);
        assertEquals(new Coordinates(4, 0, 0), drawable.getCoords());
    }

    @Test
    void updateAnimationFinishesWhenPathIsEmpty() {
        TestDrawable drawable = new TestDrawable(new Coordinates(0, 0));
        TestAnimationPath path = new TestAnimationPath(List.of(), new Coordinates(7, 7));
        TestCallbackAction callback = new TestCallbackAction();
        Animation animation = new Animation(drawable, path, callback);

        boolean running = animation.updateAnimation();

        assertFalse(running);
        assertEquals(1, callback.callCount);
    }

    @Test
    void finishAnimationMovesDrawableToLastCoordinateAndCallsCallback() {
        TestDrawable drawable = new TestDrawable(new Coordinates(0, 0));
        TestAnimationPath path = new TestAnimationPath(List.of(new Coordinates(10, 0)), new Coordinates(7, 7));
        TestCallbackAction callback = new TestCallbackAction();
        Animation animation = new Animation(drawable, path, callback);

        animation.finishAnimation();

        assertEquals(new Coordinates(7, 7, 0), drawable.getCoords());
        assertEquals(1, callback.callCount);
    }

    private static final class TestCallbackAction implements CallbackAction {
        private int callCount = 0;

        @Override
        public void doAction() {
            callCount++;
        }
    }

    private static final class TestDrawable implements Drawable {
        private Coordinates coords;
        private boolean hovered;

        private TestDrawable(Coordinates coords) {
            this.coords = coords;
        }

        @Override
        public void draw(Coordinates c) {
        }

        @Override
        public boolean isHovered() {
            return hovered;
        }

        @Override
        public void setHovered(boolean isHovered) {
            hovered = isHovered;
        }

        @Override
        public Coordinates getCoords() {
            return coords;
        }

        @Override
        public void setCoords(Coordinates coords) {
            this.coords = coords;
        }

        @Override
        public Coordinates move(Coordinates coords) {
            return this.coords.move(coords);
        }
    }

    private static final class TestAnimationPath implements AnimationPath {
        private final List<Coordinates> steps;
        private final Coordinates last;
        private int removePreviousCalls = 0;

        private TestAnimationPath(List<Coordinates> steps, Coordinates last) {
            this.steps = new ArrayList<>(steps);
            this.last = last;
        }

        @Override
        public void removePrevious() {
            removePreviousCalls++;
            if (!steps.isEmpty()) {
                steps.remove(0);
            }
        }

        @Override
        public boolean isEmpty() {
            return steps.isEmpty();
        }

        @Override
        public Coordinates getLast() {
            return last;
        }

        @Override
        public Coordinates getNext() {
            return steps.get(0);
        }
    }
}
