package fi.monopoly.components.dices;

import fi.monopoly.utils.SpotProps;
import fi.monopoly.images.Image;

public class Dice extends Image {
    private int value = 1;

    public Dice(SpotProps sp) {
        super(sp, null);
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
