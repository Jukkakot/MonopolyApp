package fi.monopoly.images;

import fi.monopoly.client.desktop.DesktopClientSettings;
import fi.monopoly.client.desktop.DesktopImageCatalog;
import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.MonopolyUtils;
import fi.monopoly.utils.SpotProps;
import lombok.Getter;
import lombok.ToString;
import processing.core.PConstants;
import processing.core.PImage;

import static fi.monopoly.text.UiTexts.text;

@ToString(callSuper = true)
public class SpotImage extends Image {
    @Getter
    protected SpotType spotType;

    public SpotImage(MonopolyRuntime runtime, SpotType spotType) {
        this(runtime, null, spotType);
    }

    public SpotImage(MonopolyRuntime runtime, Coordinates coords, SpotType spotType) {
        super(runtime, coords, null, StreetType.CORNER.equals(spotType.streetType) ? Spot.SPOT_H : Spot.SPOT_W, Spot.SPOT_H);
        this.spotType = spotType;
    }

    @Override
    public void draw(Coordinates c) {
        super.draw(c);
        if (c != null) {
            setCoords(c);
        }
        updateIsHovered();
        drawSpot();
        drawDebugSpot();
        drawIconSpot();
    }

    private void drawDebugSpot() {
        if (!DesktopClientSettings.debugMode()) {
            return;
        }
        MonopolyApp p = runtime.app();
        p.push();
        int[] rectCoords = MonopolyUtils.getCoords(new SpotProps(coords, getWidth(), getHeight()));
        if (isHovered) {
            p.fill(0, 255, 0, 30);
        } else {
            p.fill(255, 0, 0, 30);
        }
        p.noStroke();
        if (coords.r() == 0 || coords.r() == 180) {
            p.rect(rectCoords[0], rectCoords[3], getWidth(), getHeight());
        } else {
            p.rect(rectCoords[0], rectCoords[3], getHeight(), getWidth());
        }
        p.pop();
    }

    private void drawSpot() {
        MonopolyApp p = runtime.app();
        p.push();

        p.fill(205, 230, 209);
        p.strokeWeight(3);
        p.stroke(0);

        p.translate(coords.x(), coords.y());

        //Outside border
        p.rotate(MonopolyApp.radians(coords.r()));
        p.rect(-getWidth() / 2, -getHeight() / 2, getWidth(), getHeight());
        if (spotType != null && spotType.hasProperty("price")) {
            drawPrice();
        }

        p.pop();
    }

    private void updateIsHovered() {
        isHovered = MonopolyUtils.isMouseInArea(runtime.app(), new SpotProps(coords, getUnScaledWidth(), getUnScaledHeight()));
    }

    private void drawPrice() {
        MonopolyApp p = runtime.app();
        p.fill(0);
        p.textAlign(PConstants.CENTER);
        p.textFont(runtime.font10());
        p.text(text("format.money", spotType.getStringProperty("price")), (int) -(getWidth() * 0.37), getHeight() / 3, (int) (getWidth() * 0.75), getHeight());
    }

    private void drawIconSpot() {
        MonopolyApp p = runtime.app();
        PImage img = DesktopImageCatalog.getImage(spotType);
        if (img != null) {
            p.push();

            p.translate(coords.x(), coords.y());
            p.rotate(MonopolyApp.radians((coords.r())));
            p.imageMode(PConstants.CENTER);
            p.image(img, 0, 0, getWidth(), getHeight());

            p.fill(0);
            p.textAlign(PConstants.CENTER);
            p.textFont(runtime.font10());
            p.textLeading(10);
            if (!StreetType.CORNER.equals(spotType.streetType) && spotType.hasProperty("name")) {
                p.text(spotType.getStringProperty("name"),
                        (int) -(getWidth() * 0.37), (int) -(getHeight() * 0.42),
                        (int) (getWidth() * 0.75), getHeight() / 2);
            }

            p.pop();
        }
    }
}
