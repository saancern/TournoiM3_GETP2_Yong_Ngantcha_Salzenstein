package my.insa.yong.model;

/**
 * Classe représentant un utilisateur du système
 * @author saancern
 */
public class Utilisateur {
    
    private int id;
    private String surnom;
    private String pass;
    private boolean isAdmin; // Privilèges administrateur
    
    public Utilisateur() {
    }
    
    public Utilisateur(String surnom, String pass, boolean isAdmin) {
        this.surnom = surnom;
        this.pass = pass;
        this.isAdmin = isAdmin;
    }
    
    public Utilisateur(int id, String surnom, String pass, boolean isAdmin) {
        this.id = id;
        this.surnom = surnom;
        this.pass = pass;
        this.isAdmin = isAdmin;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getSurnom() {
        return surnom;
    }
    
    public void setSurnom(String surnom) {
        this.surnom = surnom;
    }
    
    public String getPass() {
        return pass;
    }
    
    public void setPass(String pass) {
        this.pass = pass;
    }
    
    public boolean isAdmin() {
        return isAdmin;
    }
    
    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    
    @Override
    public String toString() {
        return "Utilisateur{" +
                "id=" + id +
                ", surnom='" + surnom + '\'' +
                ", isAdmin=" + isAdmin +
                '}';
    }
}
