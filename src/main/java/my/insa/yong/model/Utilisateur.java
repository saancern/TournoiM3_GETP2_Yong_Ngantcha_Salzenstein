package my.insa.yong.model;

/**
 * Classe représentant un utilisateur du système
 * @author saancern
 */
public class Utilisateur {
    
    private int id;
    private String surnom;
    private String pass;
    private int role; // Niveau d'accès administratif
    
    public Utilisateur() {
    }
    
    public Utilisateur(String surnom, String pass, int role) {
        this.surnom = surnom;
        this.pass = pass;
        this.role = role;
    }
    
    public Utilisateur(int id, String surnom, String pass, int role) {
        this.id = id;
        this.surnom = surnom;
        this.pass = pass;
        this.role = role;
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
    
    public int getRole() {
        return role;
    }
    
    public void setRole(int role) {
        this.role = role;
    }
    
    @Override
    public String toString() {
        return "Utilisateur{" +
                "id=" + id +
                ", surnom='" + surnom + '\'' +
                ", role=" + role +
                '}';
    }
}
