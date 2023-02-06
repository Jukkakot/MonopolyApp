package org.example.components.spots;

import org.example.MonopolyApp;
import org.example.components.Player;
import org.example.images.SpotImage;

import java.util.Arrays;
import java.util.List;

public class StreetPropertySpot extends PropertySpot implements SpotInterface {
    private final int housePrice;
    private int houseCount;
    private final List<Integer> rentPrices;

    public StreetPropertySpot(MonopolyApp p, SpotImage spotImage) {
        super(p, spotImage);
        housePrice = Integer.parseInt(spotTypeEnum.getProperty("housePrice"));
        String rentStr = spotTypeEnum.getProperty("rents");
        rentPrices = Arrays.stream(rentStr.split(",")).map(Integer::valueOf).toList();
    }

    public int getRentAmount(Player p) {
        //TODO 2x rent if all properties
        if (ownerPlayer != null && ownerPlayer.equals(p)) {
            return rentPrices.get(houseCount);
        }
        return 0;
    }
//    @Override
//    public String getPopupText(Player p) {
//        if(!hasOwner()) {
//            return "Arrived at " + name + " do you want to buy it?";
//        } else if(isOwner(p)) {
//            return "You own " + name +" Welcome!";
//        } else {
//           return "Uh oh... you need to pay M" + getRentAmount(p) + " rent. to " + ownerPlayer.getName();
//        }
//    }
}
