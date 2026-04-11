package fi.monopoly.components.popup;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.properties.Property;
import fi.monopoly.types.PlaceType;
import fi.monopoly.types.StreetType;
import fi.monopoly.utils.MonopolyUtils;
import fi.monopoly.utils.TextWrapUtils;
import fi.monopoly.utils.UiTokens;
import processing.core.PGraphics;
import processing.core.PImage;

import java.util.List;

import static processing.core.PConstants.CENTER;
import static processing.core.PConstants.CORNER;
import static processing.core.PConstants.TOP;

public class PropertyOfferPopup extends ChoicePopup {
    private static final int CARD_BASE_R = 205;
    private static final int CARD_BASE_G = 230;
    private static final int CARD_BASE_B = 209;
    private Property offeredProperty;

    protected PropertyOfferPopup(MonopolyRuntime runtime) {
        this(runtime, "propertyOffer");
    }

    protected PropertyOfferPopup(MonopolyRuntime runtime, String controlPrefix) {
        super(runtime, controlPrefix);
    }

    public void setOfferedProperty(Property offeredProperty) {
        this.offeredProperty = offeredProperty;
    }

    public Property getOfferedProperty() {
        return offeredProperty;
    }

    @Override
    protected int getPopupWidth() {
        int availableWidth = Math.round(runtime.app().width) - UiTokens.popupWindowMargin() * 2;
        return Math.max(420, Math.min(640, availableWidth));
    }

    @Override
    protected int getPopupHeight() {
        int availableHeight = runtime.app().height - UiTokens.popupWindowMargin() * 2;
        return Math.max(360, Math.min(540, availableHeight));
    }

    @Override
    protected float getButtonAreaTop() {
        return getPopupBottom() - UiTokens.popupButtonHeight() - UiTokens.popupButtonAreaBottomPadding() - UiTokens.spacingSm();
    }

    @Override
    protected float getButtonAreaHeight() {
        return UiTokens.popupButtonHeight() + UiTokens.spacingSm();
    }

    @Override
    public void draw(PGraphics p) {
        if (!isVisible) {
            return;
        }
        refreshControlLayout();
        float centerX = getPopupCenter().x();
        float centerY = getPopupCenter().y();
        float width = getPopupWidth();
        float height = getPopupHeight();
        float top = getPopupTop();

        p.pushMatrix();
        p.pushStyle();
        p.resetMatrix();

        p.fill(p.color(255, 217, 127));
        p.rectMode(CENTER);
        p.stroke(0);
        p.strokeWeight(10);
        p.rect(centerX, centerY, width, height, 30);

        if (offeredProperty != null) {
            float cardW = fi.monopoly.components.spots.Spot.SPOT_W;
            float cardH = fi.monopoly.components.spots.Spot.SPOT_H;
            float cardX = centerX - cardW / 2f;
            float cardY = top + UiTokens.popupTextTopOffset();
            drawPropertyCard(p, cardX, cardY, cardW, cardH);
            drawPopupText(p, centerX, cardY + cardH + UiTokens.spacingSm(), width - UiTokens.popupTextSidePadding() * 2f);
        } else {
            drawPopupText(p, centerX, top + UiTokens.popupTextTopOffset(), width - UiTokens.popupTextSidePadding() * 2f);
        }

        p.popStyle();
        p.popMatrix();
    }

    protected void drawPopupText(PGraphics p, float centerX, float startY, float maxWidth) {
        p.fill(0);
        p.textFont(runtime.font20());
        p.textAlign(CENTER, TOP);
        p.textLeading(UiTokens.popupTextLineHeight());
        List<String> lines = TextWrapUtils.wrapText(p, getPopupText(), maxWidth, getPopupKind() + ".offer");
        for (int i = 0; i < lines.size(); i++) {
            float y = startY + i * UiTokens.popupTextLineHeight();
            if (y + UiTokens.popupTextLineHeight() > getButtonAreaTop() - UiTokens.spacingSm()) {
                break;
            }
            p.text(lines.get(i), centerX, y);
        }
    }

    protected void drawPropertyCard(PGraphics p, float x, float y, float width, float height) {
        p.rectMode(CORNER);
        p.stroke(p.color(0));
        p.strokeWeight(2);
        p.fill(p.color(245, 250, 241));
        p.rect(x, y, width, height, 14);

        p.noStroke();
        p.fill(p.color(CARD_BASE_R, CARD_BASE_G, CARD_BASE_B));
        p.rect(x + 4f, y + 4f, width - 8f, height - 8f, 11);

        boolean hasStripe = hasDecorativeStripe();
        if (hasStripe) {
            drawStripe(p, x, y, width);
        }
        PImage image = MonopolyApp.getImage(offeredProperty.getSpotType());
        if (image != null) {
            p.imageMode(CORNER);
            float imageTopInset = hasStripe ? 8f + UiTokens.tradePropertyColorStripeHeight() : 8f;
            float imageHeight = hasStripe ? height - 58f : height - 46f;
            p.image(image, x + 8f, y + imageTopInset, width - 16f, imageHeight);
        }

        p.fill(0);
        p.textFont(runtime.font10());
        p.textAlign(CENTER, TOP);
        p.textLeading(10);
        p.text(
                offeredProperty.getDisplayName(),
                x + width * 0.12f,
                y + height - 30f,
                width * 0.76f,
                24f
        );
    }

    protected void drawStripe(PGraphics p, float x, float y, float width) {
        p.fill(resolveStripeColor());
        p.rect(
                x + 5f,
                y + 5f,
                width - 10f,
                UiTokens.tradePropertyColorStripeHeight(),
                10,
                10,
                4,
                4
        );
    }

    protected int resolveStripeColor() {
        if (offeredProperty == null) {
            return runtime.app().color(170, 150, 100);
        }
        StreetType streetType = offeredProperty.getSpotType().streetType;
        if (streetType.color != null) {
            return MonopolyUtils.toColor(runtime.app(), streetType.color);
        }
        if (streetType.placeType == PlaceType.RAILROAD) {
            return runtime.app().color(50, 50, 50);
        }
        if (streetType.placeType == PlaceType.UTILITY) {
            return runtime.app().color(120, 160, 170);
        }
        return runtime.app().color(170, 150, 100);
    }

    protected boolean hasDecorativeStripe() {
        if (offeredProperty == null) {
            return false;
        }
        StreetType streetType = offeredProperty.getSpotType().streetType;
        return streetType.placeType == PlaceType.STREET && streetType.color != null;
    }

    @Override
    protected void hide() {
        super.hide();
        offeredProperty = null;
    }
}
