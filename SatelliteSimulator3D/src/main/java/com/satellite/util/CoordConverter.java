package com.satellite.util;

import com.satellite.model.Satellite;

public class CoordConverter {

    /**
     * Chuyển (lat, lon, alt) → (x, y, z) trong không gian 3D JavaFX.
     * JavaFX: trục Y hướng xuống, trục Z hướng ra màn hình.
     *
     * @param lat      vĩ độ (độ, -90 đến 90)
     * @param lon      kinh độ (độ, -180 đến 180)
     * @param altKm    độ cao (km)
     * @param planetRadiusKm bán kính hành tinh (km)
     * @param scale    hệ số thu nhỏ (ví dụ: 1.0/100.0 → 100km = 1 đơn vị 3D)
     * @return double[]{x, y, z}
     */
    public static double[] toXYZ(double lat, double lon,
                                  double altKm, double planetRadiusKm,
                                  double scale) {
        double r = (planetRadiusKm + altKm) * scale;
        double phi    = Math.toRadians(lat);
        double lambda = Math.toRadians(lon);

        double x = r * Math.cos(phi) * Math.cos(lambda);
        double y = r * Math.sin(phi);          // lên/xuống theo vĩ độ
        double z = r * Math.cos(phi) * Math.sin(lambda);

        return new double[]{x, y, z};
    }

    /**
     * Áp toXYZ vào thẳng object Satellite, cập nhật x,y,z của nó.
     */
    public static void applyToSatellite(Satellite s,
                                         double planetRadiusKm,
                                         double scale) {
        double[] xyz = toXYZ(s.getLatitude(), s.getLongitude(),
                              s.getAltitudeKm(), planetRadiusKm, scale);
        s.setX(xyz[0]);
        s.setY(xyz[1]);
        s.setZ(xyz[2]);
    }
}
