package org.example;

import controlP5.ControlP5;
import org.example.components.Token;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class MonopolyApp extends PApplet {
    Game game;
    public ControlP5 p5;
    public static Map<String, PImage> IMAGES = new HashMap<>();
    public static PFont font10, font20, font30;

    public void settings() {
        size(1700, 996);
    }

    public void setup() {
        p5 = new ControlP5(this);
        initImages();
        font10 = createFont("Monopoly Regular.ttf", 10);
        font20 = createFont("Monopoly Regular.ttf", 20);
        font30 = createFont("Monopoly Regular.ttf", 30);
        textFont(font10);
        game = new Game(this);
    }

    public void draw() {
        background(205, 230, 209);
        game.draw();
    }

    private void initImages() {
        String dirPath = "src/main/resources/img/";
        List<String> fileNames = listFiles(dirPath);
        for (String fileName : fileNames) {
            IMAGES.put(fileName, loadImage(dirPath + fileName));
        }
        IMAGES.get("BigToken.png").resize(Token.TOKEN_RADIUS * 2, Token.TOKEN_RADIUS * 2);
        IMAGES.get("BigTokenHover.png").resize(Token.TOKEN_RADIUS * 2, Token.TOKEN_RADIUS * 2);
        IMAGES.get("BigTokenPressed.png").resize(Token.TOKEN_RADIUS * 2, Token.TOKEN_RADIUS * 2);
        println("Finished loading", IMAGES.size(), "images.");
    }

    public List<String> listFiles(String dir) {
        return Stream.of(Objects.requireNonNull(new File(dir).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .toList();
    }
}
