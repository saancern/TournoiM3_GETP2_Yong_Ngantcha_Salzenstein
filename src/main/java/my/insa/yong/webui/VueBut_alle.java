package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.components.BaseLayout;

@PageTitle("Meilleurs Buteurs")
@Route(value = "buteurs", layout = BaseLayout.class)
public class VueBut_alle extends VerticalLayout {

    private Grid<ButeurInfo> gridCurrentTournoi;
    private Grid<ButeurInfo> gridAllTournois;
    private VerticalLayout contentLayout;

    // Classe interne pour stocker les informations des buteurs
    public static class ButeurInfo {
        private int place;
        private String nomJoueur;
        private String nomEquipe;
        private int nombreButs;
        private String nomTournoi;

        public ButeurInfo(int place, String nomJoueur, String nomEquipe, int nombreButs) {
            this.place = place;
            this.nomJoueur = nomJoueur;
            this.nomEquipe = nomEquipe;
            this.nombreButs = nombreButs;
        }

        public ButeurInfo(int place, String nomJoueur, String nomEquipe, int nombreButs, String nomTournoi) {
            this.place = place;
            this.nomJoueur = nomJoueur;
            this.nomEquipe = nomEquipe;
            this.nombreButs = nombreButs;
            this.nomTournoi = nomTournoi;
        }

        public int getPlace() { return place; }
        public String getNomJoueur() { return nomJoueur; }
        public String getNomEquipe() { return nomEquipe; }
        public int getNombreButs() { return nombreButs; }
        public String getNomTournoi() { return nomTournoi; }

        public void setPlace(int place) { this.place = place; }
    }

    public VueBut_alle() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("app-container");

        // Titre
        H2 titre = new H2("🏆 Classement des Meilleurs Buteurs");
        titre.addClassName("page-title");
        add(titre);

        // Créer les onglets
        Tabs tabs = new Tabs();
        Tab tabCurrentTournoi = new Tab("Tournoi Actuel");
        Tab tabAllTournois = new Tab("Tous les Tournois");
        tabs.add(tabCurrentTournoi, tabAllTournois);
        tabs.setSelectedTab(tabCurrentTournoi);

        // Layout pour le contenu des onglets
        contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setSpacing(true);
        contentLayout.setPadding(false);

        // Créer les grilles
        gridCurrentTournoi = createGrid(false);
        gridAllTournois = createGrid(true);

        // Charger les données
        loadButeursCurrentTournoi();
        loadButeursAllTournois();

        // Afficher la première grille
        contentLayout.removeAll();
        contentLayout.add(gridCurrentTournoi);

        // Gérer les changements d'onglet
        tabs.addSelectedChangeListener(event -> {
            contentLayout.removeAll();
            if (event.getSelectedTab() == tabCurrentTournoi) {
                contentLayout.add(gridCurrentTournoi);
            } else {
                contentLayout.add(gridAllTournois);
            }
        });

        add(tabs, contentLayout);
    }

    private Grid<ButeurInfo> createGrid(boolean showTournoi) {
        Grid<ButeurInfo> grid = new Grid<>(ButeurInfo.class, false);
        grid.setSizeFull();
        grid.addColumn(ButeurInfo::getPlace)
                .setHeader("🥇 Place")
                .setWidth("10%")
                .setFlexGrow(0);

        grid.addColumn(ButeurInfo::getNomJoueur)
                .setHeader("👤 Joueur")
                .setWidth("35%");

        grid.addColumn(ButeurInfo::getNomEquipe)
                .setHeader("⚽ Équipe")
                .setWidth("30%");

        grid.addColumn(ButeurInfo::getNombreButs)
                .setHeader("⚽ Buts")
                .setWidth("15%")
                .setFlexGrow(0);

        if (showTournoi) {
            grid.addColumn(ButeurInfo::getNomTournoi)
                    .setHeader("🏆 Tournoi")
                    .setWidth("25%");
        }

        return grid;
    }

    private void loadButeursCurrentTournoi() {
        List<ButeurInfo> buteurs = new ArrayList<>();
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

        String sql = "SELECT j.id, j.nom_joueur, e.nom_equipe, COUNT(b.id) as nombreButs " +
                     "FROM joueur j " +
                     "LEFT JOIN but b ON j.id = b.joueur_id " +
                     "LEFT JOIN equipe e ON j.equipe_id = e.id " +
                     "WHERE j.tournoi_id = ? " +
                     "GROUP BY j.id, j.nom_joueur, e.nom_equipe " +
                     "ORDER BY nombreButs DESC, j.nom_joueur ASC " +
                     "LIMIT 100";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            ResultSet rs = pst.executeQuery();

            int place = 1;
            while (rs.next()) {
                buteurs.add(new ButeurInfo(
                        place++,
                        rs.getString("nom_joueur"),
                        rs.getString("nom_equipe") != null ? rs.getString("nom_equipe") : "N/A",
                        rs.getInt("nombreButs")
                ));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        gridCurrentTournoi.setItems(buteurs);
    }

    private void loadButeursAllTournois() {
        List<ButeurInfo> buteurs = new ArrayList<>();

        String sql = "SELECT j.nom_joueur, e.nom_equipe, t.nom_tournoi, COUNT(b.id) as nombreButs " +
                     "FROM joueur j " +
                     "LEFT JOIN but b ON j.id = b.joueur_id " +
                     "LEFT JOIN equipe e ON j.equipe_id = e.id " +
                     "LEFT JOIN tournoi t ON j.tournoi_id = t.id " +
                     "GROUP BY j.nom_joueur, e.nom_equipe, t.nom_tournoi " +
                     "ORDER BY nombreButs DESC, j.nom_joueur ASC " +
                     "LIMIT 100";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();

            int place = 1;
            while (rs.next()) {
                buteurs.add(new ButeurInfo(
                        place++,
                        rs.getString("nom_joueur"),
                        rs.getString("nom_equipe") != null ? rs.getString("nom_equipe") : "N/A",
                        rs.getInt("nombreButs"),
                        rs.getString("nom_tournoi") != null ? rs.getString("nom_tournoi") : "N/A"
                ));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        gridAllTournois.setItems(buteurs);
    }
}
