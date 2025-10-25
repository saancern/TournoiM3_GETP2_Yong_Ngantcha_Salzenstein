package my.insa.yong.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class GestionMatchs {

    // ------------ DTO pour la Grid UI (record => utiliser les accesseurs) ------------
    public static record MatchRow(
            int id, int round, Integer poolIndex,
            Integer equipeAId, Integer equipeBId,
            Integer scoreA, Integer scoreB,
            Integer winnerId, boolean played,
            String equipeAName, String equipeBName, String winnerName
    ) {}

    // ------------ Étape unique "round suivante" ------------
    /**
     * 1) Si aucun match -> générer Round 1 (toutes équipes, BYE auto géré)
     * 2) S'il reste des matchs non joués au round courant -> les simuler
     * 3) Sinon et s'il reste >1 équipe -> générer round suivant
     * 4) Sinon -> tournoi terminé (champion)
     */
    public static String nextStep(Connection con, int tournoiId) throws SQLException {
        int curRound = getCurrentRound(con, tournoiId);
        if (curRound == 0 && countAllMatches(con, tournoiId) == 0) {
            generateNextRound(con, tournoiId);
            return "Round 1 généré.";
        }

        if (hasUnplayedMatches(con, tournoiId, curRound)) {
            int played = playCurrentRoundRandom(con, tournoiId, curRound);
            return "Round " + curRound + " joué (" + played + " matchs).";
        }

        List<Integer> alive = getAliveTeams(con, tournoiId);
        if (alive.size() > 1) {
            generateNextRound(con, tournoiId);
            return "Round " + (curRound + 1) + " généré.";
        }

        return "Tournoi terminé. Un champion est déjà déterminé.";
    }

    // ------------ Génération des matchs ------------
    public static void generateNextRound(Connection con, int tournoiId) throws SQLException {
        int nextRound = getCurrentRound(con, tournoiId) + 1;
        List<Integer> alive = getAliveTeams(con, tournoiId);
        if (alive.isEmpty()) {
            // Démarrage: toutes les équipes connues
            alive = getAllTeamIds(con);
        }

        Collections.shuffle(alive, new Random());
        int pool = 1;

        try (PreparedStatement pst = con.prepareStatement(
            "INSERT INTO rencontre (tournoi_id, round_number, pool_index, equipe_a_id, equipe_b_id, score_a, score_b, winner_id, played) " +
            "VALUES (?, ?, ?, ?, ?, NULL, NULL, NULL, false)"
        )) {
            for (int i = 0; i < alive.size(); i += 2) {
                int a = alive.get(i);
                Integer b = (i + 1 < alive.size()) ? alive.get(i + 1) : null;

                pst.setInt(1, tournoiId);
                pst.setInt(2, nextRound);
                pst.setInt(3, pool++);
                pst.setInt(4, a);
                if (b == null) {
                    pst.setNull(5, Types.INTEGER); // BYE
                } else {
                    pst.setInt(5, b);
                }
                pst.addBatch();
            }
            pst.executeBatch();
        }

        // Valider immédiatement les BYE: gagnant = equipe_a_id
        try (PreparedStatement psSelect = con.prepareStatement(
                "SELECT id, equipe_a_id FROM rencontre WHERE tournoi_id=? AND round_number=? AND equipe_b_id IS NULL AND played=false");
             PreparedStatement psUpdate = con.prepareStatement(
                "UPDATE rencontre SET score_a=1, score_b=NULL, winner_id=?, played=true WHERE id=?")) {

            psSelect.setInt(1, tournoiId);
            psSelect.setInt(2, nextRound);
            try (ResultSet rs = psSelect.executeQuery()) {
                while (rs.next()) {
                    int matchId = rs.getInt("id");
                    int aId = rs.getInt("equipe_a_id");
                    psUpdate.setInt(1, aId);
                    psUpdate.setInt(2, matchId);
                    psUpdate.addBatch();
                }
            }
            psUpdate.executeBatch();
        }
    }

    // ------------ Simulation aléatoire du round courant ------------
    public static int playCurrentRoundRandom(Connection con, int tournoiId, int round) throws SQLException {
        int count = 0;
        Random rnd = new Random();

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id, equipe_a_id, equipe_b_id FROM rencontre " +
                "WHERE tournoi_id=? AND round_number=? AND played=false AND equipe_b_id IS NOT NULL");
             PreparedStatement up = con.prepareStatement(
                "UPDATE rencontre SET score_a=?, score_b=?, winner_id=?, played=true WHERE id=?")) {
            ps.setInt(1, tournoiId);
            ps.setInt(2, round);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int a = rs.getInt("equipe_a_id");
                    int b = rs.getInt("equipe_b_id");

                    // scores 0..5 ; pas de match nul
                    int sa = rnd.nextInt(6);
                    int sb;
                    do { sb = rnd.nextInt(6); } while (sb == sa);

                    int winner = (sa > sb) ? a : b;

                    up.setInt(1, sa);
                    up.setInt(2, sb);
                    up.setInt(3, winner);
                    up.setInt(4, id);
                    up.addBatch();
                    count++;
                }
            }
            up.executeBatch();
        }
        return count;
    }

    // ------------ Lecture / états ------------
    public static List<MatchRow> listAllMatches(Connection con, int tournoiId) throws SQLException {
        String sql = """
            SELECT r.id, r.round_number, r.pool_index, r.equipe_a_id, r.equipe_b_id, r.score_a, r.score_b,
                   r.winner_id, r.played,
                   ea.nom_equipe AS a_name,
                   eb.nom_equipe AS b_name,
                   ew.nom_equipe AS w_name
            FROM rencontre r
            LEFT JOIN equipe ea ON ea.id = r.equipe_a_id
            LEFT JOIN equipe eb ON eb.id = r.equipe_b_id
            LEFT JOIN equipe ew ON ew.id = r.winner_id
            WHERE r.tournoi_id = ?
            ORDER BY r.round_number, r.pool_index, r.id
        """;
        List<MatchRow> list = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(new MatchRow(
                        rs.getInt("id"),
                        rs.getInt("round_number"),
                        (Integer) rs.getObject("pool_index"),
                        (Integer) rs.getObject("equipe_a_id"),
                        (Integer) rs.getObject("equipe_b_id"),
                        (Integer) rs.getObject("score_a"),
                        (Integer) rs.getObject("score_b"),
                        (Integer) rs.getObject("winner_id"),
                        rs.getBoolean("played"),
                        rs.getString("a_name"),
                        rs.getString("b_name"),
                        rs.getString("w_name")
                    ));
                }
            }
        }
        return list;
    }

    public static Optional<Integer> getChampion(Connection con, int tournoiId) throws SQLException {
        int curRound = getCurrentRound(con, tournoiId);
        if (curRound == 0) return Optional.empty();

        // Champion si le round courant comporte exactement 1 match et qu'il est joué
        String sql = """
            SELECT COUNT(*) AS total, SUM(CASE WHEN played THEN 1 ELSE 0 END) AS played_count
            FROM rencontre WHERE tournoi_id=? AND round_number=?
        """;
        int total, played;
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            pst.setInt(2, curRound);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                total = rs.getInt("total");
                played = rs.getInt("played_count");
            }
        }
        if (total == 1 && played == 1) {
            try (PreparedStatement pst = con.prepareStatement(
                    "SELECT winner_id FROM rencontre WHERE tournoi_id=? AND round_number=? LIMIT 1")) {
                pst.setInt(1, tournoiId);
                pst.setInt(2, curRound);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        Integer w = (Integer) rs.getObject(1);
                        return Optional.ofNullable(w);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static void resetMatches(Connection con, int tournoiId) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("DELETE FROM rencontre WHERE tournoi_id=?")) {
            pst.setInt(1, tournoiId);
            pst.executeUpdate();
        }
    }

    // ------------ Helpers privés ------------
    private static int getCurrentRound(Connection con, int tournoiId) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement(
                "SELECT COALESCE(MAX(round_number), 0) FROM rencontre WHERE tournoi_id=?")) {
            pst.setInt(1, tournoiId);
            try (ResultSet rs = pst.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static boolean hasUnplayedMatches(Connection con, int tournoiId, int round) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement(
                "SELECT COUNT(*) FROM rencontre WHERE tournoi_id=? AND round_number=? AND played=false")) {
            pst.setInt(1, tournoiId);
            pst.setInt(2, round);
            try (ResultSet rs = pst.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
        }
    }

    private static int countAllMatches(Connection con, int tournoiId) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement(
                "SELECT COUNT(*) FROM rencontre WHERE tournoi_id=?")) {
            pst.setInt(1, tournoiId);
            try (ResultSet rs = pst.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private static List<Integer> getAliveTeams(Connection con, int tournoiId) throws SQLException {
        int curRound = getCurrentRound(con, tournoiId);
        if (curRound == 0) {
            return getAllTeamIds(con);
        }
        List<Integer> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(
                "SELECT DISTINCT winner_id FROM rencontre WHERE tournoi_id=? AND round_number=? AND played=true AND winner_id IS NOT NULL")) {
            pst.setInt(1, tournoiId);
            pst.setInt(2, curRound);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(rs.getInt(1));
                }
            }
        }
        return res;
    }

    private static List<Integer> getAllTeamIds(Connection con) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM equipe ORDER BY id")) {
            while (rs.next()) ids.add(rs.getInt(1));
        }
        return ids;
    }
}
