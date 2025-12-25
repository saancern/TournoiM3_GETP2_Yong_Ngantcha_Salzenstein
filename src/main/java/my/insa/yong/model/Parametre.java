package my.insa.yong.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import my.insa.yong.utils.database.ClasseMiroir;

/**
 * Classe représentant les paramètres d'un tournoi
 * 
 * @author saancern
 */
public class Parametre extends ClasseMiroir {
    
    private String nomTournoi;
    private String sport;
    private int nombreTerrains;
    private int nombreJoueursParEquipe;
    
    /**
     * Constructeur par défaut (pour création d'un nouveau paramètre)
     */
    public Parametre() {
        super();
        this.nomTournoi = "Nouveau Tournoi";
        this.sport = "Football";
        this.nombreTerrains = 1;
        this.nombreJoueursParEquipe = 5;
    }
    
    /**
     * Constructeur avec paramètres (pour création d'un nouveau paramètre)
     */
    public Parametre(String nomTournoi, String sport, int nombreTerrains, int nombreJoueursParEquipe) {
        super();
        this.nomTournoi = nomTournoi;
        this.sport = sport;
        this.nombreTerrains = nombreTerrains;
        this.nombreJoueursParEquipe = nombreJoueursParEquipe;
    }
    
    /**
     * Constructeur complet (pour chargement depuis la base de données)
     */
    public Parametre(int id, String nomTournoi, String sport, int nombreTerrains, int nombreJoueursParEquipe) {
        super(id);
        this.nomTournoi = nomTournoi;
        this.sport = sport;
        this.nombreTerrains = nombreTerrains;
        this.nombreJoueursParEquipe = nombreJoueursParEquipe;
    }
    
    // Getters et Setters
    public String getNomTournoi() {
        return nomTournoi;
    }
    
    public void setNomTournoi(String nomTournoi) {
        this.nomTournoi = nomTournoi;
    }
    
    public String getSport() {
        return sport;
    }
    
    public void setSport(String sport) {
        this.sport = sport;
    }
    
    public int getNombreTerrains() {
        return nombreTerrains;
    }
    
    public void setNombreTerrains(int nombreTerrains) {
        this.nombreTerrains = nombreTerrains;
    }
    
    public int getNombreJoueursParEquipe() {
        return nombreJoueursParEquipe;
    }
    
    public void setNombreJoueursParEquipe(int nombreJoueursParEquipe) {
        this.nombreJoueursParEquipe = nombreJoueursParEquipe;
    }
    
    /**
     * Sauvegarde ou met à jour le paramètre dans la base de données
     * Cette méthode override celle de ClasseMiroir pour permettre la mise à jour
     */
    public void sauvegarderOuModifier(Connection con) throws SQLException {
        if (this.getId() == -1) {
            // Nouveau paramètre - utiliser saveInDB de ClasseMiroir
            this.saveInDB(con);
        } else {
            // Paramètre existant - UPDATE
            String sql = "UPDATE tournoi SET nom_tournoi = ?, sport = ?, nombre_terrains = ?, nombre_joueurs_par_equipe = ? WHERE id = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, this.nomTournoi);
                pst.setString(2, this.sport);
                pst.setInt(3, this.nombreTerrains);
                pst.setInt(4, this.nombreJoueursParEquipe);
                pst.setInt(5, this.getId());
                
                pst.executeUpdate();
            }
        }
    }
    
    /**
     * Implémentation de la méthode abstraite de ClasseMiroir
     * Sauvegarde tous les attributs sauf l'ID
     */
    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        String sql = "INSERT INTO tournoi (nom_tournoi, sport, nombre_terrains, nombre_joueurs_par_equipe) VALUES (?, ?, ?, ?)";
        PreparedStatement pst = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
        pst.setString(1, this.nomTournoi);
        pst.setString(2, this.sport);
        pst.setInt(3, this.nombreTerrains);
        pst.setInt(4, this.nombreJoueursParEquipe);
        
        pst.executeUpdate();
        return pst;
    }
    
    /**
     * Charge un paramètre depuis la base de données par son ID
     */
    public static Parametre getParametreById(Connection con, int id) throws SQLException {
        String sql = "SELECT id, nom_tournoi, sport, nombre_terrains, nombre_joueurs_par_equipe FROM tournoi WHERE id = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new Parametre(
                        rs.getInt("id"),
                        rs.getString("nom_tournoi"),
                        rs.getString("sport"),
                        rs.getInt("nombre_terrains"),
                        rs.getInt("nombre_joueurs_par_equipe")
                    );
                }
            }
        }
        return null;
    }
    
    /**
     * Récupère un paramètre par défaut (premier enregistrement)
     */
    public static Parametre getParametreParDefaut(Connection con) throws SQLException {
        String sql = "SELECT id, nom_tournoi, sport, nombre_terrains, nombre_joueurs_par_equipe FROM tournoi LIMIT 1";
        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            
            if (rs.next()) {
                return new Parametre(
                    rs.getInt("id"),
                    rs.getString("nom_tournoi"),
                    rs.getString("sport"),
                    rs.getInt("nombre_terrains"),
                    rs.getInt("nombre_joueurs_par_equipe")
                );
            }
        }
        return null;
    }
    
    
    /**
     * Récupère tous les paramètres (tournois)
     */
    public static List<Parametre> tousLesParametres(Connection con) throws SQLException {
        List<Parametre> parametres = new ArrayList<>();
        String sql = "SELECT id, nom_tournoi, sport, nombre_terrains, nombre_joueurs_par_equipe FROM tournoi ORDER BY nom_tournoi";
        
        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            
            while (rs.next()) {
                parametres.add(new Parametre(
                    rs.getInt("id"),
                    rs.getString("nom_tournoi"),
                    rs.getString("sport"),
                    rs.getInt("nombre_terrains"),
                    rs.getInt("nombre_joueurs_par_equipe")
                ));
            }
        }
        return parametres;
    }
    
    @Override
    public String toString() {
        return String.format("Parametre{id=%d, nomTournoi='%s', sport='%s', nombreTerrains=%d, nombreJoueursParEquipe=%d}", 
                           getId(), nomTournoi, sport, nombreTerrains, nombreJoueursParEquipe);
    }
}