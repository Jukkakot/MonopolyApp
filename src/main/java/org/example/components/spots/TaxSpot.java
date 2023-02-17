package org.example.components.spots;

import lombok.Getter;
import org.example.images.SpotImage;

public class TaxSpot extends Spot {
    @Getter
    private final Integer price;

    public TaxSpot(SpotImage image) {
        super(image);
        price = Integer.parseInt(spotType.getProperty("price"));
    }
}
