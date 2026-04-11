package fi.monopoly.components.popup;

import fi.monopoly.components.Player;

import java.util.Arrays;
import java.util.stream.Collectors;

final class PopupPerspectiveText {

    private PopupPerspectiveText() {
    }

    static String adaptForTurnPlayer(String text, Player turnPlayer) {
        if (text == null || text.isBlank() || turnPlayer == null || !turnPlayer.isComputerControlled()) {
            return text;
        }
        String playerName = turnPlayer.getName();
        return Arrays.stream(text.split("\\n", -1))
                .map(line -> adaptLine(line, playerName))
                .collect(Collectors.joining("\n"));
    }

    private static String adaptLine(String line, String playerName) {
        if (line == null || line.isBlank()) {
            return line;
        }
        String adaptedEnglish = adaptEnglishLine(line, playerName);
        if (!adaptedEnglish.equals(line)) {
            return adaptedEnglish;
        }
        String adaptedFinnish = adaptFinnishLine(line, playerName);
        if (!adaptedFinnish.equals(line)) {
            return adaptedFinnish;
        }
        if (containsSecondPersonMarker(line)) {
            return playerName + ": " + line;
        }
        return line;
    }

    private static String adaptEnglishLine(String line, String playerName) {
        return replaceSentenceAware(line,
                new ReplacementRule("Arrived at ", playerName + " arrived at "),
                new ReplacementRule("Do you want to ", "Does " + playerName + " want to "),
                new ReplacementRule("How many houses do you want to sell?", "How many houses should " + playerName + " sell?"),
                new ReplacementRule("You were not sent to jail", playerName + " was not sent to jail"),
                new ReplacementRule("You were sent to jail", playerName + " was sent to jail"),
                new ReplacementRule("You got out of jail", playerName + " got out of jail"),
                new ReplacementRule("You still have enough assets to cover the debt.", playerName + " still has enough assets to cover the debt."),
                new ReplacementRule("You still have ", playerName + " still has "),
                new ReplacementRule("You don't have", playerName + " doesn't have"),
                new ReplacementRule("You dont have", playerName + " doesn't have"),
                new ReplacementRule("You didn't have", playerName + " didn't have"),
                new ReplacementRule("You didin't have", playerName + " didn't have"),
                new ReplacementRule("You cannot", playerName + " cannot"),
                new ReplacementRule("You can't", playerName + " cannot"),
                new ReplacementRule("You need to ", playerName + " needs to "),
                new ReplacementRule("You can declare bankruptcy.", playerName + " can declare bankruptcy."),
                new ReplacementRule("You can ", playerName + " can "),
                new ReplacementRule("You are assessed", playerName + " is assessed"),
                new ReplacementRule("You have won", playerName + " has won"),
                new ReplacementRule("You have been elected", playerName + " has been elected"),
                new ReplacementRule("You inherit ", playerName + " inherits "),
                new ReplacementRule("Your building loan matures.", playerName + "'s building loan matures.")
        );
    }

    private static String adaptFinnishLine(String line, String playerName) {
        return replaceSentenceAware(line,
                new ReplacementRule("Saavuit ruutuun ", playerName + " saapui ruutuun "),
                new ReplacementRule("Haluatko ", "Haluaako pelaaja " + playerName + " "),
                new ReplacementRule("Sinulla on edelleen tarpeeksi varoja", "Pelaajalla " + playerName + " on edelleen tarpeeksi varoja"),
                new ReplacementRule("Sinulla ei ole", "Pelaajalla " + playerName + " ei ole"),
                new ReplacementRule("Sinulla on ", "Pelaajalla " + playerName + " on "),
                new ReplacementRule("Sinun taytyy", "Pelaajan " + playerName + " taytyy"),
                new ReplacementRule("Sinua ei lahetetty vankilaan", playerName + " ei joutunut vankilaan"),
                new ReplacementRule("Sinut lahetettiin vankilaan", playerName + " joutui vankilaan"),
                new ReplacementRule("Sinulla on viela ", "Pelaajalla " + playerName + " on viela "),
                new ReplacementRule("Et voi ", "Pelaaja " + playerName + " ei voi ")
        );
    }

    private static String replaceSentenceAware(String line, ReplacementRule... rules) {
        String adapted = line;
        for (ReplacementRule rule : rules) {
            adapted = replaceAtSentenceBoundary(adapted, rule.match(), rule.replace());
        }
        return adapted;
    }

    private static String replaceAtSentenceBoundary(String line, String match, String replacement) {
        if (line.startsWith(match)) {
            line = replacement + line.substring(match.length());
        }
        return line.replace(". " + match, ". " + replacement);
    }

    private static boolean containsSecondPersonMarker(String line) {
        return line.contains("You ")
                || line.contains("Your ")
                || line.contains("You'")
                || line.contains("Sinun ")
                || line.contains("Sinulla ")
                || line.contains("Sinua ")
                || line.contains("Haluatko ")
                || line.contains("Et voi ");
    }

    private record ReplacementRule(String match, String replace) {
    }
}
