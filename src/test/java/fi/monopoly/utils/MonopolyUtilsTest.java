package fi.monopoly.utils;

import fi.monopoly.client.desktop.MonopolyApp;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MonopolyUtilsTest {

    @Test
    void toFloatMapsIntRangeToZeroAndOneRange() {
        assertEquals(0.0f, MonopolyUtils.toFloat(0));
        assertEquals(1.0f, MonopolyUtils.toFloat(255));
        assertEquals(128f / 255f, MonopolyUtils.toFloat(128), 0.0001f);
    }

    @Test
    void toFloatMapsDoubleRangeToZeroAnd255Range() {
        assertEquals(0.0f, MonopolyUtils.toFloat(0.0));
        assertEquals(255.0f, MonopolyUtils.toFloat(1.0));
        assertEquals(127.5f, MonopolyUtils.toFloat(0.5), 0.0001f);
    }

    @Test
    void toColorCreatesJavaFxColorFromRgbInts() {
        Color color = MonopolyUtils.toColor(255, 128, 0);

        assertEquals(1.0, color.getRed(), 0.0001);
        assertEquals(128.0 / 255.0, color.getGreen(), 0.0001);
        assertEquals(0.0, color.getBlue(), 0.0001);
    }

    @Test
    void getCoordsUsesWidthAndHeightNormallyWhenRotationIsZeroOr180() {
        SpotProps zeroRotation = new SpotProps(10, 20, 8, 6, 0);
        SpotProps oneEightyRotation = new SpotProps(10, 20, 8, 6, 180);

        assertArrayEquals(new int[]{6, 23, 14, 17}, MonopolyUtils.getCoords(zeroRotation));
        assertArrayEquals(new int[]{6, 23, 14, 17}, MonopolyUtils.getCoords(oneEightyRotation));
    }

    @Test
    void getCoordsSwapsWidthAndHeightWhenRotationIsSideways() {
        SpotProps ninetyRotation = new SpotProps(10, 20, 8, 6, 90);

        assertArrayEquals(new int[]{7, 24, 13, 16}, MonopolyUtils.getCoords(ninetyRotation));
    }

    @Test
    void isPointInAreaUsesSpotPropsCoordinates() {
        SpotProps area = new SpotProps(10, 20, 8, 6, 0);

        assertTrue(MonopolyUtils.isPointInArea(10, 20, area));
        assertFalse(MonopolyUtils.isPointInArea(6, 20, area));
    }

    @Test
    void isMouseInAreaUsesCurrentMouseCoordinatesFromApp() {
        MonopolyApp app = new MonopolyApp();
        app.mouseX = 10;
        app.mouseY = 20;
        SpotProps area = new SpotProps(10, 20, 8, 6, 0);

        assertTrue(MonopolyUtils.isMouseInArea(area));

        app.mouseX = 100;
        app.mouseY = 100;
        assertFalse(MonopolyUtils.isMouseInArea(area));
    }

    @Test
    void isMouseInAreaWithCoordinatesDelegatesToAreaCalculation() {
        MonopolyApp app = new MonopolyApp();
        app.mouseX = 12;
        app.mouseY = 20;

        assertTrue(MonopolyUtils.isMouseInArea(new Coordinates(10, 20, 0), 8, 6));

        app.mouseX = 30;
        app.mouseY = 30;
        assertFalse(MonopolyUtils.isMouseInArea(new Coordinates(10, 20, 0), 8, 6));
    }

    @Test
    void parseIllegalCharactersNormalizesKnownBrokenCharacters() {
        assertEquals("Aani ja Oljy", MonopolyUtils.parseIllegalCharacters("Ääni ja Öljy"));
        assertEquals("maito", MonopolyUtils.parseIllegalCharacters("maito"));
    }

    @Test
    void isInsideAreaChecksBoundsStrictly() {
        assertTrue(MonopolyUtils.isInsideArea(0, 10, 10, 0, 5, 5));
        assertFalse(MonopolyUtils.isInsideArea(0, 10, 10, 0, 0, 5));
        assertFalse(MonopolyUtils.isInsideArea(0, 10, 10, 0, 10, 5));
    }
}
