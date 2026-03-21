package fi.monopoly.components.animation;

import fi.monopoly.utils.Coordinates;

public interface AnimationPath {
    void removePrevious();
    boolean isEmpty();
    Coordinates getLast();
    Coordinates getNext();
}
