package my.insa.yong.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe utilitaire pour les classements de joueurs (buteurs)
 * @author saancern
 */
public class JoueurClassement {

    /**
     * Données de classement pour un buteur (tournoi actuel)
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
     * Données de classement pour un buteur (tous les tournois)
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
     * Charge le classement des buteurs pour le tournoi actuel
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
     * Charge le classement des buteurs pour tous les tournois
     */
    public static List<ButeurInfoAllTournois> chargerButeursAllTournois(Connection con) throws SQLException {
        List<ButeurInfoAllTournois> buteurs = new ArrayList<>();
        
        String sql = "SELECT j.id, j.prenom, j.nom, e.nom_equipe, t.nom_tournoi, COUNT(b.id) as nombreButs " +
                     "FROM joueur j " +
                     "LEFT JOIN but b ON j.id = b.joueur_id " +
                     "LEFT JOIN joueur_equipe je ON j.id = je.joueur_id " +
                     "LEFT JOIN equipe e ON je.equipe_id = e.id " +
                     "LEFT JOIN tournoi t ON j.tournoi_id = t.id " +
                     "GROUP BY j.id, j.prenom, j.nom, e.nom_equipe, t.nom_tournoi " +
                     "ORDER BY nombreButs DESC, j.nom ASC, j.prenom ASC";

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            try (ResultSet rs = pst.executeQuery()) {
                int rank = 0;
                Integer previousButs = null;

                while (rs.next()) {
                    int nombreButs = rs.getInt("nombreButs");
                    if (previousButs == null || nombreButs != previousButs) {
                        rank++;
                    }
                    String nomComplet = rs.getString("prenom") + " " + rs.getString("nom");
                    buteurs.add(new ButeurInfoAllTournois(rank, nomComplet, 
                        rs.getString("nom_equipe") != null ? rs.getString("nom_equipe") : "Sans équipe", 
                        nombreButs, 
                        rs.getString("nom_tournoi") != null ? rs.getString("nom_tournoi") : "N/A"));
                    previousButs = nombreButs;
                }
            }
        }

        return buteurs;
    }
}
