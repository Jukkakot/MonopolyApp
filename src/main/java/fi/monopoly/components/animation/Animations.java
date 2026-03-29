package fi.monopoly.components.animation;

import java.util.ArrayList;
import java.util.List;

public class Animations {

    private final List<Animation> animationList = new ArrayList<>();

    public void addAnimation(Animation animation) {
        List<Animation> sameAnim = animationList.stream().filter(a -> a.getDrawable().equals(animation.getDrawable())).toList();
        if (!sameAnim.isEmpty()) {
            Animation anim = sameAnim.get(0);
            anim.finishAnimation();
            animationList.remove(anim);
        }
        animationList.add(animation);

    }

    public void updateAnimations() {
        animationList.removeIf(a -> !a.updateAnimation());
    }

    public void finishAllAnimations() {
        // Finish only the animations that were queued at the start of this call.
        // Callbacks may enqueue follow-up animations (for example forced moves after landing),
        // and those must remain queued instead of being wiped by a blanket clear().
        List<Animation> pendingAtStart = new ArrayList<>(animationList);
        for (Animation animation : pendingAtStart) {
            animation.finishAnimation();
            animationList.remove(animation);
        }
    }

    public boolean isRunning() {
        return !animationList.isEmpty();
    }
}
