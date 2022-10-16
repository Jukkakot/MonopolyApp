package org.example.types;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public enum SpotTypeEnum {
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
            props.load(SpotTypeEnum.class.getResourceAsStream("/SpotType.properties"));
        } catch (Exception e) {
            System.err.println("Error loading SpotType properties: " + e.getMessage());
        }
    }

    SpotTypeEnum(StreetType sType, int id) {
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

    public static SpotTypeEnum randomType() {
        int randomIndex = (int) (Math.random() * SpotTypeEnum.values().length);
        return SpotTypeEnum.values()[randomIndex];
    }

    public static final List<SpotTypeEnum> SPOT_TYPE_ENUMS = Arrays.asList(SpotTypeEnum.CORNER1, SpotTypeEnum.B1, SpotTypeEnum.COMMUNITY1, SpotTypeEnum.B2,
            SpotTypeEnum.TAX1, SpotTypeEnum.RR1, SpotTypeEnum.LB1, SpotTypeEnum.CHANCE1, SpotTypeEnum.LB2, SpotTypeEnum.LB3, SpotTypeEnum.CORNER2,
            SpotTypeEnum.P1, SpotTypeEnum.U1, SpotTypeEnum.P2, SpotTypeEnum.P3, SpotTypeEnum.RR2, SpotTypeEnum.O1, SpotTypeEnum.COMMUNITY2, SpotTypeEnum.O2, SpotTypeEnum.O3, SpotTypeEnum.CORNER3,
            SpotTypeEnum.R1, SpotTypeEnum.CHANCE2, SpotTypeEnum.R2, SpotTypeEnum.R3, SpotTypeEnum.RR3, SpotTypeEnum.Y1, SpotTypeEnum.Y2, SpotTypeEnum.U2, SpotTypeEnum.Y3, SpotTypeEnum.CORNER4,
            SpotTypeEnum.G1, SpotTypeEnum.G2, SpotTypeEnum.COMMUNITY3, SpotTypeEnum.G3, SpotTypeEnum.RR4, SpotTypeEnum.CHANCE3, SpotTypeEnum.DB1, SpotTypeEnum.TAX2, SpotTypeEnum.DB2);
}
