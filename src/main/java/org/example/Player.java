package org.example;

import org.example.types.SpotType;

import java.util.ArrayList;
import java.util.List;

public class Player {
    int id;
    String name;
    Integer money = 0;
    Token token;
    List<SpotType> ownedProperties = new ArrayList<>();

}
