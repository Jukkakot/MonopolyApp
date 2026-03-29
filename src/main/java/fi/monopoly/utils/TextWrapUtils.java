package fi.monopoly.utils;

import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

public final class TextWrapUtils {
    private TextWrapUtils() {
    }

    public static List<String> wrapText(PGraphics graphics, String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (graphics == null || text == null || text.isBlank() || maxWidth <= 0) {
            return lines;
        }
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
        return lines;
    }
}
