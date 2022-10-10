package org.example.images;

import javafx.scene.paint.Color;
import org.example.spots.Drawable;
import org.example.types.PropertyType;
import processing.core.PApplet;
import static org.example.Utils.toFloat;

public class PropertyImage extends RowImage implements Drawable {

    private final PropertyType propertyType;

    public PropertyImage(PApplet p, int x, int y, PropertyType propertyType) {
        super(p,x,y);
        this.propertyType = propertyType;
    }

    @Override
    public void draw() {
        draw(0);
    }
    @Override
    public void draw(float rotate) {
        p.push();
        p.noFill();
        p.strokeWeight(3);
        p.stroke(50);
        //Outside border
        p.rotate(radians(rotate));
        p.rect(x,y,width,height);

        //Property color
        p.rotate(radians((rotate)));
        Color color = this.propertyType.streetType.color;
        p.fill(toFloat(color.getRed()),toFloat(color.getGreen()), toFloat(color.getBlue()));
        p.rect(x,y,width,height/4);

        p.pop();
    }
}
