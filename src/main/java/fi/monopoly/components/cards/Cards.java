package fi.monopoly.components.cards;

import fi.monopoly.text.UiTexts;
import fi.monopoly.types.CardType;
import fi.monopoly.types.StreetType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class Cards {
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private static final String SINGLE_TEXT_DELIMITER = "#";
    private static final String PROP_VALUES_DELIMITER = ";";
    private final StreetType streetType;
    private final List<CardDefinition> cardList = new ArrayList<>();
    private int previousCardIndex = -1;

    public Cards(StreetType streetType) {
        this.streetType = streetType;
        initCards();
        Collections.shuffle(cardList);
    }

    static String getLocalizedCardText(StreetType streetType, CardType cardType, int entryIndex) {
        List<String> localizedEntries = splitCardEntries(getBundleValue(getBundle(streetType, UiTexts.getLocale()), cardType.name()));
        if (entryIndex < localizedEntries.size()) {
            return getTextPart(localizedEntries.get(entryIndex));
        }
        List<String> defaultEntries = splitCardEntries(getBundleValue(getDefaultBundle(streetType), cardType.name()));
        if (entryIndex < defaultEntries.size()) {
            return getTextPart(defaultEntries.get(entryIndex));
        }
        throw new IllegalArgumentException("Missing card text for " + streetType + " " + cardType + " at index " + entryIndex);
    }

    private static String getTextPart(String cardEntry) {
        return cardEntry.split(PROP_VALUES_DELIMITER)[0];
    }

    private static List<String> splitCardEntries(String fullText) {
        if (fullText == null || fullText.isBlank()) {
            return List.of();
        }
        return Arrays.stream(fullText.split(SINGLE_TEXT_DELIMITER))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList();
    }

    private static ResourceBundle getBundle(StreetType streetType, Locale locale) {
        return ResourceBundle.getBundle(getBundleName(streetType), locale == null ? DEFAULT_LOCALE : locale);
    }

    private static ResourceBundle getDefaultBundle(StreetType streetType) {
        return getBundle(streetType, DEFAULT_LOCALE);
    }

    private static String getBundleValue(ResourceBundle bundle, String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    private static String getBundleName(StreetType streetType) {
        return streetType.name().toLowerCase(Locale.ROOT);
    }

    private void initCards() {
        for (CardType ct : CardType.values()) {
            List<String> entries = splitCardEntries(getBundleValue(getDefaultBundle(streetType), ct.name()));
            if (!entries.isEmpty()) {
                for (int i = 0; i < entries.size(); i++) {
                    String propSingleText = entries.get(i);
                    List<String> propParts = new ArrayList<>(Arrays.asList(propSingleText.split(PROP_VALUES_DELIMITER)));
                    propParts.remove(0);
                    cardList.add(new CardDefinition(ct, i, List.copyOf(propParts)));
                }
            }
        }
    }

    public Card getCard() {
        if (previousCardIndex == -1) {
            previousCardIndex = 0;
        } else if (previousCardIndex == cardList.size() - 1) {
            Collections.shuffle(cardList);
            log.info("Shuffling cards...");
            previousCardIndex = 0;
        } else {
            previousCardIndex++;
        }
        CardDefinition definition = cardList.get(previousCardIndex);
        return new Card(definition.cardType(), getLocalizedCardText(streetType, definition.cardType(), definition.entryIndex()), definition.values());
    }

    private record CardDefinition(CardType cardType, int entryIndex, List<String> values) {
    }
}
