package my.insa.yong.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe utilitaire pour les classements d'équipes
 * @author saancern
 */
public class EquipeClassement {

    /**
     * Données de classement pour une équipe (tournoi actuel)
     */
    public static class RankingInfo {
        private final int place;
        private final String nomEquipe;
        private final int victoires;
        private final int defaites;
        private final int matchsNuls;
        private final int buts;

        public RankingInfo(int place, String nomEquipe, int victoires, int defaites, int matchsNuls, int buts) {
            this.place = place;
            this.nomEquipe = nomEquipe;
            this.victoires = victoires;
            this.defaites = defaites;
            this.matchsNuls = matchsNuls;
            this.buts = buts;
        }

        public int getPlace() { return place; }
        public String getNomEquipe() { return nomEquipe; }
        public int getVictoires() { return victoires; }
        public int getDefaites() { return defaites; }
        public int getMatchsNuls() { return matchsNuls; }
        public int getButs() { return buts; }

        public String getPlaceFormatted() {
            return switch (place) {
                case 1 -> "🥇 1er";
                case 2 -> "🥈 2ème";
                case 3 -> "🥉 3ème";
                default -> String.valueOf(place);
            };
        }

        public int getPoints() {
            return victoires * 3 + matchsNuls;
        }
    }

    /**
     * Données de classement pour une équipe (tous les tournois)
     */
    public static class RankingInfoAllTournois {
        private final int place;
        private final String nomEquipe;
        private final int points;
        private final int victoires;
        private final int defaites;
        private final int matchsNuls;
        private final int buts;
        private final String nomTournoi;

        public RankingInfoAllTournois(int place, String nomEquipe, int points, int victoires, 
                                      int defaites, int matchsNuls, String nomTournoi) {
            this(place, nomEquipe, points, victoires, defaites, matchsNuls, 0, nomTournoi);
        }

        public RankingInfoAllTournois(int place, String nomEquipe, int points, int victoires, 
                                      int defaites, int matchsNuls, int buts, String nomTournoi) {
            this.place = place;
            this.nomEquipe = nomEquipe;
            this.points = points;
            this.victoires = victoires;
            this.defaites = defaites;
            this.matchsNuls = matchsNuls;
            this.buts = buts;
            this.nomTournoi = nomTournoi;
        }

        public int getPlace() { return place; }
        public String getNomEquipe() { return nomEquipe; }
        public int getPoints() { return points; }
        public int getVictoires() { return victoires; }
        public int getDefaites() { return defaites; }
        public int getMatchsNuls() { return matchsNuls; }
        public int getButs() { return buts; }
        public String getNomTournoi() { return nomTournoi; }

        public String getPlaceFormatted() {
            return switch (place) {
                case 1 -> "🥇 1er";
                case 2 -> "🥈 2ème";
                case 3 -> "🥉 3ème";
                default -> String.valueOf(place);
            };
        }
    }

