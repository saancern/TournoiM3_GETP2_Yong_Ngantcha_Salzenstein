package my.insa.yong.model;

/**
 * Représente un joueur dans le système
 * @author saancern
 */
public class Joueur {
    private int id;
    private String prenom;
    private String nom;
    private double taille;

    public Joueur() {
    }

    public Joueur(int id, String prenom, String nom, double taille) {
        this.id = id;
        this.prenom = prenom;
        this.nom = nom;
        this.taille = taille;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public double getTaille() {
        return taille;
    }

    public void setTaille(double taille) {
        this.taille = taille;
    }

    @Override
    public String toString() {
        return String.format("%s %s (%.1f cm)", prenom, nom, taille);
    }
}