package org.example;

import controlP5.ControlP5;
import javafx.scene.paint.Color;
import org.example.components.Game;
import org.example.components.Token;
import org.example.components.event.MonopolyEventListener;
import org.example.types.SpotType;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

import static org.example.utils.Utils.toColor;

public class MonopolyApp extends PApplet {
    public static MonopolyApp self;
    private Game game;
    public static ControlP5 p5;
    private static Map<String, PImage> IMAGES = new HashMap<>();
    public static PFont font10, font20, font30;
    private static final Set<MonopolyEventListener> eventListeners = new HashSet<>();
    public static final char ENTER = '\n';
    public static final char SPACE = ' ';

    public MonopolyApp() {
        self = this;
    }

    public void settings() {
        size(1700, 996);
    }

    public void setup() {
        initImages();
        p5 = new ControlP5(this);
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

    public static PImage getImage(SpotType spotType) {
        PImage image = getImage(spotType.streetType.imgName, null);
        if (image == null) {
            String imgName = spotType.streetType.imgName;
            image = getImage(imgName.substring(0, imgName.indexOf(".")) + spotType.id + imgName.substring(imgName.indexOf(".")), null);
        }
        return image;
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

    public static void addListener(MonopolyEventListener listener) {
        eventListeners.add(listener);
    }
    @Override
    public void keyPressed(KeyEvent keyEvent) {
        super.keyPressed(keyEvent);
        sendEvent(keyEvent);
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        super.mouseClicked(mouseEvent);
        sendEvent(mouseEvent);
    }

    private void sendEvent(Event event) {
        eventListeners.forEach(eventListener -> eventListener.onEvent(event));
    }
}
