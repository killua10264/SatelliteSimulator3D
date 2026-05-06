package com.satellite.model;

public class Planet {
    private int id;
    private String name;
    private double radiusKm;
    private double massKg;
    private String textureFile;

    public Planet(int id, String name, double radiusKm,
                  double massKg, String textureFile) {
        this.id = id;
        this.name = name;
        this.radiusKm = radiusKm;
        this.massKg = massKg;
        this.textureFile = textureFile;
    }

    public int getId()           { return id; }
    public String getName()      { return name; }
    public double getRadiusKm()  { return radiusKm; }
    public double getMassKg()    { return massKg; }
    public String getTextureFile() { return textureFile; }
}
