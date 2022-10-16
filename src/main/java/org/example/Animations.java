package org.example;

import java.util.ArrayList;
import java.util.List;

public class Animations {

    private final List<Animation> animationList = new ArrayList<>();

    public void addAnimation(Animation animation) {
        List<Animation> sameAnim = animationList.stream().filter(a -> a.getDrawable().equals(animation.getDrawable())).toList();
        if (sameAnim.size() > 0) {
            Animation anim = sameAnim.get(0);
            anim.finishAnimation();
            animationList.remove(anim);
        }
        animationList.add(animation);

    }

    public void updateAnimations() {
        animationList.removeIf(a -> !a.updateAnimation());
    }

    public boolean isRunning() {
        return animationList.size() > 0;
    }
}
