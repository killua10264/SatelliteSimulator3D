package com.satellite.model;

public class Satellite {
    private int id;
    private int planetId;
    private String name;
    private double latitude;    // độ
    private double longitude;   // độ
    private double altitudeKm;  // km
    private boolean relay;

    // Tọa độ 3D — tính runtime
    private double x, y, z;

    public Satellite(int id, int planetId, String name,
                     double lat, double lon, double alt, boolean relay) {
        this.id = id;
        this.planetId = planetId;
        this.name = name;
        this.latitude = lat;
        this.longitude = lon;
        this.altitudeKm = alt;
        this.relay = relay;
    }

    public int getId()            { return id; }
    public int getPlanetId()      { return planetId; }
    public String getName()       { return name; }
    public double getLatitude()   { return latitude; }
    public double getLongitude()  { return longitude; }
    public double getAltitudeKm(){ return altitudeKm; }
    public boolean isRelay()      { return relay; }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setZ(double z) { this.z = z; }

    @Override
    public String toString() {
        return name + " (lat=" + latitude + ", lon=" + longitude
               + ", alt=" + altitudeKm + "km)";
    }
}
