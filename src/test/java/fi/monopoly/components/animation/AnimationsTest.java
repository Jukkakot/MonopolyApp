package fi.monopoly.components.animation;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Drawable;
import fi.monopoly.utils.Coordinates;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationsTest {

    @Test
    void addAnimationMarksCollectionRunning() {
        Animations animations = new Animations();

        animations.addAnimation(animationFor(new TestDrawable(new Coordinates(0, 0))));

        assertTrue(animations.isRunning());
    }

    @Test
    void updateAnimationsRemovesFinishedAnimations() {
        Animations animations = new Animations();
        animations.addAnimation(new Animation(new TestDrawable(new Coordinates(0, 0)), new FixedPath(List.of(), new Coordinates(1, 1)), null));

        animations.updateAnimations();

        assertFalse(animations.isRunning());
    }

    @Test
    void addingAnimationForSameDrawableFinishesPreviousOne() {
        Animations animations = new Animations();
        TestDrawable drawable = new TestDrawable(new Coordinates(0, 0));
        TestCallbackAction firstCallback = new TestCallbackAction();

        animations.addAnimation(new Animation(drawable, new FixedPath(List.of(new Coordinates(10, 0)), new Coordinates(5, 5)), firstCallback));
        animations.addAnimation(new Animation(drawable, new FixedPath(List.of(new Coordinates(20, 0)), new Coordinates(6, 6)), null));

        assertTrue(firstCallback.called);
    }

    @Test
    void finishAllAnimationsClearsCollection() {
        Animations animations = new Animations();
        animations.addAnimation(animationFor(new TestDrawable(new Coordinates(0, 0))));

        animations.finishAllAnimations();

        assertFalse(animations.isRunning());
    }

    @Test
    void finishAllAnimationsKeepsFollowUpAnimationsQueuedFromCallbacks() {
        Animations animations = new Animations();
        TestDrawable firstDrawable = new TestDrawable(new Coordinates(0, 0));
        TestDrawable followUpDrawable = new TestDrawable(new Coordinates(5, 5));

        animations.addAnimation(new Animation(
                firstDrawable,
                new FixedPath(List.of(new Coordinates(10, 0)), new Coordinates(10, 0)),
                () -> animations.addAnimation(animationFor(followUpDrawable))
        ));

        animations.finishAllAnimations();

        assertTrue(animations.isRunning());
    }

    private Animation animationFor(TestDrawable drawable) {
        return new Animation(drawable, new FixedPath(List.of(new Coordinates(10, 0)), new Coordinates(10, 0)), null);
    }

    private static final class TestCallbackAction implements CallbackAction {
        private boolean called = false;

        @Override
        public void doAction() {
            called = true;
        }
    }

    private static final class TestDrawable implements Drawable {
        private Coordinates coords;

        private TestDrawable(Coordinates coords) {
            this.coords = coords;
        }

        @Override
        public void draw(Coordinates c) {
        }

        @Override
        public void setHovered(boolean isHovered) {
        }

        @Override
        public boolean isHovered() {
            return false;
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

    private record FixedPath(List<Coordinates> steps, Coordinates last) implements AnimationPath {
        @Override
        public void removePrevious() {
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
