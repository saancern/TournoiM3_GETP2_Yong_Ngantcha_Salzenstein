package my.insa.yong.model;

/**
 * Représente un joueur dans le système
 * @author saancern
 */
public class Joueur {
    private int id;
    private String prenom;
    private String nom;
    private int age;
    private String sexe;
    private double taille;

    public Joueur() {
    }

    public Joueur(int id, String prenom, String nom, double taille) {
        this.id = id;
        this.prenom = prenom;
        this.nom = nom;
        this.taille = taille;
    }

    public Joueur(int id, String prenom, String nom, int age, String sexe, double taille) {
        this.id = id;
        this.prenom = prenom;
        this.nom = nom;
        this.age = age;
        this.sexe = sexe;
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