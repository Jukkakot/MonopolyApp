package fi.monopoly.utils;

import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TextWrapUtils {
    private static final int MAX_CACHE_ENTRIES = 256;
    private static final Map<WrapCacheKey, List<String>> WRAP_CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<WrapCacheKey, List<String>> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };

    private TextWrapUtils() {
    }

    public static List<String> wrapText(PGraphics graphics, String text, float maxWidth) {
        return wrapText(graphics, text, maxWidth, "default");
    }

    public static List<String> wrapText(PGraphics graphics, String text, float maxWidth, String cacheScope) {
        if (graphics == null || text == null || text.isBlank() || maxWidth <= 0) {
            return List.of();
        }
        WrapCacheKey cacheKey = new WrapCacheKey(cacheScope, text, Math.round(maxWidth));
        List<String> cachedLines = WRAP_CACHE.get(cacheKey);
        if (cachedLines != null) {
            return cachedLines;
        }

        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R", -1)) {
            if (paragraph.isBlank()) {
                lines.add("");
                continue;
            }
            String[] words = paragraph.trim().split("\\s+");
            String currentLine = words[0];
            for (int i = 1; i < words.length; i++) {
                String candidate = currentLine + " " + words[i];
                if (graphics.textWidth(candidate) <= maxWidth) {
                    currentLine = candidate;
                } else {
                    lines.add(currentLine);
                    currentLine = words[i];
                }
            }
            lines.add(currentLine);
        }
        List<String> immutableLines = List.copyOf(lines);
        WRAP_CACHE.put(cacheKey, immutableLines);
        return immutableLines;
    }

    public static void clearCache() {
        WRAP_CACHE.clear();
    }

    private record WrapCacheKey(
            String scope,
            String text,
            int width
    ) {
    }
}
