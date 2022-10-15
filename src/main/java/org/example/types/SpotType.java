package org.example.types;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public enum SpotType {
    B1(StreetType.BROWN, 1), B2(StreetType.BROWN, 2),
    LB1(StreetType.LIGHT_BLUE, 1), LB2(StreetType.LIGHT_BLUE, 2), LB3(StreetType.LIGHT_BLUE, 3),
    P1(StreetType.PURPLE, 1), P2(StreetType.PURPLE, 2), P3(StreetType.PURPLE, 3),
    O1(StreetType.ORANGE, 1), O2(StreetType.ORANGE, 2), O3(StreetType.ORANGE, 3),
    R1(StreetType.RED, 1), R2(StreetType.RED, 2), R3(StreetType.RED, 3),
    Y1(StreetType.YELLOW, 1), Y2(StreetType.YELLOW, 2), Y3(StreetType.YELLOW, 3),
    G1(StreetType.GREEN, 1), G2(StreetType.GREEN, 2), G3(StreetType.GREEN, 3),
    DB1(StreetType.DARK_BLUE, 1), DB2(StreetType.DARK_BLUE, 2),
    RR1(StreetType.RAILROAD, 1), RR2(StreetType.RAILROAD, 2), RR3(StreetType.RAILROAD, 3), RR4(StreetType.RAILROAD, 4),
    U1(StreetType.UTILITY1, 1), U2(StreetType.UTILITY2, 2),
    TAX1(StreetType.TAX1, 1), TAX2(StreetType.TAX2, 2),
    COMMUNITY1(StreetType.COMMUNITY, 1), COMMUNITY2(StreetType.COMMUNITY, 2), COMMUNITY3(StreetType.COMMUNITY, 3),
    CHANCE1(StreetType.CHANCE1, 1), CHANCE2(StreetType.CHANCE2, 2), CHANCE3(StreetType.CHANCE3, 3),
    CORNER1(StreetType.CORNER1, 1), CORNER2(StreetType.CORNER2, 2), CORNER3(StreetType.CORNER3, 3), CORNER4(StreetType.CORNER4, 4);

    public final StreetType streetType;
    public final int id;
    private static final Properties props = new Properties();

    static {
        try {
            props.load(SpotType.class.getResourceAsStream("/SpotType.properties"));
        } catch (Exception e) {
            System.err.println("Error loading SpotType properties: " + e.getMessage());
        }
    }

    SpotType(StreetType sType, int id) {
        this.streetType = sType;
        this.id = id;

    }

    public String getProperty(String propName) {
        try {
            if (!name().startsWith("COMMUNITY") && !name().startsWith("CHANCE")) {
                return props.getProperty(this.name() + "." + propName, "");
            } else {
                return props.getProperty(this.name().substring(0, this.name().length() - 1) + "." + propName, "");
            }
        } catch (Exception e) {
            System.err.println(propName + " missing for " + this.name());
            return "";
        }
    }

    public static SpotType randomType() {
        int randomIndex = (int) (Math.random() * SpotType.values().length);
        return SpotType.values()[randomIndex];
    }

    public static final List<SpotType> spotTypes = Arrays.asList(SpotType.CORNER1, SpotType.B1, SpotType.COMMUNITY1, SpotType.B2,
            SpotType.TAX1, SpotType.RR1, SpotType.LB1, SpotType.CHANCE1, SpotType.LB2, SpotType.LB3, SpotType.CORNER2,
            SpotType.P1, SpotType.U1, SpotType.P2, SpotType.P3, SpotType.RR2, SpotType.O1, SpotType.COMMUNITY2, SpotType.O2, SpotType.O3, SpotType.CORNER3,
            SpotType.R1, SpotType.CHANCE2, SpotType.R2, SpotType.R3, SpotType.RR3, SpotType.Y1, SpotType.Y2, SpotType.U2, SpotType.Y3, SpotType.CORNER4,
            SpotType.G1, SpotType.G2, SpotType.COMMUNITY3, SpotType.G3, SpotType.RR4, SpotType.CHANCE3, SpotType.DB1, SpotType.TAX2, SpotType.DB2);
}
