package com.satellite.physics;

public final class PhysicsEngine {

    public static final double G = 6.674e-11;

    private PhysicsEngine() {}

    public static double orbitalVelocity(double massKg, double radiusM) {
        return Math.sqrt(G * massKg / radiusM);
    }

    public static double angularVelocity(double velocityMS, double radiusM) {
        return velocityMS / radiusM;
    }

    public static double orbitalPeriod(double angularVelocityRadS) {
        return 2.0 * Math.PI / angularVelocityRadS;
    }

    public static OrbitParams compute(double planetRadiusKm, double planetMassKg, double altitudeKm) {
        double r = (planetRadiusKm + altitudeKm) * 1000.0;
        double v = orbitalVelocity(planetMassKg, r);
        double omega = angularVelocity(v, r);
        double period = orbitalPeriod(omega);
        return new OrbitParams(r, v, omega, period);
    }

    public record OrbitParams(double radiusM, double velocityMS, double angularVelocityRadS, double periodS) {
        public double radiusKm()  { return radiusM / 1000.0; }
        public double velocityKms() { return velocityMS / 1000.0; }
        public double periodMinutes() { return periodS / 60.0; }
    }
}