    /**
     * Charger le classement des équipes pour le tournoi actuel
     */
    public static List<RankingInfo> chargerClassementTournoiActuel(Connection con, int tournoiId) throws SQLException {
        List<RankingInfo> equipes = new ArrayList<>();
        
        String sql = "SELECT e.nom_equipe, " +
                     "SUM(CASE WHEN r.winner_id = e.id THEN 1 ELSE 0 END) as victoires, " +
                     "SUM(CASE WHEN r.winner_id IS NULL AND r.played = TRUE THEN 1 ELSE 0 END) as matchs_nuls, " +
                     "SUM(CASE WHEN (r.equipe_a_id = e.id OR r.equipe_b_id = e.id) " +
                     "     AND r.winner_id IS NOT NULL AND r.winner_id != e.id THEN 1 ELSE 0 END) as defaites, " +
                     "COALESCE(COUNT(b.id), 0) as total_buts " +
                     "FROM equipe e " +
                     "LEFT JOIN rencontre r ON (r.equipe_a_id = e.id OR r.equipe_b_id = e.id) " +
                     "     AND r.tournoi_id = e.tournoi_id " +
                     "LEFT JOIN but b ON b.equipe_id = e.id AND b.tournoi_id = e.tournoi_id " +
                     "WHERE e.tournoi_id = ? " +
                     "GROUP BY e.id, e.nom_equipe " +
                     "ORDER BY victoires DESC, total_buts DESC";
        
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            try (ResultSet rs = pst.executeQuery()) {
                int place = 1;
                while (rs.next()) {
                    String nomEquipe = rs.getString("nom_equipe");
                    int victoires = rs.getInt("victoires");
                    int defaites = rs.getInt("defaites");
                    int matchsNuls = rs.getInt("matchs_nuls");
                    int buts = rs.getInt("total_buts");
                    
                    equipes.add(new RankingInfo(place++, nomEquipe, victoires, defaites, matchsNuls, buts));
                }
            }
        }
        return equipes;
    }

    /**
     * Charger le classement des équipes pour le tournoi actuel avec points
     */
    public static List<RankingInfoAllTournois> chargerClassementTournoiActuelAvecPoints(Connection con, int tournoiId) throws SQLException {
        List<RankingInfoAllTournois> equipes = new ArrayList<>();
        
        String sql = "SELECT e.id, e.nom_equipe, " +
                     "SUM(CASE WHEN r.winner_id = e.id THEN 3 " +
                     "         WHEN r.winner_id IS NOT NULL AND r.winner_id != e.id THEN 0 " +
                     "         WHEN r.equipe1_id = e.id OR r.equipe2_id = e.id THEN 1 " +
                     "         ELSE 0 END) as points, " +
                     "SUM(CASE WHEN r.winner_id = e.id THEN 1 ELSE 0 END) as victoires, " +
                     "SUM(CASE WHEN r.winner_id IS NOT NULL AND r.winner_id != e.id AND (r.equipe1_id = e.id OR r.equipe2_id = e.id) THEN 1 ELSE 0 END) as defaites, " +
                     "SUM(CASE WHEN r.winner_id IS NULL AND (r.equipe1_id = e.id OR r.equipe2_id = e.id) THEN 1 ELSE 0 END) as matchsNuls " +
                     "FROM equipe e " +
                     "LEFT JOIN rencontre r ON (r.equipe1_id = e.id OR r.equipe2_id = e.id) AND r.tournoi_id = ? " +
                     "WHERE e.tournoi_id = ? " +
                     "GROUP BY e.id, e.nom_equipe " +
                     "ORDER BY points DESC, victoires DESC, e.nom_equipe ASC";

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            pst.setInt(2, tournoiId);
            try (ResultSet rs = pst.executeQuery()) {
                int rank = 0;
                Integer previousPoints = null;

                while (rs.next()) {
                    int points = rs.getInt("points");
                    if (previousPoints == null || points != previousPoints) {
                        rank++;
                    }
                    equipes.add(new RankingInfoAllTournois(rank,
                        rs.getString("nom_equipe"),
                        points,
                        rs.getInt("victoires"),
                        rs.getInt("defaites"),
                        rs.getInt("matchsNuls"),
                        ""));
                    previousPoints = points;
                }
            }
        }
        return equipes;
    }

    /**
     * Charger le classement des équipes pour tous les tournois
     */
    public static List<RankingInfoAllTournois> chargerClassementTousTournois(Connection con) throws SQLException {
        List<RankingInfoAllTournois> equipes = new ArrayList<>();
        
        String sql = "SELECT e.id, e.nom_equipe, t.nom_tournoi, " +
                     "SUM(CASE WHEN r.winner_id = e.id THEN 3 " +
                     "         WHEN r.winner_id IS NOT NULL AND r.winner_id != e.id THEN 0 " +
                     "         WHEN r.equipe1_id = e.id OR r.equipe2_id = e.id THEN 1 " +
                     "         ELSE 0 END) as points, " +
                     "SUM(CASE WHEN r.winner_id = e.id THEN 1 ELSE 0 END) as victoires, " +
                     "SUM(CASE WHEN r.winner_id IS NOT NULL AND r.winner_id != e.id AND (r.equipe1_id = e.id OR r.equipe2_id = e.id) THEN 1 ELSE 0 END) as defaites, " +
                     "SUM(CASE WHEN r.winner_id IS NULL AND (r.equipe1_id = e.id OR r.equipe2_id = e.id) THEN 1 ELSE 0 END) as matchsNuls " +
                     "FROM equipe e " +
                     "LEFT JOIN rencontre r ON (r.equipe1_id = e.id OR r.equipe2_id = e.id) " +
                     "LEFT JOIN tournoi t ON e.tournoi_id = t.id " +
                     "GROUP BY e.id, e.nom_equipe, t.nom_tournoi " +
                     "ORDER BY points DESC, victoires DESC, e.nom_equipe ASC";

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            try (ResultSet rs = pst.executeQuery()) {
                int rank = 0;
                Integer previousPoints = null;

                while (rs.next()) {
                    int points = rs.getInt("points");
                    if (previousPoints == null || points != previousPoints) {
                        rank++;
                    }
                    equipes.add(new RankingInfoAllTournois(rank,
                        rs.getString("nom_equipe"),
                        points,
                        rs.getInt("victoires"),
                        rs.getInt("defaites"),
                        rs.getInt("matchsNuls"),
                        rs.getString("nom_tournoi") != null ? rs.getString("nom_tournoi") : "N/A"));
                    previousPoints = points;
                }
            }
        }
        return equipes;
    }
}
