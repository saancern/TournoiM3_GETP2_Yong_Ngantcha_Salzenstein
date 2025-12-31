package my.insa.yong.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe pour calculer et gérer les statistiques d'un joueur
 * @author saancern
 */
public class StatistiquesJoueur {
    
    private int joueurId;
    private String prenomNom;
    private int nombreButs;
    private int matchsJoues;
    private int victoires;
    private int defaites;
    private int matchsNuls;
    private double efficacite; // Buts par match
    private int classementButeur; // Position dans le classement
    private String equipeActuelle;
    
    // Constructeur
    public StatistiquesJoueur(int joueurId) {
        this.joueurId = joueurId;
    }
    
    /**
     * Charge toutes les statistiques du joueur depuis la base
     */
    public void chargerStatistiques(Connection con, int tournoiId) throws SQLException {
        chargerInfosBase(con, tournoiId);
        chargerStatsButs(con, tournoiId);
        chargerStatsMatchs(con, tournoiId);
        calculerEfficacite();
        chargerClassement(con, tournoiId);
    }
    
    /**
     * Charge les informations de base du joueur
     */
    private void chargerInfosBase(Connection con, int tournoiId) throws SQLException {
        String sql = "SELECT CONCAT(prenom, ' ', nom) as nom_complet, " +
                    "(SELECT nom_equipe FROM equipe e " +
                    " JOIN joueur_equipe je ON e.id = je.equipe_id " +
                    " WHERE je.joueur_id = j.id AND e.tournoi_id = ? LIMIT 1) as equipe " +
                    "FROM joueur j WHERE j.id = ? AND j.tournoi_id = ?";
        
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            pst.setInt(2, joueurId);
            pst.setInt(3, tournoiId);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                this.prenomNom = rs.getString("nom_complet");
                this.equipeActuelle = rs.getString("equipe");
                if (this.equipeActuelle == null) {
                    this.equipeActuelle = "Sans équipe";
                }
            }
        }
    }
    
    /**
     * Charge les statistiques de buts
     */
    private void chargerStatsButs(Connection con, int tournoiId) throws SQLException {
        String sql = "SELECT COUNT(*) as total_buts FROM but WHERE joueur_id = ? AND tournoi_id = ?";
        
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, joueurId);
            pst.setInt(2, tournoiId);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                this.nombreButs = rs.getInt("total_buts");
            }
        }
    }
    
    /**
     * Charge les statistiques de matchs (joués, victoires, défaites, nuls)
     */
    private void chargerStatsMatchs(Connection con, int tournoiId) throws SQLException {
        String sql = """
            SELECT 
                COUNT(DISTINCT r.id) as matchs_joues,
                SUM(CASE 
                    WHEN r.winner_id = je.equipe_id THEN 1
                    ELSE 0
                END) as victoires,
                SUM(CASE 
                    WHEN r.played = true AND r.winner_id IS NULL THEN 1
                    ELSE 0
                END) as nuls,
                SUM(CASE 
                    WHEN r.played = true AND r.winner_id IS NOT NULL AND r.winner_id != je.equipe_id THEN 1
                    ELSE 0
                END) as defaites
            FROM rencontre r
            JOIN joueur_equipe je ON (je.equipe_id = r.equipe_a_id OR je.equipe_id = r.equipe_b_id)
            WHERE je.joueur_id = ? 
                AND r.tournoi_id = ? 
                AND je.tournoi_id = ?
                AND r.played = true
            """;
        
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, joueurId);
            pst.setInt(2, tournoiId);
            pst.setInt(3, tournoiId);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                this.matchsJoues = rs.getInt("matchs_joues");
                this.victoires = rs.getInt("victoires");
                this.matchsNuls = rs.getInt("nuls");
                this.defaites = rs.getInt("defaites");
            }
        }
    }
    
    /**
     * Calcule l'efficacité (buts par match)
     */
    private void calculerEfficacite() {
        if (matchsJoues > 0) {
            this.efficacite = (double) nombreButs / matchsJoues;
        } else {
            this.efficacite = 0.0;
        }
    }
    
    /**
     * Détermine le classement du joueur parmi les buteurs
     */
    private void chargerClassement(Connection con, int tournoiId) throws SQLException {
        String sql = """
            SELECT joueur_id, COUNT(*) as buts,
                   RANK() OVER (ORDER BY COUNT(*) DESC) as position
            FROM but
            WHERE tournoi_id = ?
            GROUP BY joueur_id
            """;
        
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                if (rs.getInt("joueur_id") == joueurId) {
                    this.classementButeur = rs.getInt("position");
                    break;
                }
            }
        }
    }
    
    /**
     * Récupère le prochain match du joueur (version statique)
     */
    public static MatchHistorique getProchainMatch(Connection con, int joueurId, int tournoiId) throws SQLException {
        String sql = """
            SELECT 
                r.round_number,
                CASE 
                    WHEN je.equipe_id = r.equipe_a_id THEN ea.nom_equipe
                    ELSE eb.nom_equipe
                END as mon_equipe,
                CASE 
                    WHEN je.equipe_id = r.equipe_a_id THEN eb.nom_equipe
                    ELSE ea.nom_equipe
                END as adversaire,
                COALESCE(r.score_a, 0) as score_a,
                COALESCE(r.score_b, 0) as score_b,
                COALESCE(t.nom_terrain, 'Non défini') as terrain
            FROM rencontre r
            JOIN joueur_equipe je ON (je.equipe_id = r.equipe_a_id OR je.equipe_id = r.equipe_b_id)
            LEFT JOIN equipe ea ON r.equipe_a_id = ea.id
            LEFT JOIN equipe eb ON r.equipe_b_id = eb.id
            LEFT JOIN terrain_rencontre tr ON r.id = tr.rencontre_id AND tr.tournoi_id = ?
            LEFT JOIN terrain t ON tr.terrain_id = t.id
            WHERE je.joueur_id = ? 
                AND je.tournoi_id = ?
                AND r.tournoi_id = ?
                AND r.played = false
            ORDER BY r.round_number ASC, r.id ASC
            LIMIT 1
            """;
        
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            pst.setInt(2, joueurId);
            pst.setInt(3, tournoiId);
            pst.setInt(4, tournoiId);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                int roundNumber = rs.getInt("round_number");
                String monEquipe = rs.getString("mon_equipe");
                String adversaire = rs.getString("adversaire");
                int scoreA = rs.getInt("score_a");
                int scoreB = rs.getInt("score_b");
                String terrain = rs.getString("terrain");
                
                String score = scoreA + " - " + scoreB;
                String matchInfo = monEquipe + " vs " + adversaire;
                
                return new MatchHistorique(
                    "Round " + roundNumber,
                    matchInfo,
                    "À venir",
                    score,
                    0,
                    terrain
                );
            }
        }
        
        // Aucun match à venir
        return new MatchHistorique(
            "Aucun match à venir",
            "-",
            "-",
            "-",
            0,
            "-"
        );
    }
    
    /**
     * Récupère le prochain match du joueur (ou le match en cours)
     */
    public MatchHistorique chargerProchainMatch(Connection con, int tournoiId) throws SQLException {
        String sql = """
            SELECT 
                r.round_number,
                CASE 
                    WHEN je.equipe_id = r.equipe_a_id THEN ea.nom_equipe
                    ELSE eb.nom_equipe
                END as mon_equipe,
                CASE 
                    WHEN je.equipe_id = r.equipe_a_id THEN eb.nom_equipe
                    ELSE ea.nom_equipe
                END as adversaire,
                COALESCE(r.score_a, 0) as score_a,
                COALESCE(r.score_b, 0) as score_b,
                CASE 
                    WHEN r.winner_id IS NULL THEN 'À venir'
                    WHEN r.winner_id = je.equipe_id THEN 'Victoire'
                    WHEN r.winner_id = 0 THEN 'Nul'
                    ELSE 'Défaite'
                END as resultat,
                (r.winner_id IS NOT NULL) as played,
                COALESCE(t.nom_terrain, 'Non défini') as terrain
            FROM rencontre r
            JOIN joueur_equipe je ON (je.equipe_id = r.equipe_a_id OR je.equipe_id = r.equipe_b_id)
            LEFT JOIN equipe ea ON r.equipe_a_id = ea.id
            LEFT JOIN equipe eb ON r.equipe_b_id = eb.id
            LEFT JOIN terrain_rencontre tr ON r.id = tr.rencontre_id AND tr.tournoi_id = ?
            LEFT JOIN terrain t ON tr.terrain_id = t.id
            WHERE je.joueur_id = ? 
                AND je.tournoi_id = ?
                AND r.tournoi_id = ?
                AND r.winner_id IS NULL
            ORDER BY r.round_number ASC
            LIMIT 1
            """;
        
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            pst.setInt(2, joueurId);
            pst.setInt(3, tournoiId);
            pst.setInt(4, tournoiId);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                int roundNumber = rs.getInt("round_number");
                String monEquipe = rs.getString("mon_equipe");
                String adversaire = rs.getString("adversaire");
                String resultat = rs.getString("resultat");
                int scoreA = rs.getInt("score_a");
                int scoreB = rs.getInt("score_b");
                String terrain = rs.getString("terrain");
                
                String score = scoreA + " - " + scoreB;
                String matchInfo = monEquipe + " vs " + adversaire;
                
                return new MatchHistorique(
                    "Round " + roundNumber,
                    matchInfo,
                    resultat,
                    score,
                    0,
                    terrain
                );
            }
        }
        
        return null; // Aucun match à venir
    }

    /**
     * Récupère l'historique des matchs du joueur
     */
    public List<MatchHistorique> chargerHistoriqueMatchs(Connection con, int tournoiId) throws SQLException {
        List<MatchHistorique> historique = new ArrayList<>();
        
        String sql = """
            SELECT DISTINCT
                r.id as match_id,
                r.round_number,
                r.score_a,
                r.score_b,
                r.played,
                CASE 
                    WHEN je.equipe_id = r.equipe_a_id THEN eb.nom_equipe
                    ELSE ea.nom_equipe
                END as adversaire,
                CASE 
                    WHEN je.equipe_id = r.equipe_a_id THEN ea.nom_equipe
                    ELSE eb.nom_equipe
                END as mon_equipe,
                CASE 
                    WHEN r.winner_id = je.equipe_id THEN 'Victoire'
                    WHEN r.played = true AND r.winner_id IS NULL THEN 'Nul'
                    WHEN r.played = true THEN 'Défaite'
                    ELSE 'À venir'
                END as resultat,
                (SELECT COUNT(*) FROM but b WHERE b.rencontre_id = r.id AND b.joueur_id = ?) as mes_buts
            FROM rencontre r
            JOIN equipe ea ON r.equipe_a_id = ea.id AND ea.tournoi_id = ?
            LEFT JOIN equipe eb ON r.equipe_b_id = eb.id AND eb.tournoi_id = ?
            JOIN joueur_equipe je ON (je.equipe_id = r.equipe_a_id OR je.equipe_id = r.equipe_b_id) 
                AND je.joueur_id = ? AND je.tournoi_id = ?
            WHERE r.tournoi_id = ?
            ORDER BY r.round_number DESC, r.id DESC
            LIMIT 10
            """;
        
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, joueurId);
            pst.setInt(2, tournoiId);
            pst.setInt(3, tournoiId);
            pst.setInt(4, joueurId);
            pst.setInt(5, tournoiId);
            pst.setInt(6, tournoiId);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                int roundNumber = rs.getInt("round_number");
                String adversaire = rs.getString("adversaire");
                String resultat = rs.getString("resultat");
                int scoreA = rs.getInt("score_a");
                int scoreB = rs.getInt("score_b");
                boolean played = rs.getBoolean("played");
                int mesButs = rs.getInt("mes_buts");
                String monEquipe = rs.getString("mon_equipe");
                
                String score = played ? scoreA + " - " + scoreB : "À venir";
                
                historique.add(new MatchHistorique(
                    "Round " + roundNumber,
                    monEquipe + " vs " + adversaire,
                    resultat,
                    score,
                    mesButs
                ));
            }
        }
        
        return historique;
    }
    
    // Getters
    public int getJoueurId() { return joueurId; }
    public String getPrenomNom() { return prenomNom; }
    public int getNombreButs() { return nombreButs; }
    public int getMatchsJoues() { return matchsJoues; }
    public int getVictoires() { return victoires; }
    public int getDefaites() { return defaites; }
    public int getMatchsNuls() { return matchsNuls; }
    public double getEfficacite() { return efficacite; }
    public String getEfficaciteFormatted() { return String.format("%.2f", efficacite); }
    public int getClassementButeur() { return classementButeur; }
    public String getClassementFormatted() { 
        if (classementButeur == 0) return "Non classé";
        return classementButeur + (classementButeur == 1 ? "er" : "ème");
    }
    public String getEquipeActuelle() { return equipeActuelle; }
    public double getTauxVictoire() {
        if (matchsJoues > 0) {
            return (double) victoires / matchsJoues * 100;
        }
        return 0.0;
    }
    public String getTauxVictoireFormatted() { return String.format("%.1f%%", getTauxVictoire()); }
    
    /**
     * Classe interne pour représenter un match dans l'historique
     */
    public static class MatchHistorique {
        private final String round;
        private final String match;
        private final String resultat;
        private final String score;
        private final int mesButs;
        private final String terrain;
        
        public MatchHistorique(String round, String match, String resultat, String score, int mesButs) {
            this(round, match, resultat, score, mesButs, "");
        }
        
        public MatchHistorique(String round, String match, String resultat, String score, int mesButs, String terrain) {
            this.round = round;
            this.match = match;
            this.resultat = resultat;
            this.score = score;
            this.mesButs = mesButs;
            this.terrain = terrain;
        }
        
        public String getRound() { return round; }
        public String getMatch() { return match; }
        public String getResultat() { return resultat; }
        public String getScore() { return score; }
        public int getMesButs() { return mesButs; }
        public String getTerrain() { return terrain; }
        public String getMesButsFormatted() { 
            return mesButs > 0 ? mesButs + " but" + (mesButs > 1 ? "s" : "") : "-";
        }
    }
}
