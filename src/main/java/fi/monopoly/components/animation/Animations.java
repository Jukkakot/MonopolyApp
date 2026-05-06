package fi.monopoly.components.animation;

import java.util.ArrayList;
import java.util.List;

public class Animations {
    private static final float MAX_ANIMATION_DELTA_SECONDS = 1f / 15f;

    private final List<Animation> animationList = new ArrayList<>();

    public void addAnimation(Animation animation) {
        List<Animation> sameAnim = animationList.stream().filter(a -> a.getDrawable().equals(animation.getDrawable())).toList();
        if (!sameAnim.isEmpty()) {
            Animation anim = sameAnim.get(0);
            animationList.remove(anim);
            anim.finishAnimation();
        }
        animationList.add(animation);

    }

    public void updateAnimations() {
        updateAnimations(Animation.REFERENCE_FRAME_SECONDS);
    }

    public void updateAnimations(float deltaSeconds) {
        float safeDeltaSeconds = Math.min(Math.max(0f, deltaSeconds), MAX_ANIMATION_DELTA_SECONDS);
        animationList.removeIf(animation -> !animation.updateAnimation(safeDeltaSeconds));
    }

    public void finishAllAnimations() {
        // Finish only the animations that were queued at the start of this call.
        // Callbacks may enqueue follow-up animations (for example forced moves after landing),
        // and those must remain queued instead of being wiped by a blanket clear().
        List<Animation> pendingAtStart = new ArrayList<>(animationList);
        for (Animation animation : pendingAtStart) {
            animationList.remove(animation);
            animation.finishAnimation();
        }
    }

    public boolean isRunning() {
        return !animationList.isEmpty();
    }
}
