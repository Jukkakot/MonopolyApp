package org.example.components.cards;

import org.example.types.CardType;

import java.util.List;

public record CardInfo(CardType cardType, String text, List<String> values) {
}
