package com.satellite.physics;

import com.satellite.model.Planet;
import com.satellite.model.Satellite;

public class OrbitalSatellite {

    private final Satellite satellite;
    private final Planet planet;
    private final double radiusM;
    private final double omega;
    private final double yOffset;
    private final double scale;

    private double theta;

    public OrbitalSatellite(Satellite satellite, Planet planet, double scale) {
        this.satellite = satellite;
        this.planet = planet;
        this.scale = scale;

        PhysicsEngine.OrbitParams params =
            PhysicsEngine.compute(planet.getRadiusKm(), planet.getMassKg(), satellite.getAltitudeKm());
        this.radiusM = params.radiusM();
        this.omega = params.angularVelocityRadS();

        this.theta = Math.toRadians(satellite.getLongitude());
        this.yOffset = radiusM * Math.sin(Math.toRadians(satellite.getLatitude())) / 1000.0 * scale;

        updatePosition();
    }

    public void update(double dtSeconds, double timeScale) {
        theta += omega * dtSeconds * timeScale;
        updatePosition();
    }

    private void updatePosition() {
        double rScene = (radiusM / 1000.0) * scale;
        double cosLat = Math.cos(Math.toRadians(satellite.getLatitude()));
        satellite.setX(rScene * cosLat * Math.cos(theta));
        satellite.setY(yOffset);
        satellite.setZ(rScene * cosLat * Math.sin(theta));
    }

    public Satellite getSatellite() { return satellite; }
    public Planet getPlanet()       { return planet; }
    public double getTheta()        { return theta; }
    public double getOmega()        { return omega; }
    public double getRadiusM()      { return radiusM; }
}
