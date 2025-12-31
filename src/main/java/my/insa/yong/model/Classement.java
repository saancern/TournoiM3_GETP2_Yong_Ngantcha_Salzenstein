package my.insa.yong.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import my.insa.yong.utils.database.ConnectionPool;

/**
 * Consolidated utility class for all ranking and tournament data queries
 * Combines functionality from JoueurClassement, EquipeClassement, TerrainClassement, and PrincipaleClassement
 * @author saancern
 */
public class Classement {

    // ============================================================================
    // JOUEUR (PLAYER) DATA CLASSES AND METHODS
    // ============================================================================

    /**
     * Data class for top scorers in current tournament
     */
    public static class ButeurInfo {
        private final int place;
        private final String nomJoueur;
        private final String nomEquipe;
        private final int nombreButs;

        public ButeurInfo(int place, String nomJoueur, String nomEquipe, int nombreButs) {
            this.place = place;
            this.nomJoueur = nomJoueur;
            this.nomEquipe = nomEquipe;
            this.nombreButs = nombreButs;
        }

        public int getPlace() { return place; }
        public String getNomJoueur() { return nomJoueur; }
        public String getNomEquipe() { return nomEquipe; }
        public int getNombreButs() { return nombreButs; }

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
     * Data class for top scorers across all tournaments
     */
    public static class ButeurInfoAllTournois extends ButeurInfo {
        private final String nomTournoi;

        public ButeurInfoAllTournois(int place, String nomJoueur, String nomEquipe, int nombreButs, String nomTournoi) {
            super(place, nomJoueur, nomEquipe, nombreButs);
            this.nomTournoi = nomTournoi;
        }

        public String getNomTournoi() { return nomTournoi; }
    }

    /**
     * Load top scorers for current tournament
     */
    public static List<ButeurInfo> chargerButeursTournoiActuel(Connection con, int tournoiId) throws SQLException {
        List<ButeurInfo> buteurs = new ArrayList<>();
        
        String sql = "SELECT j.id, j.prenom, j.nom, e.nom_equipe, COUNT(b.id) as nombreButs " +
                     "FROM joueur j " +
                     "LEFT JOIN but b ON j.id = b.joueur_id AND b.tournoi_id = ? " +
                     "LEFT JOIN joueur_equipe je ON j.id = je.joueur_id AND je.tournoi_id = ? " +
                     "LEFT JOIN equipe e ON je.equipe_id = e.id " +
                     "WHERE j.tournoi_id = ? " +
                     "GROUP BY j.id, j.prenom, j.nom, e.nom_equipe " +
                     "ORDER BY nombreButs DESC, j.nom ASC, j.prenom ASC";

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            pst.setInt(2, tournoiId);
            pst.setInt(3, tournoiId);
            try (ResultSet rs = pst.executeQuery()) {
                int rank = 0;
                Integer previousButs = null;

                while (rs.next()) {
                    int nombreButs = rs.getInt("nombreButs");
                    if (previousButs == null || nombreButs != previousButs) {
                        rank++;
                    }
                    String nomComplet = rs.getString("prenom") + " " + rs.getString("nom");
                    buteurs.add(new ButeurInfo(rank, nomComplet, 
                        rs.getString("nom_equipe") != null ? rs.getString("nom_equipe") : "Sans équipe", 
                        nombreButs));
                    previousButs = nombreButs;
                }
            }
        }

        return buteurs;
    }

