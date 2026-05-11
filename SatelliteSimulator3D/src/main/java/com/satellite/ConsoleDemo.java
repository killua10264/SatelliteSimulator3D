package com.satellite;

import com.satellite.dao.DatabaseConnection;
import com.satellite.dao.PlanetDAO;
import com.satellite.dao.SatelliteDAO;
import com.satellite.model.Planet;
import com.satellite.model.Satellite;
import com.satellite.physics.OrbitalSatellite;
import com.satellite.physics.PhysicsEngine;
import com.satellite.physics.PhysicsEngine.OrbitParams;
import com.satellite.util.CoordConverter;

import java.util.ArrayList;
import java.util.List;

public class ConsoleDemo {
    public static void main(String[] args) {
        System.out.println("=== Satellite Simulator 3D — Phase 1+2 Demo ===\n");

        PlanetDAO planetDAO = new PlanetDAO();
        SatelliteDAO satelliteDAO = new SatelliteDAO();

        // ============ PHẦN 1: NỀN TẢNG DB (thành viên A) ============
        System.out.println("--- Hành tinh trong DB ---");
        List<Planet> planets = planetDAO.findAll();
        for (Planet p : planets) {
            System.out.printf("  [%d] %s | R=%.0f km | M=%.3e kg | texture=%s%n",
                p.getId(), p.getName(), p.getRadiusKm(), p.getMassKg(), p.getTextureFile());
        }

        if (planets.isEmpty()) {
            System.out.println("⚠ Không có hành tinh — hãy chạy script SQL trước!");
            DatabaseConnection.close();
            return;
        }

        Planet earth = planets.get(0);

        List<Satellite> existing = satelliteDAO.findByPlanet(earth.getId());
        if (existing.isEmpty()) {
            System.out.println("\n--- Thêm vệ tinh mẫu vào " + earth.getName() + " ---");
            satelliteDAO.insert(new Satellite(0, earth.getId(), "ISS", 51.6, 0.0, 408.0, false));
            satelliteDAO.insert(new Satellite(0, earth.getId(), "Relay-1", -35.0, 120.0, 35786.0, true));
        } else {
            System.out.println("\n--- Đã có " + existing.size() + " vệ tinh, không insert lại ---");
        }

        System.out.println("\n--- Tọa độ 3D tĩnh (scale: 1 đơn vị = 100 km) ---");
        List<Satellite> sats = satelliteDAO.findByPlanet(earth.getId());
        double scale = 1.0 / 100.0;
        for (Satellite s : sats) {
            CoordConverter.applyToSatellite(s, earth.getRadiusKm(), scale);
            System.out.printf("  %s → X=%.2f  Y=%.2f  Z=%.2f%n",
                s.getName(), s.getX(), s.getY(), s.getZ());
        }

        // ============ PHẦN 2: PHYSICS ENGINE (thành viên B) ============
        System.out.println("\n=== PHYSICS ENGINE — Kiểm chứng công thức quỹ đạo ===");
        System.out.println("Hằng số hấp dẫn G = " + PhysicsEngine.G);

        System.out.println("\n--- Quỹ đạo lý thuyết (so với giá trị thực tế) ---");
        printOrbit("ISS (408 km)",         earth, 408.0,    "~7.66 km/s, ~93 phút");
        printOrbit("Hubble (540 km)",      earth, 540.0,    "~7.59 km/s, ~95 phút");
        printOrbit("GPS (20180 km)",       earth, 20180.0,  "~3.87 km/s, ~12 giờ");
        printOrbit("Geostationary",        earth, 35786.0,  "~3.07 km/s, ~24 giờ");

        // ============ PHẦN 3: ANIMATION SCAFFOLD (thành viên B) ============
        System.out.println("\n=== ANIMATION SIMULATION — Vệ tinh chuyển động theo thời gian ===");
        System.out.println("TIME_SCALE = 1000 (1 giây thực = 1000 giây quỹ đạo)\n");

        List<OrbitalSatellite> orbitals = new ArrayList<>();
        for (Satellite s : sats) {
            orbitals.add(new OrbitalSatellite(s, earth, scale));
        }

        double timeScale = 1000.0;
        double dt = 1.0; // mô phỏng 1 giây thực mỗi bước
        for (int step = 0; step <= 5; step++) {
            double simulatedMinutes = (step * dt * timeScale) / 60.0;
            System.out.printf("[t = %.1f phút mô phỏng]%n", simulatedMinutes);
            for (OrbitalSatellite os : orbitals) {
                Satellite s = os.getSatellite();
                System.out.printf("  %-10s θ=%6.2f°  X=%7.2f  Z=%7.2f%n",
                    s.getName(),
                    Math.toDegrees(os.getTheta()) % 360.0,
                    s.getX(),
                    s.getZ());
            }
            for (OrbitalSatellite os : orbitals) os.update(dt, timeScale);
        }

        DatabaseConnection.close();
        System.out.println("\n=== Demo hoàn tất ===");
    }

    private static void printOrbit(String label, Planet planet, double altKm, String expected) {
        OrbitParams op = PhysicsEngine.compute(planet.getRadiusKm(), planet.getMassKg(), altKm);
        System.out.printf("  %-22s | v = %5.2f km/s | T = %7.2f phút   (thực tế: %s)%n",
            label, op.velocityKms(), op.periodMinutes(), expected);
    }
}
