package fi.monopoly.components.popup;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.Player;
import fi.monopoly.utils.UiTokens;
import processing.core.PGraphics;
import processing.core.PImage;

import static fi.monopoly.text.UiTexts.text;
import static processing.core.PConstants.CENTER;
import static processing.core.PConstants.CORNER;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class PropertyAuctionPopup extends PropertyOfferPopup {
    private String reasonText = "";
    private int currentBidAmount;
    private Player currentLeader;

    protected PropertyAuctionPopup(MonopolyRuntime runtime) {
        super(runtime, "propertyAuction");
    }

    public void setButtonLabels(String primaryLabel, String secondaryLabel) {
        setActionLabels(primaryLabel, secondaryLabel);
    }

    public void setAuctionInfo(String reasonText, Player currentLeader, int currentBidAmount) {
        this.reasonText = reasonText;
        this.currentLeader = currentLeader;
        this.currentBidAmount = currentBidAmount;
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
        float cardW = fi.monopoly.components.spots.Spot.SPOT_W;
        float cardH = fi.monopoly.components.spots.Spot.SPOT_H;
        float cardX = centerX - cardW / 2f;
        float cardY = top + UiTokens.popupTextTopOffset() - 6f;

        p.pushMatrix();
        p.pushStyle();
        p.resetMatrix();

        p.fill(p.color(255, 217, 127));
        p.rectMode(CENTER);
        p.stroke(0);
        p.strokeWeight(10);
        p.rect(centerX, centerY, width, height, 30);

        if (getOfferedProperty() != null) {
            drawPropertyCard(p, cardX, cardY, cardW, cardH);
        }

        p.fill(0);
        p.textFont(runtime.font20());
        p.textAlign(CENTER, TOP);
        p.text(getPopupText(), centerX, cardY + cardH + 8f);

        p.textFont(runtime.font20());
        p.text(reasonText, centerX, cardY + cardH + 36f);

        float infoY = cardY + cardH + 74f;
        drawLeaderCard(p, centerX - 116f, infoY, 112f, 92f);
        drawMoneyChip(p, centerX + 4f, infoY, 176f, 92f, currentBidAmount);

        p.popStyle();
        p.popMatrix();
    }

    private void drawLeaderCard(PGraphics p, float x, float y, float width, float height) {
        p.rectMode(CORNER);
        p.stroke(0);
        p.strokeWeight(2);
        p.fill(247, 237, 202);
        p.rect(x, y, width, height, 12);

        p.fill(76, 68, 44);
        p.textAlign(CENTER, TOP);
        p.textFont(runtime.font10());
        p.text(text("property.auction.leaderLabel"), x + width / 2f, y + 6f);

        if (currentLeader == null) {
            p.textFont(runtime.font20());
            p.text(text("property.auction.none"), x + width / 2f, y + 38f);
            return;
        }

        PImage token = MonopolyApp.getImage("BigToken.png", currentLeader.getColor());
        if (token != null) {
            float tokenSize = 28f;
            p.imageMode(CORNER);
            p.image(token, x + width / 2f - tokenSize / 2f, y + 22f, tokenSize, tokenSize);
        }
        p.textFont(runtime.font10());
        p.text(currentLeader.getName(), x + width / 2f, y + 56f, width - 12f, 24f);
    }

    private void drawMoneyChip(PGraphics p, float x, float y, float width, float height, int amount) {
        p.rectMode(CORNER);
        p.stroke(34, 77, 44);
        p.strokeWeight(2);
        p.fill(196, 227, 170);
        p.rect(x, y, width, height, 14);

        p.noStroke();
        p.fill(121, 171, 104);
        p.rect(x + 10f, y + 9f, 16f, height - 18f, 8);
        p.rect(x + width - 26f, y + 9f, 16f, height - 18f, 8);

        p.fill(35, 75, 41);
        p.textAlign(CENTER, TOP);
        p.textFont(runtime.font10());
        p.text(text("property.auction.currentBidLabel"), x + width / 2f, y + 10f);
        p.textFont(runtime.font30());
        p.text("M" + amount, x + width / 2f, y + 34f);
    }

    @Override
    protected boolean onComputerAction(fi.monopoly.components.computer.ComputerPlayerProfile profile) {
        return false;
    }

    @Override
    protected boolean allowManualInteractionDuringComputerTurn() {
        return true;
    }

    @Override
    protected void hide() {
        super.hide();
        reasonText = "";
        currentBidAmount = 0;
        currentLeader = null;
    }
}
