package org.example;

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
    public static Map<String, PImage> IMAGES = new HashMap<>();
    public static PFont font;
    public void settings() {
        size(1700, 1000);
    }

    public void setup() {
        initImages();
        font = createFont("Monopoly Regular.ttf",10);
        textFont(font);
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
        println("Finished loading", IMAGES.size(), "images.");
    }

    public List<String> listFiles(String dir) {
        return Stream.of(Objects.requireNonNull(new File(dir).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .toList();
    }
}
