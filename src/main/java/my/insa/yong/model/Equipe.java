package my.insa.yong.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Représente une équipe dans le système
 * @author saancern
 */
public class Equipe {
    private int id;
    private String nomEquipe;
    private LocalDate dateCreation;
    private List<Joueur> joueurs;

    public Equipe() {
    }

    public Equipe(int id, String nomEquipe, LocalDate dateCreation) {
        this.id = id;
        this.nomEquipe = nomEquipe;
        this.dateCreation = dateCreation;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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