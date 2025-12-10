package my.insa.yong.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import my.insa.yong.utils.database.ClasseMiroir;

/**
 * Représente un terrain dans le système
 * @author saancern
 */
public class Terrain extends ClasseMiroir {
    private String nomTerrain;
    private int numero;

    /**
     * Constructeur pour nouveau terrain (id = -1)
     */
    public Terrain() {
        super(); // id = -1
    }

    /**
     * Constructeur pour terrain existant en base
     */
    public Terrain(int id, String nomTerrain, int numero) {
        super(id);
        this.nomTerrain = nomTerrain;
        this.numero = numero;
    }

    /**
     * Constructeur pour nouveau terrain avec données
     */
    public Terrain(String nomTerrain, int numero) {
        super(); // id = -1
        this.nomTerrain = nomTerrain;
        this.numero = numero;
    }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        // Use unified schema with tournoi_id column
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
        String sql = "INSERT INTO terrain (tournoi_id, nom_terrain, numero) VALUES (?, ?, ?)";
        PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pst.setInt(1, tournoiId);
        pst.setString(2, this.nomTerrain);
        pst.setInt(3, this.numero);
        pst.executeUpdate();
        return pst;
    }

    public String getNomTerrain() {
        return nomTerrain;
    }

    public void setNomTerrain(String nomTerrain) {
        this.nomTerrain = nomTerrain;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    @Override
    public String toString() {
        return String.format("Terrain %d - %s", numero, nomTerrain);
    }
}
