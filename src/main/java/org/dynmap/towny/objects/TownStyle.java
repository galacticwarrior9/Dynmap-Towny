package org.dynmap.towny.objects;

import com.palmergames.bukkit.towny.object.Town;
import org.jetbrains.annotations.NotNull;

public class TownStyle {
    private double borderOpacity;
    private int borderWeight;
    private int borderColor;

    private double fillOpacity;
    private int fillColor;
    
    public TownStyle(double borderOpacity, int borderWeight, int borderColor, double fillOpacity, int fillColor) {
        this.borderOpacity = borderOpacity;
        this.borderWeight = borderWeight;
        this.borderColor = borderColor;
        this.fillOpacity = fillOpacity;
        this.fillColor = fillColor;
    }

    public double getBorderOpacity() {
        return borderOpacity;
    }

    public void setBorderOpacity(double borderOpacity) {
        this.borderOpacity = borderOpacity;
    }

    public int getBorderWeight() {
        return borderWeight;
    }

    public void setBorderWeight(int borderWeight) {
        this.borderWeight = borderWeight;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }
    
    public void setBorder(double borderOpacity, int borderWeight, int borderColor) {
        this.borderOpacity = borderOpacity;
        this.borderWeight = borderWeight;
        this.borderColor = borderColor;
    }

    public double getFillOpacity() {
        return fillOpacity;
    }

    public void setFillOpacity(double fillOpacity) {
        this.fillOpacity = fillOpacity;
    }

    public int getFillColor() {
        return fillColor;
    }

    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
    }
    
    public void setFill(double fillOpacity, int fillColor) {
        this.fillOpacity = fillOpacity;
        this.fillColor = fillColor;
    }
}
