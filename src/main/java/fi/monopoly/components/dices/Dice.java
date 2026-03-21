package fi.monopoly.components.dices;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.utils.SpotProps;
import fi.monopoly.images.Image;

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
