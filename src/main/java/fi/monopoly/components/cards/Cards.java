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
    private static final Map<StreetType, DeckState> DECKS = new EnumMap<>(StreetType.class);

    public Cards(StreetType streetType) {
        this.streetType = streetType;
        synchronized (DECKS) {
            DECKS.computeIfAbsent(streetType, Cards::createDeckState);
        }
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

    private static DeckState createDeckState(StreetType streetType) {
        List<CardDefinition> definitions = new ArrayList<>();
        for (CardType ct : CardType.values()) {
            List<String> entries = splitCardEntries(getBundleValue(getDefaultBundle(streetType), ct.name()));
            if (!entries.isEmpty()) {
                for (int i = 0; i < entries.size(); i++) {
                    String propSingleText = entries.get(i);
                    List<String> propParts = new ArrayList<>(Arrays.asList(propSingleText.split(PROP_VALUES_DELIMITER)));
                    propParts.remove(0);
                    definitions.add(new CardDefinition(ct, i, List.copyOf(propParts)));
                }
            }
        }
        Collections.shuffle(definitions);
        return new DeckState(definitions);
    }

    public Card getCard() {
        DeckState deckState = getDeckState();
        CardDefinition definition = deckState.drawNext();
        return new Card(definition.cardType(), getLocalizedCardText(streetType, definition.cardType(), definition.entryIndex()), definition.values());
    }

    public static void returnOutOfJailCard(StreetType streetType) {
        synchronized (DECKS) {
            DeckState deckState = DECKS.get(streetType);
            if (deckState == null) {
                return;
            }
            deckState.returnOutOfJailCard();
        }
    }

    static void resetDecks() {
        synchronized (DECKS) {
            DECKS.clear();
        }
    }

    private DeckState getDeckState() {
        synchronized (DECKS) {
            DeckState deckState = DECKS.get(streetType);
            if (deckState == null) {
                deckState = createDeckState(streetType);
                DECKS.put(streetType, deckState);
            }
            return deckState;
        }
    }

    private record CardDefinition(CardType cardType, int entryIndex, List<String> values) {
    }

    private static final class DeckState {
        private final Deque<CardDefinition> drawPile = new ArrayDeque<>();
        private final Deque<CardDefinition> heldOutOfJailCards = new ArrayDeque<>();

        private DeckState(List<CardDefinition> definitions) {
            drawPile.addAll(definitions);
        }

        private CardDefinition drawNext() {
            if (drawPile.isEmpty()) {
                throw new IllegalStateException("No cards available in deck");
            }
            CardDefinition definition = drawPile.removeFirst();
            if (definition.cardType() == CardType.OUT_OF_JAIL) {
                heldOutOfJailCards.addLast(definition);
            } else {
                drawPile.addLast(definition);
            }
            return definition;
        }

        private void returnOutOfJailCard() {
            CardDefinition definition = heldOutOfJailCards.pollFirst();
            if (definition == null) {
                return;
            }
            drawPile.addLast(definition);
        }
    }
}
