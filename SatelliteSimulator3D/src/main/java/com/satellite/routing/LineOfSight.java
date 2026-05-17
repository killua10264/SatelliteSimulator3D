package com.satellite.routing;

import com.satellite.model.Satellite;

public final class LineOfSight {

    private LineOfSight() {}

    /**
     * Kiểm tra 2 vệ tinh có nhìn thấy nhau không.
     * @param s1 vệ tinh nguồn (tọa độ x,y,z đã tính sẵn — scene units)
     * @param s2 vệ tinh đích
     * @param planetRadiusScene bán kính hành tinh trong scene units (radiusKm * SCALE)
     * @return true nếu có line-of-sight (không bị hành tinh che)
     */
    public static boolean hasLOS(Satellite s1, Satellite s2, double planetRadiusScene) {
        return hasLOS(
            s1.getX(), s1.getY(), s1.getZ(),
            s2.getX(), s2.getY(), s2.getZ(),
            planetRadiusScene
        );
    }

    /**
     * Kiểm tra LOS giữa 2 điểm trong không gian 3D.
     * Hành tinh là hình cầu tâm O(0,0,0) bán kính R.
     * Thuật toán: tìm điểm trên đoạn P1P2 gần tâm O nhất,
     * nếu khoảng cách > R thì có LOS.
     */
    public static boolean hasLOS(double x1, double y1, double z1,
                                  double x2, double y2, double z2,
                                  double R) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double len2 = dx * dx + dy * dy + dz * dz;

        if (len2 == 0) return true; // cùng vị trí

        // t tối ưu: điểm trên đoạn P1P2 gần tâm O nhất
        double dot = x1 * dx + y1 * dy + z1 * dz;
        double t = Math.max(0, Math.min(1, -dot / len2));

        // Điểm gần nhất
        double cx = x1 + t * dx;
        double cy = y1 + t * dy;
        double cz = z1 + t * dz;
        double dist2 = cx * cx + cy * cy + cz * cz;

        return dist2 > R * R;
    }

    /**
     * Khoảng cách Euclidean giữa 2 vệ tinh (scene units).
     */
    public static double distance(Satellite s1, Satellite s2) {
        double dx = s2.getX() - s1.getX();
        double dy = s2.getY() - s1.getY();
        double dz = s2.getZ() - s1.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