    /**
     * Load top scorers across all tournaments
     */
    public static List<ButeurInfoAllTournois> chargerButeursAllTournois(Connection con) throws SQLException {
        List<ButeurInfoAllTournois> buteurs = new ArrayList<>();
        String sql = "SELECT t.id, t.nom_tournoi, j.id, j.prenom, j.nom, e.nom_equipe, COUNT(b.id) as nombreButs " +
                     "FROM tournoi t " +
                     "CROSS JOIN joueur j ON j.tournoi_id = t.id " +
                     "LEFT JOIN but b ON j.id = b.joueur_id AND b.tournoi_id = t.id " +
                     "LEFT JOIN joueur_equipe je ON j.id = je.joueur_id AND je.tournoi_id = t.id " +
                     "LEFT JOIN equipe e ON je.equipe_id = e.id " +
                     "GROUP BY t.id, t.nom_tournoi, j.id, j.prenom, j.nom, e.nom_equipe " +
                     "ORDER BY t.nom_tournoi, nombreButs DESC, j.nom ASC, j.prenom ASC";

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String nomComplet = rs.getString("prenom") + " " + rs.getString("nom");
                    buteurs.add(new ButeurInfoAllTournois(
                        buteurs.size() + 1,
                        nomComplet,
                        rs.getString("nom_equipe") != null ? rs.getString("nom_equipe") : "Sans équipe",
                        rs.getInt("nombreButs"),
                        rs.getString("nom_tournoi")
                    ));
                }
            }
        }
        return buteurs;
    }

    // ============================================================================
    // EQUIPE (TEAM) DATA CLASSES AND METHODS
    // ============================================================================

    /**
     * Data class for team ranking in current tournament
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
     * Data class for team ranking across all tournaments
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
     * Load team ranking for current tournament
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
     * Load team ranking for current tournament with points
     */
    public static List<RankingInfoAllTournois> chargerClassementTournoiActuelAvecPoints(Connection con, int tournoiId) throws SQLException {
        List<RankingInfoAllTournois> equipes = new ArrayList<>();
        
        String sql = "SELECT e.id, e.nom_equipe, " +
                     "SUM(CASE WHEN r.winner_id = e.id THEN 3 " +
                     "         WHEN r.winner_id IS NOT NULL AND r.winner_id != e.id THEN 0 " +
                     "         WHEN r.equipe_a_id = e.id OR r.equipe_b_id = e.id THEN 1 " +
                     "         ELSE 0 END) as points, " +
                     "SUM(CASE WHEN r.winner_id = e.id THEN 1 ELSE 0 END) as victoires, " +
                     "SUM(CASE WHEN r.winner_id IS NOT NULL AND r.winner_id != e.id AND (r.equipe_a_id = e.id OR r.equipe_b_id = e.id) THEN 1 ELSE 0 END) as defaites, " +
                     "SUM(CASE WHEN r.winner_id IS NULL AND (r.equipe_a_id = e.id OR r.equipe_b_id = e.id) THEN 1 ELSE 0 END) as matchsNuls " +
                     "FROM equipe e " +
                     "LEFT JOIN rencontre r ON (r.equipe_a_id = e.id OR r.equipe_b_id = e.id) AND r.tournoi_id = ? " +
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
     * Load team ranking for all tournaments
     */
    public static List<RankingInfoAllTournois> chargerClassementTousTournois(Connection con) throws SQLException {
        List<RankingInfoAllTournois> equipes = new ArrayList<>();
        
        String sql = "SELECT e.id, e.nom_equipe, t.nom_tournoi, " +
                     "SUM(CASE WHEN r.winner_id = e.id THEN 3 " +
                     "         WHEN r.winner_id IS NOT NULL AND r.winner_id != e.id THEN 0 " +
                     "         WHEN r.equipe_a_id = e.id OR r.equipe_b_id = e.id THEN 1 " +
                     "         ELSE 0 END) as points, " +
                     "SUM(CASE WHEN r.winner_id = e.id THEN 1 ELSE 0 END) as victoires, " +
                     "SUM(CASE WHEN r.winner_id IS NOT NULL AND r.winner_id != e.id AND (r.equipe_a_id = e.id OR r.equipe_b_id = e.id) THEN 1 ELSE 0 END) as defaites, " +
                     "SUM(CASE WHEN r.winner_id IS NULL AND (r.equipe_a_id = e.id OR r.equipe_b_id = e.id) THEN 1 ELSE 0 END) as matchsNuls " +
                     "FROM equipe e " +
                     "LEFT JOIN rencontre r ON (r.equipe_a_id = e.id OR r.equipe_b_id = e.id) " +
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

    // ============================================================================
    // TERRAIN (FIELD) DATA CLASSES AND METHODS
    // ============================================================================

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
     * Load all terrains for tournament
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
     * Load all matches for tournament
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
     * Load matches assigned to specific terrain
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
     * Assign terrain to match
     */
    public static void assignerTerrainAuMatch(int terrainId, int matchId, int tournoiId) throws SQLException {
        try (Connection con = ConnectionPool.getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM terrain_rencontre WHERE rencontre_id = ? AND tournoi_id = ?";
            PreparedStatement checkPst = con.prepareStatement(checkSql);
            checkPst.setInt(1, matchId);
            checkPst.setInt(2, tournoiId);
            ResultSet rs = checkPst.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                String deleteSql = "DELETE FROM terrain_rencontre WHERE rencontre_id = ? AND tournoi_id = ?";
                PreparedStatement deletePst = con.prepareStatement(deleteSql);
                deletePst.setInt(1, matchId);
                deletePst.setInt(2, tournoiId);
                deletePst.executeUpdate();
            }

            String insertSql = "INSERT INTO terrain_rencontre (terrain_id, rencontre_id, tournoi_id) VALUES (?, ?, ?)";
            PreparedStatement insertPst = con.prepareStatement(insertSql);
            insertPst.setInt(1, terrainId);
            insertPst.setInt(2, matchId);
            insertPst.setInt(3, tournoiId);
            insertPst.executeUpdate();
        }
    }

    // ============================================================================
    // PRINCIPALE (DASHBOARD) DATA CLASSES AND METHODS
    // ============================================================================

    /**
     * Data class for dashboard match display
     */
    public static class DashboardMatchInfo {
        private final int id;
        private final int roundNumber;
        private final String teamAName;
        private final String teamBName;
        private final int scoreA;
        private final int scoreB;
        private final String terrainName;

        public DashboardMatchInfo(int matchId, int matchRound, String nameA, String nameB, int scoreTeamA, int scoreTeamB, String terrain) {
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
     * Load latest match for dashboard
     */
    public static DashboardMatchInfo chargerDernierMatch(int tournoiId) throws SQLException {
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
                return new DashboardMatchInfo(
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
        return new DashboardMatchInfo(0, 0, "À déterminer", "À déterminer", 0, 0, "-");
    }

    /**
     * Load all matches for tournament with scores and round numbers
     */
    public static List<DashboardMatchInfo> chargerTousLesMatchs(int tournoiId) throws SQLException {
        List<DashboardMatchInfo> matches = new ArrayList<>();
        String sql = "SELECT r.id, r.round_number, COALESCE(r.score_a, 0) as score_a, " +
                     "COALESCE(r.score_b, 0) as score_b, " +
                     "COALESCE(ea.nom_equipe, 'À déterminer') as equipe_a, " +
                     "COALESCE(eb.nom_equipe, 'À déterminer') as equipe_b, " +
                     "COALESCE(t.nom_terrain, '-') as nom_terrain " +
                     "FROM rencontre r " +
                     "LEFT JOIN equipe ea ON r.equipe_a_id = ea.id " +
                     "LEFT JOIN equipe eb ON r.equipe_b_id = eb.id " +
                     "LEFT JOIN terrain_rencontre tr ON r.id = tr.rencontre_id AND tr.tournoi_id = ? " +
                     "LEFT JOIN terrain t ON tr.terrain_id = t.id " +
                     "WHERE r.tournoi_id = ? " +
                     "ORDER BY r.round_number ASC, r.id ASC";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            pst.setInt(2, tournoiId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                matches.add(new DashboardMatchInfo(
                    rs.getInt("id"),
                    rs.getInt("round_number"),
                    rs.getString("equipe_a"),
                    rs.getString("equipe_b"),
                    rs.getInt("score_a"),
                    rs.getInt("score_b"),
                    rs.getString("nom_terrain")
                ));
            }
        }
        return matches;
    }
}
