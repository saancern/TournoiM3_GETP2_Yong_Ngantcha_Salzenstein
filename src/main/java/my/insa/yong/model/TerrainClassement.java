package my.insa.yong.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import my.insa.yong.utils.database.ConnectionPool;

/**
 * Utility class for terrain-related queries and operations
 * Centralizes all JDBC operations for terrain management
 * @author saancern
 */
public class TerrainClassement {

    /**
     * Data class for match information
     */
    public static class MatchInfo {
        private final int id;
        private final String equipeA;
        private final String equipeB;
        private final int terrainId;
        private final String terrainNom;

        public MatchInfo(int id, String equipeA, String equipeB, int terrainId, String terrainNom) {
            this.id = id;
            this.equipeA = equipeA;
            this.equipeB = equipeB;
            this.terrainId = terrainId;
            this.terrainNom = terrainNom;
        }

        public int getId() {
            return id;
        }

        public String getEquipeA() {
            return equipeA;
        }

        public String getEquipeB() {
            return equipeB;
        }

        public int getTerrainId() {
            return terrainId;
        }

        public String getTerrainNom() {
            return terrainNom;
        }

        @Override
        public String toString() {
            return String.format("%s vs %s", equipeA, equipeB);
        }
    }

    /**
     * Load all terrains for the current tournament
     */
    public static List<Terrain> chargerTerrains(int tournoiId) throws SQLException {
        List<Terrain> terrains = new ArrayList<>();
        String sql = "SELECT id, nom_terrain, numero FROM terrain WHERE tournoi_id = ? ORDER BY numero ASC";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                terrains.add(new Terrain(
                        rs.getInt("id"),
                        rs.getString("nom_terrain"),
                        rs.getInt("numero")
                ));
            }
        }
        return terrains;
    }

    /**
     * Load a specific terrain by ID
     */
    public static Terrain chargerTerrainParId(int id, int tournoiId) throws SQLException {
        String sql = "SELECT id, nom_terrain, numero FROM terrain WHERE id = ? AND tournoi_id = ?";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.setInt(2, tournoiId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return new Terrain(
                        rs.getInt("id"),
                        rs.getString("nom_terrain"),
                        rs.getInt("numero")
                );
            }
        }
        return null;
    }

    /**
     * Count matches assigned to a terrain
     */
    public static int compterMatchsParTerrain(int terrainId, int tournoiId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM terrain_rencontre WHERE terrain_id = ? AND tournoi_id = ?";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, terrainId);
            pst.setInt(2, tournoiId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Load all matches for the current tournament (with terrain assignments)
     */
    public static List<MatchInfo> chargerMatchs(int tournoiId) throws SQLException {
        List<MatchInfo> matchs = new ArrayList<>();
        String sql = "SELECT r.id, e1.nom_equipe as equipeA, e2.nom_equipe as equipeB, " +
                     "COALESCE(tr.terrain_id, -1) as terrain_id, t.nom_terrain " +
                     "FROM rencontre r " +
                     "LEFT JOIN equipe e1 ON r.equipe_a_id = e1.id " +
                     "LEFT JOIN equipe e2 ON r.equipe_b_id = e2.id " +
                     "LEFT JOIN terrain_rencontre tr ON r.id = tr.rencontre_id AND tr.tournoi_id = ? " +
                     "LEFT JOIN terrain t ON tr.terrain_id = t.id " +
                     "WHERE r.tournoi_id = ? " +
                     "ORDER BY r.round_number, r.id";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            pst.setInt(2, tournoiId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                matchs.add(new MatchInfo(
                        rs.getInt("id"),
                        rs.getString("equipeA") != null ? rs.getString("equipeA") : "TBD",
                        rs.getString("equipeB") != null ? rs.getString("equipeB") : "TBD",
                        rs.getInt("terrain_id"),
                        rs.getString("nom_terrain")
                ));
            }
        }
        return matchs;
    }

    /**
     * Load matches assigned to a specific terrain
     */
    public static List<MatchInfo> chargerMatchsParTerrain(int terrainId, int tournoiId) throws SQLException {
        List<MatchInfo> matchs = new ArrayList<>();
        String sql = "SELECT r.id, e1.nom_equipe as equipeA, e2.nom_equipe as equipeB, " +
                     "t.id as terrain_id, t.nom_terrain " +
                     "FROM rencontre r " +
                     "LEFT JOIN equipe e1 ON r.equipe_a_id = e1.id AND e1.tournoi_id = ? " +
                     "LEFT JOIN equipe e2 ON r.equipe_b_id = e2.id AND e2.tournoi_id = ? " +
                     "INNER JOIN terrain_rencontre tr ON r.id = tr.rencontre_id AND tr.tournoi_id = ? " +
                     "INNER JOIN terrain t ON tr.terrain_id = t.id AND t.id = ? " +
                     "WHERE r.tournoi_id = ? " +
                     "ORDER BY r.round_number, r.id";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            pst.setInt(2, tournoiId);
            pst.setInt(3, tournoiId);
            pst.setInt(4, terrainId);
            pst.setInt(5, tournoiId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                matchs.add(new MatchInfo(
                        rs.getInt("id"),
                        rs.getString("equipeA") != null ? rs.getString("equipeA") : "TBD",
                        rs.getString("equipeB") != null ? rs.getString("equipeB") : "TBD",
                        rs.getInt("terrain_id"),
                        rs.getString("nom_terrain")
                ));
            }
        }
        return matchs;
    }

    /**
     * Assign a terrain to a match
     */
    public static void assignerTerrainAuMatch(int terrainId, int matchId, int tournoiId) throws SQLException {
        try (Connection con = ConnectionPool.getConnection()) {
            // Check if there's already an assignment
            String checkSql = "SELECT COUNT(*) FROM terrain_rencontre WHERE rencontre_id = ? AND tournoi_id = ?";
            PreparedStatement checkPst = con.prepareStatement(checkSql);
            checkPst.setInt(1, matchId);
            checkPst.setInt(2, tournoiId);
            ResultSet rs = checkPst.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                // Delete the old assignment
                String deleteSql = "DELETE FROM terrain_rencontre WHERE rencontre_id = ? AND tournoi_id = ?";
                PreparedStatement deletePst = con.prepareStatement(deleteSql);
                deletePst.setInt(1, matchId);
                deletePst.setInt(2, tournoiId);
                deletePst.executeUpdate();
            }

            // Add the new assignment
            String insertSql = "INSERT INTO terrain_rencontre (terrain_id, rencontre_id, tournoi_id) VALUES (?, ?, ?)";
            PreparedStatement insertPst = con.prepareStatement(insertSql);
            insertPst.setInt(1, terrainId);
            insertPst.setInt(2, matchId);
            insertPst.setInt(3, tournoiId);
            insertPst.executeUpdate();
        }
    }
}
