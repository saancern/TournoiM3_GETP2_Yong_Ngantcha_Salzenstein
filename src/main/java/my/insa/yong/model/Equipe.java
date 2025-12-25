package my.insa.yong.model;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import my.insa.yong.utils.database.ClasseMiroir;

/**
 * Représente une équipe dans le système
 * @author saancern
 */
public class Equipe extends ClasseMiroir {
    private String nomEquipe;
    private LocalDate dateCreation;
    private List<Joueur> joueurs;

    /**
     * Constructeur pour nouvelle équipe (id = -1)
     */
    public Equipe() {
        super(); // id = -1
    }

    /**
     * Constructeur pour équipe existante en base
     */
    public Equipe(int id, String nomEquipe, LocalDate dateCreation) {
        super(id);
        this.nomEquipe = nomEquipe;
        this.dateCreation = dateCreation;
    }

    /**
     * Constructeur pour nouvelle équipe avec données
     */
    public Equipe(String nomEquipe, LocalDate dateCreation) {
        super(); // id = -1
        this.nomEquipe = nomEquipe;
        this.dateCreation = dateCreation;
    }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        // Use unified schema with tournoi_id column
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
        String sql = "INSERT INTO equipe (tournoi_id, nom_equipe, date_creation) VALUES (?, ?, ?)";
        PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pst.setInt(1, tournoiId);
        pst.setString(2, this.nomEquipe);
        pst.setDate(3, Date.valueOf(this.dateCreation));
        pst.executeUpdate();
        return pst;
    }

    public String getNomEquipe() {
        return nomEquipe;
    }

    public void setNomEquipe(String nomEquipe) {
        this.nomEquipe = nomEquipe;
    }

    public LocalDate getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDate dateCreation) {
        this.dateCreation = dateCreation;
    }

    public List<Joueur> getJoueurs() {
        return joueurs;
    }

    public void setJoueurs(List<Joueur> joueurs) {
        this.joueurs = joueurs;
    }

    @Override
    public String toString() {
        return String.format("%s (créée le %s)", nomEquipe, dateCreation);
    }

    /**
     * Modifier une équipe existante dans la base de données
     */
    public static void modifierEquipe(Connection con, Equipe equipe, int tournoiId) throws SQLException {
        String sql = "UPDATE equipe SET nom_equipe = ?, date_creation = ? WHERE id = ? AND tournoi_id = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, equipe.getNomEquipe());
            pst.setDate(2, Date.valueOf(equipe.getDateCreation()));
            pst.setInt(3, equipe.getId());
            pst.setInt(4, tournoiId);
            pst.executeUpdate();
        }
    }

    /**
     * Supprimer une équipe de la base de données
     */
    public static void supprimerEquipe(Connection con, int equipeId, int tournoiId) throws SQLException {
        String sql = "DELETE FROM equipe WHERE id = ? AND tournoi_id = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, equipeId);
            pst.setInt(2, tournoiId);
            pst.executeUpdate();
        }
    }

    /**
     * Charger toutes les équipes pour un tournoi donné
     */
    public static List<Equipe> chargerEquipesPourTournoi(Connection con, int tournoiId) throws SQLException {
        List<Equipe> equipes = new ArrayList<>();
        String sql = "SELECT id, nom_equipe, date_creation FROM equipe WHERE tournoi_id = ? ORDER BY nom_equipe, date_creation";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    equipes.add(new Equipe(
                            rs.getInt("id"),
                            rs.getString("nom_equipe"),
                            rs.getDate("date_creation").toLocalDate()
                    ));
                }
            }
        }
        return equipes;
    }

    /**
     * Compter le nombre d'équipes pour un tournoi
     */
    public static int compterEquipesPourTournoi(Connection con, int tournoiId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM equipe WHERE tournoi_id = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Vérifier si un joueur est déjà assigné à une autre équipe
     */
    public static boolean joueurDansAutreEquipe(Connection con, int joueurId, int tournoiId, int equipeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM joueur_equipe WHERE joueur_id = ? AND tournoi_id = ? AND equipe_id != ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, joueurId);
            pst.setInt(2, tournoiId);
            pst.setInt(3, equipeId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Ajouter des joueurs à une équipe
     */
    public static void ajouterJoueursAEquipe(Connection con, int equipeId, List<Joueur> joueurs, int tournoiId) throws SQLException {
        if (joueurs.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO joueur_equipe (joueur_id, equipe_id, tournoi_id) VALUES (?, ?, ?)";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            for (Joueur joueur : joueurs) {
                pst.setInt(1, joueur.getId());
                pst.setInt(2, equipeId);
                pst.setInt(3, tournoiId);
                pst.addBatch();
            }
            pst.executeBatch();
        }
    }

    /**
     * Supprimer tous les joueurs d'une équipe
     */
    public static void supprimerJoueursEquipe(Connection con, int equipeId, int tournoiId) throws SQLException {
        String sql = "DELETE FROM joueur_equipe WHERE equipe_id = ? AND tournoi_id = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, equipeId);
            pst.setInt(2, tournoiId);
            pst.executeUpdate();
        }
    }

    /**
     * Charger les joueurs d'une équipe
     */
    public static List<Joueur> chargerJoueursEquipe(Connection con, int equipeId, int tournoiId) throws SQLException {
        List<Joueur> joueurs = new ArrayList<>();
        String sql = "SELECT j.id, j.prenom, j.nom, j.age, j.sexe, j.taille " +
                     "FROM joueur j " +
                     "INNER JOIN joueur_equipe je ON j.id = je.joueur_id " +
                     "WHERE je.equipe_id = ? AND j.tournoi_id = ? AND je.tournoi_id = ? " +
                     "ORDER BY j.nom, j.prenom";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, equipeId);
            pst.setInt(2, tournoiId);
            pst.setInt(3, tournoiId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    joueurs.add(new Joueur(
                            rs.getInt("id"),
                            rs.getString("prenom"),
                            rs.getString("nom"),
                            rs.getInt("age"),
                            rs.getString("sexe"),
                            rs.getDouble("taille")
                    ));
                }
            }
        }
        return joueurs;
    }
}