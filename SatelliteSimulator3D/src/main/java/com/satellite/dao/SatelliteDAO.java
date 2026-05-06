package com.satellite.dao;

import com.satellite.model.Satellite;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SatelliteDAO {

    public List<Satellite> findByPlanet(int planetId) {
        List<Satellite> list = new ArrayList<>();
        String sql = "SELECT * FROM satellites WHERE planet_id = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, planetId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Satellite(
                    rs.getInt("id"), rs.getInt("planet_id"),
                    rs.getString("name"), rs.getDouble("latitude"),
                    rs.getDouble("longitude"), rs.getDouble("altitude_km"),
                    rs.getBoolean("is_relay")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void insert(Satellite s) {
        String sql = "INSERT INTO satellites (planet_id,name,latitude,longitude,altitude_km,is_relay) "
                   + "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, s.getPlanetId());
            ps.setString(2, s.getName());
            ps.setDouble(3, s.getLatitude());
            ps.setDouble(4, s.getLongitude());
            ps.setDouble(5, s.getAltitudeKm());
            ps.setBoolean(6, s.isRelay());
            ps.executeUpdate();
            System.out.println("✅ Đã thêm vệ tinh: " + s.getName());
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void delete(int id) {
        String sql = "DELETE FROM satellites WHERE id = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Đã xoá vệ tinh ID: " + id);
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
