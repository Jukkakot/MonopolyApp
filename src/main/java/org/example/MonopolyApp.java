package org.example;

import controlP5.ControlP5;
import javafx.scene.paint.Color;
import org.example.components.Token;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

import static org.example.utils.Utils.toColor;

public class MonopolyApp extends PApplet {
    public static MonopolyApp self;
    Game game;
    public ControlP5 p5;
    private static Map<String, PImage> IMAGES = new HashMap<>();
    public static PFont font10, font20, font30;

    public MonopolyApp() {
        self = this;
    }

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
        game = new Game();
    }

    public void draw() {
        background(205, 230, 209);
        game.draw();
    }

    /**
     * @param name  name of the image
     * @param color color the image should be tinted
     * @return Instance of an image. If color is given, then it returns a copy of the image tinted with the given color
     */
    public static PImage getImage(String name, Color color) {
        PImage image = IMAGES.get(name);
        if (image == null) {
            System.out.println("No image found with name: " + name);
            return null;
        }
        if (color != null) {
            return getColoredCopy(image, toColor(color));
        }
        return image;
    }

    private static PImage getColoredCopy(PImage img, int color) {
        PImage result = self.createImage(img.width, img.height, RGB);
        for (int i = 0; i < img.pixels.length; i++) {
            int pixel = blendColor(img.pixels[i], color, DARKEST);
            result.pixels[i] = pixel;
        }
        result.mask(img);
        result.updatePixels();
        return result;
    }

    public static PImage getImage(String name) {
        return getImage(name, null);
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

    private List<String> listFiles(String dir) {
        return Stream.of(Objects.requireNonNull(new File(dir).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .toList();
    }
}
