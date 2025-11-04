package my.insa.yong.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class GestionMatchs {

    // ---------------- DTOs ----------------
    public static record MatchRow(
            int id, int round, Integer poolIndex,
            Integer equipeAId, Integer equipeBId,
            Integer scoreA, Integer scoreB,
            Integer winnerId, boolean played,
            String equipeAName, String equipeBName, String winnerName,
            String buteursA, String buteursB
    ) {}

    public static record ButeurRow(
            int joueurId, String joueurNom, String equipeNom, int buts
    ) {}

    public static record TeamRow(int id, String name) {
        @Override public String toString() { return name; }
    }

    public static record JoueurRow(int id, String nom, int equipeId, String equipeNom) {}
    public static record GoalRow(int id, int rencontreId, int equipeId, String equipeNom,
                                 int joueurId, String joueurNom, Integer minute) {}

    // ------------- Bouton "Round suivante" (auto ou manuel) -------------

    /** Mode auto historique (conserve pour compat) */
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

    // ------------- Génération des matchs (tirage) -------------
    public static void generateNextRound(Connection con, int tournoiId) throws SQLException {
        int nextRound = getCurrentRound(con, tournoiId) + 1;
        List<Integer> alive = getAliveTeams(con, tournoiId);
        if (alive.isEmpty()) {
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
                if (b == null) pst.setNull(5, Types.INTEGER); else pst.setInt(5, b);
                pst.addBatch();
            }
            pst.executeBatch();
        }

        // BYE: valider immédiatement les matches sans adversaire
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

    // ------------- Simulation aléatoire (scores + buteurs) -------------
    public static int playCurrentRoundRandom(Connection con, int tournoiId, int round) throws SQLException {
        int count = 0;
        Random rnd = new Random();

        String qSelect = """
            SELECT id, equipe_a_id, equipe_b_id
            FROM rencontre
            WHERE tournoi_id=? AND round_number=? AND played=false AND equipe_b_id IS NOT NULL
        """;
        String qUpdate = "UPDATE rencontre SET score_a=?, score_b=?, winner_id=?, played=true WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(qSelect);
             PreparedStatement up = con.prepareStatement(qUpdate)) {
            ps.setInt(1, tournoiId);
            ps.setInt(2, round);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int matchId = rs.getInt("id");
                    int a = rs.getInt("equipe_a_id");
                    int b = rs.getInt("equipe_b_id");

                    // scores 0..5 ; pas d'égalité
                    int sa = rnd.nextInt(6);
                    int sb;
                    do { sb = rnd.nextInt(6); } while (sb == sa);
                    int winner = (sa > sb) ? a : b;

                    up.setInt(1, sa);
                    up.setInt(2, sb);
                    up.setInt(3, winner);
                    up.setInt(4, matchId);
                    up.addBatch();

                    // --- buteurs aléatoires cohérents avec le score ---
                    insertRandomGoals(con, matchId, a, sa, rnd);
                    insertRandomGoals(con, matchId, b, sb, rnd);

                    count++;
                }
            }
            up.executeBatch();
        }
        return count;
    }

    private static void insertRandomGoals(Connection con, int rencontreId, int equipeId, int nbButs, Random rnd) throws SQLException {
        if (nbButs <= 0) return;
        List<Integer> joueurs = getPlayerIdsByTeam(con, equipeId);
        if (joueurs.isEmpty()) return;

        try (PreparedStatement ins = con.prepareStatement(
                "INSERT INTO but (rencontre_id, equipe_id, joueur_id, minute) VALUES (?,?,?,?)")) {
            for (int i = 0; i < nbButs; i++) {
                int j = joueurs.get(rnd.nextInt(joueurs.size()));
                Integer minute = 1 + rnd.nextInt(90); // 1..90
                ins.setInt(1, rencontreId);
                ins.setInt(2, equipeId);
                ins.setInt(3, j);
                ins.setInt(4, minute);
                ins.addBatch();
            }
            ins.executeBatch();
        }
    }

    // ------------- Lecture / états -------------
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
        Map<Integer, BaseMatch> base = new LinkedHashMap<>();
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    base.put(id, new BaseMatch(
                            id,
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
        if (base.isEmpty()) return List.of();

        Map<Integer, String> buteursA = new HashMap<>();
        Map<Integer, String> buteursB = new HashMap<>();
        fillScorers(con, tournoiId, base, buteursA, buteursB);

        List<MatchRow> out = new ArrayList<>(base.size());
        for (BaseMatch m : base.values()) {
            out.add(new MatchRow(
                    m.id, m.round, m.poolIndex, m.equipeAId, m.equipeBId,
                    m.scoreA, m.scoreB, m.winnerId, m.played,
                    m.equipeAName, m.equipeBName, m.winnerName,
                    buteursA.getOrDefault(m.id, ""), buteursB.getOrDefault(m.id, "")
            ));
        }
        return out;
    }

    public static Optional<Integer> getChampion(Connection con, int tournoiId) throws SQLException {
        int curRound = getCurrentRound(con, tournoiId);
        if (curRound == 0) return Optional.empty();

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
                    if (rs.next()) return Optional.ofNullable((Integer) rs.getObject(1));
                }
            }
        }
        return Optional.empty();
    }

    public static void resetMatches(Connection con, int tournoiId) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("DELETE FROM but WHERE rencontre_id IN (SELECT id FROM rencontre WHERE tournoi_id=?)")) {
            pst.setInt(1, tournoiId);
            pst.executeUpdate();
        }
        try (PreparedStatement pst = con.prepareStatement("DELETE FROM rencontre WHERE tournoi_id=?")) {
            pst.setInt(1, tournoiId);
            pst.executeUpdate();
        }
    }

    // ------------- Classement des buteurs -------------
    public static List<ButeurRow> getTopScorers(Connection con, int tournoiId, int limit) throws SQLException {
        String sql = """
            SELECT j.id AS joueur_id,
                   CONCAT(j.prenom, ' ', j.nom) AS joueur,
                   e.nom_equipe AS equipe,
                   COUNT(*) AS buts
            FROM but b
            JOIN rencontre r ON r.id = b.rencontre_id
            JOIN joueur j    ON j.id = b.joueur_id
            LEFT JOIN equipe e ON e.id = b.equipe_id
            WHERE r.tournoi_id = ?
            GROUP BY j.id, j.prenom, j.nom, e.nom_equipe
            ORDER BY buts DESC, joueur
            LIMIT ?
        """;
        List<ButeurRow> list = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            pst.setInt(2, Math.max(1, limit));
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(new ButeurRow(
                            rs.getInt("joueur_id"),
                            rs.getString("joueur"),
                            rs.getString("equipe"),
                            rs.getInt("buts")
                    ));
                }
            }
        }
        return list;
    }

    // ===================== Mode SEMI-AUTO : API =====================

    /** Liste des équipes (id, nom) */
    public static List<TeamRow> listEquipes(Connection con) throws SQLException {
        String sql = "SELECT id, nom_equipe FROM equipe ORDER BY nom_equipe";
        List<TeamRow> out = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                out.add(new TeamRow(rs.getInt("id"), rs.getString("nom_equipe")));
            }
        }
        return out;
    }

    /** Liste des joueurs pour le match (union des 2 équipes) */
    public static List<JoueurRow> listPlayersForMatch(Connection con, int matchId) throws SQLException {
        int aId, bId;
        try (PreparedStatement ps = con.prepareStatement("SELECT equipe_a_id, equipe_b_id FROM rencontre WHERE id=?")) {
            ps.setInt(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return List.of();
                aId = rs.getInt(1);
                bId = rs.getInt(2);
                if (rs.wasNull()) bId = -1;
            }
        }

        String sql = (bId > 0)
                ? """
                   SELECT j.id, CONCAT(j.prenom,' ',j.nom) AS nom, e.id AS equipe_id, e.nom_equipe
                   FROM joueur j
                   JOIN joueur_equipe je ON je.joueur_id=j.id
                   JOIN equipe e ON e.id=je.equipe_id
                   WHERE je.equipe_id IN (?,?)
                   ORDER BY e.nom_equipe, nom
                  """
                : """
                   SELECT j.id, CONCAT(j.prenom,' ',j.nom) AS nom, e.id AS equipe_id, e.nom_equipe
                   FROM joueur j
                   JOIN joueur_equipe je ON je.joueur_id=j.id
                   JOIN equipe e ON e.id=je.equipe_id
                   WHERE je.equipe_id = ?
                   ORDER BY e.nom_equipe, nom
                  """;
        List<JoueurRow> out = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, aId);
            if (bId > 0) pst.setInt(2, bId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    out.add(new JoueurRow(
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getInt(3),
                            rs.getString(4)
                    ));
                }
            }
        }
        return out;
    }

    /** Liste des buts pour un match */
    public static List<GoalRow> listGoalsForMatch(Connection con, int matchId) throws SQLException {
        String sql = """
            SELECT b.id, b.rencontre_id, b.equipe_id, e.nom_equipe,
                   j.id AS joueur_id, CONCAT(j.prenom,' ',j.nom) AS joueur_nom, b.minute
            FROM but b
            LEFT JOIN joueur j ON j.id=b.joueur_id
            LEFT JOIN equipe e ON e.id=b.equipe_id
            WHERE b.rencontre_id=?
            ORDER BY COALESCE(b.minute, 999), b.id
        """;
        List<GoalRow> out = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, matchId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    out.add(new GoalRow(
                            rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4),
                            rs.getInt(5), rs.getString(6), (Integer) rs.getObject(7)
                    ));
                }
            }
        }
        return out;
    }

    /** Ajoute un but puis recalcule score/vainqueur. */
    public static void addGoal(Connection con, int matchId, int equipeId, int joueurId, Integer minute) throws SQLException {
        // Vérif que l'équipe appartient bien au match
        int aId, bId;
        try (PreparedStatement ps = con.prepareStatement("SELECT equipe_a_id, equipe_b_id FROM rencontre WHERE id=?")) {
            ps.setInt(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Match introuvable.");
                aId = rs.getInt(1);
                bId = rs.getInt(2);
            }
        }
        if (equipeId != aId && (equipeId) != bId) {
            throw new SQLException("Le but saisi ne correspond à aucune des équipes du match.");
        }

        try (PreparedStatement ins = con.prepareStatement(
                "INSERT INTO but (rencontre_id, equipe_id, joueur_id, minute) VALUES (?,?,?,?)")) {
            ins.setInt(1, matchId);
            ins.setInt(2, equipeId);
            ins.setInt(3, joueurId);
            if (minute == null) ins.setNull(4, Types.INTEGER); else ins.setInt(4, minute);
            ins.executeUpdate();
        }
        recomputeScoreFromGoals(con, matchId);
    }

    /** Supprime un but (par id) puis recalcule. */
    public static void deleteGoal(Connection con, int goalId) throws SQLException {
        Integer matchId = null;
        try (PreparedStatement s = con.prepareStatement("SELECT rencontre_id FROM but WHERE id=?")) {
            s.setInt(1, goalId);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) matchId = (Integer) rs.getObject(1);
            }
        }
        try (PreparedStatement del = con.prepareStatement("DELETE FROM but WHERE id=?")) {
            del.setInt(1, goalId);
            del.executeUpdate();
        }
        if (matchId != null) {
            recomputeScoreFromGoals(con, matchId);
        }
    }

    /** Efface tous les buts du match puis recalcule (score=0-0, non joué). */
    public static void clearGoalsForMatch(Connection con, int matchId) throws SQLException {
        try (PreparedStatement del = con.prepareStatement("DELETE FROM but WHERE rencontre_id=?")) {
            del.setInt(1, matchId);
            del.executeUpdate();
        }
        recomputeScoreFromGoals(con, matchId);
    }

    /** Recalcule score/winner/played à partir de la table des buts. Pas d'égalité autorisée -> 'played' reste false si égalité. */
    public static void recomputeScoreFromGoals(Connection con, int matchId) throws SQLException {
        int aId, bId;
        try (PreparedStatement ps = con.prepareStatement("SELECT equipe_a_id, equipe_b_id FROM rencontre WHERE id=?")) {
            ps.setInt(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                aId = rs.getInt(1);
                bId = rs.getInt(2);
                if (rs.wasNull()) bId = -1; // BYE
            }
        }
        if (bId <= 0) return; // BYE: rien à recalculer

        String sql = "SELECT equipe_id, COUNT(*) FROM but WHERE rencontre_id=? GROUP BY equipe_id";
        int sa = 0, sb = 0;
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, matchId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int team = rs.getInt(1);
                    int c = rs.getInt(2);
                    if (team == aId) sa = c;
                    else if (team == bId) sb = c;
                }
            }
        }

        Integer winner = null;
        boolean played = false;
        if (sa != sb) {
            played = true;
            winner = (sa > sb) ? aId : bId;
        }

        try (PreparedStatement up = con.prepareStatement(
                "UPDATE rencontre SET score_a=?, score_b=?, winner_id=?, played=? WHERE id=?")) {
            up.setInt(1, sa);
            up.setInt(2, sb);
            if (winner == null) up.setNull(3, Types.INTEGER); else up.setInt(3, winner);
            up.setBoolean(4, played);
            up.setInt(5, matchId);
            up.executeUpdate();
        }
    }

    // ======= Utilitaires exposés à l'UI =======

    public static int getCurrentRound(Connection con, int tournoiId) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement(
                "SELECT COALESCE(MAX(round_number), 0) FROM rencontre WHERE tournoi_id=?")) {
            pst.setInt(1, tournoiId);
            try (ResultSet rs = pst.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    public static boolean hasUnplayedMatchesPublic(Connection con, int tournoiId, int round) throws SQLException {
        return hasUnplayedMatches(con, tournoiId, round);
    }

    // ------------- Helpers privés -------------
    private record BaseMatch(
            int id, int round, Integer poolIndex,
            Integer equipeAId, Integer equipeBId,
            Integer scoreA, Integer scoreB,
            Integer winnerId, boolean played,
            String equipeAName, String equipeBName, String winnerName
    ) {}

    private static void fillScorers(Connection con, int tournoiId,
                                    Map<Integer, BaseMatch> base,
                                    Map<Integer, String> buteursA,
                                    Map<Integer, String> buteursB) throws SQLException {

        String sql = """
            SELECT b.rencontre_id, b.equipe_id, j.id AS joueur_id, j.prenom, j.nom
            FROM but b
            JOIN rencontre r ON r.id = b.rencontre_id
            JOIN joueur j    ON j.id = b.joueur_id
            WHERE r.tournoi_id = ?
            ORDER BY b.rencontre_id
        """;
        Map<Integer, Map<Integer, Integer>> goalsByMatchTeamPlayer = new HashMap<>();

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int matchId = rs.getInt("rencontre_id");
                    int teamId  = rs.getInt("equipe_id");
                    int playerId = rs.getInt("joueur_id");

                    goalsByMatchTeamPlayer
                            .computeIfAbsent(matchId, k -> new HashMap<>())
                            .merge((teamId << 20) | playerId, 1, Integer::sum);
                }
            }
        }

        Map<Integer, String> cachePlayerName = new HashMap<>();
        for (BaseMatch m : base.values()) {
            Map<Integer, Integer> counts = goalsByMatchTeamPlayer.getOrDefault(m.id, Map.of());

            String a = summariseForTeam(con, counts, m.equipeAId, cachePlayerName);
            String b = (m.equipeBId == null) ? "" : summariseForTeam(con, counts, m.equipeBId, cachePlayerName);

            buteursA.put(m.id, a);
            buteursB.put(m.id, b);
        }
    }

    private static String summariseForTeam(Connection con,
                                           Map<Integer, Integer> counts,
                                           Integer teamId,
                                           Map<Integer, String> cachePlayerName) throws SQLException {
        if (teamId == null) return "";
        List<int[]> entries = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            int key = e.getKey();
            int teamKey = key >>> 20;
            int playerId = key & ((1 << 20) - 1);
            if (teamKey == teamId) {
                entries.add(new int[]{playerId, e.getValue()});
            }
        }
        if (entries.isEmpty()) return "";

        entries.sort((x, y) -> Integer.compare(y[1], x[1])); // par buts décroissant

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int[] it : entries) {
            int pid = it[0];
            int buts = it[1];
            String nom = cachePlayerName.computeIfAbsent(pid, id -> {
                try (PreparedStatement p = con.prepareStatement("SELECT CONCAT(prenom,' ',nom) FROM joueur WHERE id=?")) {
                    try {
                        p.setInt(1, id);
                        try (ResultSet rs = p.executeQuery()) {
                            if (rs.next()) return rs.getString(1);
                        }
                    } catch (SQLException ex) {
                        // fallback
                    }
                } catch (SQLException ignore) {}
                return "Joueur#" + id;
            });
            if (!first) sb.append(", ");
            sb.append(nom);
            if (buts > 1) sb.append(" x").append(buts);
            first = false;
        }
        return sb.toString();
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
        if (curRound == 0) return getAllTeamIds(con);
        List<Integer> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(
                "SELECT DISTINCT winner_id FROM rencontre WHERE tournoi_id=? AND round_number=? AND played=true AND winner_id IS NOT NULL")) {
            pst.setInt(1, tournoiId);
            pst.setInt(2, curRound);
            try (ResultSet rs = pst.executeQuery()) { while (rs.next()) res.add(rs.getInt(1)); }
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

    private static List<Integer> getPlayerIdsByTeam(Connection con, int equipeId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT joueur_id FROM joueur_equipe WHERE equipe_id=?")) {
            ps.setInt(1, equipeId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) ids.add(rs.getInt(1)); }
        }
        return ids;
    }
}
