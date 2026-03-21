package fi.monopoly.components.dices;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.images.Image;
import fi.monopoly.utils.SpotProps;

public class Dice extends Image {
    private int value = 1;

    public Dice(MonopolyRuntime runtime, SpotProps sp) {
        super(runtime, sp, null);
        setImgName(getDiceImgName());
    }

    public int roll() {
        value = (int) (Math.random() * 6) + 1;
        setImgName(getDiceImgName());
        return value;
    }

    private String getDiceImgName() {
        return "Dice" + value + ".png";
    }
}
