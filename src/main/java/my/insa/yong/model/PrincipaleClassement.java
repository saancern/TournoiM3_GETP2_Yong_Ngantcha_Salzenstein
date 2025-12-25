package my.insa.yong.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import my.insa.yong.utils.database.ConnectionPool;

/**
 * Utility class for main dashboard/home page queries
 * Centralizes all JDBC operations for dashboard data
 * @author saancern
 */
public class PrincipaleClassement {

    /**
     * Data class for match information on dashboard
     */
    public static class MatchInfo {
        private final int id;
        private final int roundNumber;
        private final String teamAName;
        private final String teamBName;
        private final int scoreA;
        private final int scoreB;
        private final String terrainName;

        public MatchInfo(int matchId, int matchRound, String nameA, String nameB, int scoreTeamA, int scoreTeamB, String terrain) {
            this.id = matchId;
            this.roundNumber = matchRound;
            this.teamAName = nameA;
            this.teamBName = nameB;
            this.scoreA = scoreTeamA;
            this.scoreB = scoreTeamB;
            this.terrainName = terrain;
        }

        public int getId() {
            return id;
        }

        public int getRoundNumber() {
            return roundNumber;
        }

        public String getTeamAName() {
            return teamAName;
        }

        public String getTeamBName() {
            return teamBName;
        }

        public int getScoreA() {
            return scoreA;
        }

        public int getScoreB() {
            return scoreB;
        }

        public String getTerrainName() {
            return terrainName;
        }
    }

    /**
     * Load recent matches for current tournament
     * Prioritizes unplayed matches first, then recent played matches
     */
    public static MatchInfo chargerDernierMatch(int tournoiId) throws SQLException {
        String sql = "SELECT r.id, r.round_number, COALESCE(r.score_a, 0) as score_a, " +
                     "COALESCE(r.score_b, 0) as score_b, " +
                     "COALESCE(ea.nom_equipe, 'À déterminer') as equipe_a, " +
                     "COALESCE(eb.nom_equipe, 'À déterminer') as equipe_b, " +
                     "COALESCE(t.nom_terrain, '-') as nom_terrain, " +
                     "COALESCE(r.played, false) as played " +
                     "FROM rencontre r " +
                     "LEFT JOIN equipe ea ON r.equipe_a_id = ea.id " +
                     "LEFT JOIN equipe eb ON r.equipe_b_id = eb.id " +
                     "LEFT JOIN terrain_rencontre tr ON r.id = tr.rencontre_id AND tr.tournoi_id = ? " +
                     "LEFT JOIN terrain t ON tr.terrain_id = t.id " +
                     "WHERE r.tournoi_id = ? " +
                     "ORDER BY r.played ASC, r.round_number DESC, r.id DESC " +
                     "LIMIT 1";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            pst.setInt(2, tournoiId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return new MatchInfo(
                    rs.getInt("id"),
                    rs.getInt("round_number"),
                    rs.getString("equipe_a"),
                    rs.getString("equipe_b"),
                    rs.getInt("score_a"),
                    rs.getInt("score_b"),
                    rs.getString("nom_terrain")
                );
            }
        }
        // Return default match if no match found
        return new MatchInfo(0, 0, "À déterminer", "À déterminer", 0, 0, "-");
    }
}
