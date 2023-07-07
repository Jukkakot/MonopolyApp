package org.example.components.cards;

import org.example.types.CardType;

import java.util.List;

public record Card(CardType cardType, String text, List<String> values) {
}
