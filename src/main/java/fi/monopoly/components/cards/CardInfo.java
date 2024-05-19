package fi.monopoly.components.cards;

import fi.monopoly.types.CardType;

import java.util.List;

public record CardInfo(CardType cardType, String text, List<String> values) {
}
