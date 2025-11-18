package my.insa.yong.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import my.insa.yong.utils.database.ClasseMiroir;

/**
 * hey
 * Représente un joueur dans le système
 * @author saancern
 */
public class Joueur extends ClasseMiroir {
    private String prenom;
    private String nom;
    private int age;
    private String sexe;
    private double taille;

    /**
     * Constructeur pour nouveau joueur (id = -1)
     */
    public Joueur() {
        super(); // id = -1
    }

    /**
     * Constructeur pour joueur existant en base (ancien format)
     */
    public Joueur(int id, String prenom, String nom, double taille) {
        super(id);
        this.prenom = prenom;
        this.nom = nom;
        this.taille = taille;
    }

    /**
     * Constructeur pour joueur existant en base (format complet)
     */
    public Joueur(int id, String prenom, String nom, int age, String sexe, double taille) {
        super(id);
        this.prenom = prenom;
        this.nom = nom;
        this.age = age;
        this.sexe = sexe;
        this.taille = taille;
    }

    /**
     * Constructeur pour nouveau joueur avec données
     */
    public Joueur(String prenom, String nom, int age, String sexe, double taille) {
        super(); // id = -1
        this.prenom = prenom;
        this.nom = nom;
        this.age = age;
        this.sexe = sexe;
        this.taille = taille;
    }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        // Use unified schema with tournoi_id column
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
        String sql = "INSERT INTO joueur (tournoi_id, prenom, nom, age, sexe, taille) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pst.setInt(1, tournoiId);
        pst.setString(2, this.prenom);
        pst.setString(3, this.nom);
        pst.setInt(4, this.age);
        pst.setString(5, this.sexe);
        pst.setDouble(6, this.taille);
        pst.executeUpdate();
        return pst;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getSexe() {
        return sexe;
    }

    public void setSexe(String sexe) {
        this.sexe = sexe;
    }

    public double getTaille() {
        return taille;
    }

    public void setTaille(double taille) {
        this.taille = taille;
    }

    @Override
    public String toString() {
        return String.format("%s %s (%d ans, %s, %.1f cm)", prenom, nom, age, sexe, taille);
    }
}