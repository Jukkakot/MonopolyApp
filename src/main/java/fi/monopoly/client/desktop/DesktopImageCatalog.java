package fi.monopoly.client.desktop;

import fi.monopoly.components.PlayerToken;
import fi.monopoly.types.SpotType;
import fi.monopoly.utils.MonopolyUtils;
import javafx.scene.paint.Color;
import processing.core.PImage;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Desktop-only image registry and tint cache for the Processing client.
 *
 * <p>This pulls image ownership out of {@link MonopolyApp} so the app class can stay focused on
 * Processing lifecycle concerns instead of also acting as a global asset registry.</p>
 */
public final class DesktopImageCatalog {
    private static final Map<String, PImage> IMAGES = new HashMap<>();
    private static final Map<String, PImage> TINTED_IMAGE_CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PImage> eldest) {
            return size() > 128;
        }
    };
    private static MonopolyApp app;
    private static long coloredImageCopies;

    private DesktopImageCatalog() {
    }

    public static void initialize(MonopolyApp app) {
        DesktopImageCatalog.app = app;
        coloredImageCopies = 0;
        IMAGES.clear();
        TINTED_IMAGE_CACHE.clear();
        String dirPath = "src/main/resources/img/";
        for (String fileName : listFiles(dirPath)) {
            IMAGES.put(fileName, app.loadImage(dirPath + fileName));
        }
        IMAGES.get("BigToken.png").resize(PlayerToken.PLAYER_TOKEN_BIG_DIAMETER, PlayerToken.PLAYER_TOKEN_BIG_DIAMETER);
        IMAGES.get("BigTokenHover.png").resize(PlayerToken.PLAYER_TOKEN_BIG_DIAMETER, PlayerToken.PLAYER_TOKEN_BIG_DIAMETER);
        IMAGES.get("BigTokenPressed.png").resize(PlayerToken.PLAYER_TOKEN_BIG_DIAMETER, PlayerToken.PLAYER_TOKEN_BIG_DIAMETER);
        app.println("Finished loading", IMAGES.size(), "images.");
    }

    public static PImage getImage(String name, Color color) {
        PImage image = IMAGES.get(name);
        if (image == null) {
            return null;
        }
        if (color == null) {
            return image;
        }
        int tintColor = MonopolyUtils.toColor(requireApp(), color);
        String cacheKey = name + "#" + tintColor;
        PImage cachedImage = TINTED_IMAGE_CACHE.get(cacheKey);
        if (cachedImage != null) {
            return cachedImage;
        }
        PImage tintedImage = getColoredCopy(image, tintColor);
        TINTED_IMAGE_CACHE.put(cacheKey, tintedImage);
        return tintedImage;
    }

    public static PImage getImage(String name) {
        return getImage(name, null);
    }

    public static PImage getImage(SpotType spotType) {
        PImage image = getImage(spotType.streetType.imgName);
        if (image == null) {
            String imgName = spotType.streetType.imgName;
            if (imgName != null) {
                image = getImage(imgName.substring(0, imgName.indexOf(".")) + spotType.id + imgName.substring(imgName.indexOf(".")));
            }
        }
        return image;
    }

    public static long getColoredImageCopies() {
        return coloredImageCopies;
    }

    private static PImage getColoredCopy(PImage img, int color) {
        coloredImageCopies++;
        MonopolyApp app = requireApp();
        PImage result = app.createImage(img.width, img.height, MonopolyApp.RGB);
        for (int i = 0; i < img.pixels.length; i++) {
            result.pixels[i] = MonopolyApp.blendColor(img.pixels[i], color, MonopolyApp.DARKEST);
        }
        result.mask(img);
        result.updatePixels();
        return result;
    }

    private static MonopolyApp requireApp() {
        if (app == null) {
            throw new IllegalStateException("DesktopImageCatalog has not been initialized yet");
        }
        return app;
    }

    private static List<String> listFiles(String dir) {
        return Stream.of(Objects.requireNonNull(new File(dir).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .toList();
    }
}
