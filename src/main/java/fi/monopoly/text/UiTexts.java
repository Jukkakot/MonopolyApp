package fi.monopoly.text;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public final class UiTexts {
    private static final Properties POPUP_TEXTS = loadProperties();

    private UiTexts() {
    }

    public static String text(String key, Object... args) {
        String template = POPUP_TEXTS.getProperty(key);
        if (template == null) {
            throw new IllegalArgumentException("Missing popup text for key: " + key);
        }
        String result = template;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", Objects.toString(args[i]));
        }
        return result;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream stream = UiTexts.class.getResourceAsStream("/PopupTexts.properties")) {
            if (stream == null) {
                throw new IllegalStateException("PopupTexts.properties not found");
            }
            properties.load(stream);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load PopupTexts.properties", e);
        }
    }
}
