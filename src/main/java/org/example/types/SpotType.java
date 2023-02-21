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
    U1(StreetType.UTILITY, 1), U2(StreetType.UTILITY, 2),
    TAX1(StreetType.TAX, 1), TAX2(StreetType.TAX, 2),
    COMMUNITY1(StreetType.COMMUNITY, 1), COMMUNITY2(StreetType.COMMUNITY, 2), COMMUNITY3(StreetType.COMMUNITY, 3),
    CHANCE1(StreetType.CHANCE, 1), CHANCE2(StreetType.CHANCE, 2), CHANCE3(StreetType.CHANCE, 3),
    GO_SPOT(StreetType.CORNER, 1), JAIL(StreetType.CORNER, 2), FREE_PARKING(StreetType.CORNER, 3), GO_TO_JAIL(StreetType.CORNER, 4);

    public final StreetType streetType;
    public final int id;
    private static final Properties props = new Properties();

    static {
        try {
            props.load(SpotType.class.getResourceAsStream("/" + SpotType.class.getSimpleName() + ".properties"));
        } catch (Exception e) {
            System.err.println("Error loading SpotType properties: " + e.getMessage());
        }
    }

    SpotType(StreetType sType, int id) {
        this.streetType = sType;
        this.id = id;

    }

    public String getProperty(String propName) {
        String result = "";
        try {
            result = props.getProperty(name() + "." + propName, "");
            if (result.isBlank() || result.isEmpty()) {
                result = props.getProperty(name().substring(0, name().length() - 1) + "." + propName, "");
            }
        } catch (Exception e) {
            System.err.println(propName + " missing for " + name());
        }
        return result;
    }

    public static SpotType randomType() {
        int randomIndex = (int) (Math.random() * SpotType.values().length);
        return SpotType.values()[randomIndex];
    }

    public static final List<SpotType> SPOT_TYPES = Arrays.asList(SpotType.GO_SPOT, SpotType.B1, SpotType.COMMUNITY1, SpotType.B2,
            SpotType.TAX1, SpotType.RR1, SpotType.LB1, SpotType.CHANCE1, SpotType.LB2, SpotType.LB3, SpotType.JAIL,
            SpotType.P1, SpotType.U1, SpotType.P2, SpotType.P3, SpotType.RR2, SpotType.O1, SpotType.COMMUNITY2, SpotType.O2, SpotType.O3, SpotType.FREE_PARKING,
            SpotType.R1, SpotType.CHANCE2, SpotType.R2, SpotType.R3, SpotType.RR3, SpotType.Y1, SpotType.Y2, SpotType.U2, SpotType.Y3, SpotType.GO_TO_JAIL,
            SpotType.G1, SpotType.G2, SpotType.COMMUNITY3, SpotType.G3, SpotType.RR4, SpotType.CHANCE3, SpotType.DB1, SpotType.TAX2, SpotType.DB2);

    public static Integer getNumberOfSpots(StreetType streetType) {
        return SPOT_TYPES.stream().filter(spotType -> streetType.equals(spotType.streetType)).toList().size();
    }
}
