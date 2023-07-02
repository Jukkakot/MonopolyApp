package org.example.images;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.example.types.SpotType;

@ToString(callSuper = true)
@RequiredArgsConstructor
public class Deed {
    @Getter
    private final SpotImage spotImage;

    public Deed(SpotType spotType) {
        this.spotImage = ImageFactory.getImage(null, spotType);
    }
}
