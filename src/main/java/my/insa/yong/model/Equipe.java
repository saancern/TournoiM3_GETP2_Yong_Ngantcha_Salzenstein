package my.insa.yong.model;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import my.insa.yong.utils.database.ClasseMiroir;

/**
 * allo
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
        // Use tournament-specific table names
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
        String equipeTable = tournoiId == 1 ? "equipe" : "equipe_" + tournoiId;
        String sql = "INSERT INTO " + equipeTable + " (nom_equipe, date_creation) VALUES (?, ?)";
        PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, this.nomEquipe);
        pst.setDate(2, Date.valueOf(this.dateCreation));
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
}