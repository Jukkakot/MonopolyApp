package org.example.components;

import org.example.MonopolyApp;
import org.example.utils.Coordinates;

public class MonopolyButton extends controlP5.Button {

    public MonopolyButton(String id) {
        super(MonopolyApp.p5, id);
        setFont(MonopolyApp.font20);
    }

    public controlP5.Button setPosition(Coordinates coordinates) {
        return setPosition(coordinates.x(), coordinates.y());
    }
}
