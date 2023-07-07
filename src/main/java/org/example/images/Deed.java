package org.example.images;

import lombok.Getter;
import lombok.ToString;
import org.example.components.properties.Property;

@ToString(callSuper = true)
public class Deed extends Clickable {
    @Getter
    private Property property;

    public Deed(Property property) {
        super(property.getSpotType());
        this.property = property;
    }

    @Override
    public void onClick() {
        //Mortgaging
        System.out.println("Clicked deed " + this);
    }

}
