package com.satellite.dao;

import com.satellite.model.Planet;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlanetDAO {

    public List<Planet> findAll() {
        List<Planet> list = new ArrayList<>();
        String sql = "SELECT * FROM planets";
        try (Statement st = DatabaseConnection.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Planet(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("radius_km"),
                    rs.getDouble("mass_kg"),
                    rs.getString("texture_file")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Planet findById(int id) {
        String sql = "SELECT * FROM planets WHERE id = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Planet(rs.getInt("id"), rs.getString("name"),
                    rs.getDouble("radius_km"), rs.getDouble("mass_kg"),
                    rs.getString("texture_file"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
}
