package org.example.types;

public enum PropertyType {
    B1(StreetType.BROWN,1), B2(StreetType.BROWN,2),
    LB1(StreetType.LIGHT_BLUE,1), LB2(StreetType.LIGHT_BLUE,2), LB3(StreetType.LIGHT_BLUE,3),
    P1(StreetType.PINK,1), P2(StreetType.PINK,2), P3(StreetType.PINK,3),
    O1(StreetType.ORANGE,1), O2(StreetType.ORANGE,2), O3(StreetType.ORANGE,3),
    R1(StreetType.RED,1), R2(StreetType.RED,2), R3(StreetType.RED,3),
    Y1(StreetType.YELLOW,1), Y2(StreetType.YELLOW,2), Y3(StreetType.YELLOW,3),
    G1(StreetType.GREEN,1), G2(StreetType.GREEN,2), G3(StreetType.GREEN,3),
    DB1(StreetType.DARK_BLUE,1), DB2(StreetType.DARK_BLUE,2), DB3(StreetType.DARK_BLUE,3),
    RR1(StreetType.RAILROAD,1), RR2(StreetType.RAILROAD,2), RR3(StreetType.RAILROAD,3), RR4(StreetType.RAILROAD,4),
    U1(StreetType.UTILITY,1), U2(StreetType.UTILITY,2);
    public final StreetType streetType;
    public final int id;
    PropertyType(StreetType sType, int id) {
        this.streetType = sType;
        this.id = id;
    }
}
