package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
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
    private Paragraph noDataMessage;

    // Classe interne pour stocker les informations des buteurs
    public static class ButeurInfo {
        private int place;
        private String nomJoueur;
        private String nomEquipe;
        private int nombreButs;
        private String nomTournoi;
        private int matchsJoues;
        private double moyenne;

        public ButeurInfo(int place, String nomJoueur, String nomEquipe, int nombreButs) {
            this.place = place;
            this.nomJoueur = nomJoueur;
            this.nomEquipe = nomEquipe;
            this.nombreButs = nombreButs;
            this.matchsJoues = 0;
            this.moyenne = 0.0;
        }

        public ButeurInfo(int place, String nomJoueur, String nomEquipe, int nombreButs, int matchsJoues) {
            this.place = place;
            this.nomJoueur = nomJoueur;
            this.nomEquipe = nomEquipe;
            this.nombreButs = nombreButs;
            this.matchsJoues = matchsJoues;
            this.moyenne = matchsJoues > 0 ? (double) nombreButs / matchsJoues : 0.0;
        }

        public ButeurInfo(int place, String nomJoueur, String nomEquipe, int nombreButs, String nomTournoi) {
            this.place = place;
            this.nomJoueur = nomJoueur;
            this.nomEquipe = nomEquipe;
            this.nombreButs = nombreButs;
            this.nomTournoi = nomTournoi;
            this.matchsJoues = 0;
            this.moyenne = 0.0;
        }

        public int getPlace() { return place; }
        public String getPlaceFormatted() {
            switch (place) {
                case 1: return "🥇 1er";
                case 2: return "🥈 2ème";
                case 3: return "🥉 3ème";
                default: return String.valueOf(place);
            }
        }
        public String getNomJoueur() { return nomJoueur; }
        public String getNomEquipe() { return nomEquipe; }
        public int getNombreButs() { return nombreButs; }
        public String getNombreButsFormatted() { return "⚽ " + nombreButs; }
        public String getNomTournoi() { return nomTournoi; }
        public int getMatchsJoues() { return matchsJoues; }
        public String getMoyenneFormatted() { return String.format("%.2f", moyenne); }

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

        // Message pour indiquer qu'il n'y a pas de données
        noDataMessage = new Paragraph("⚽ Aucun but marqué pour le moment. Les statistiques s'afficheront dès qu'un but sera enregistré.");
        noDataMessage.getStyle()
            .set("text-align", "center")
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "1.1em")
            .set("padding", "2em");
        noDataMessage.setVisible(false);

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

        // Afficher la première grille ou le message
        updateContentDisplay(gridCurrentTournoi);

        // Gérer les changements d'onglet
        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab() == tabCurrentTournoi) {
                updateContentDisplay(gridCurrentTournoi);
            } else {
                updateContentDisplay(gridAllTournois);
            }
        });

        add(tabs, contentLayout);
    }

    private void updateContentDisplay(Grid<ButeurInfo> grid) {
        contentLayout.removeAll();
        if (grid.getListDataView().getItemCount() == 0) {
            noDataMessage.setVisible(true);
            contentLayout.add(noDataMessage);
        } else {
            noDataMessage.setVisible(false);
            contentLayout.add(grid);
        }
    }

    private Grid<ButeurInfo> createGrid(boolean showTournoi) {
        Grid<ButeurInfo> grid = new Grid<>(ButeurInfo.class, false);
        grid.setSizeFull();
        grid.setPageSize(20);
        grid.setColumnReorderingAllowed(true);
        
        // Colonne place avec style
        grid.addColumn(ButeurInfo::getPlaceFormatted)
                .setHeader("Place")
                .setWidth("100px")
                .setFlexGrow(0)
                .setFrozen(true)
                .setSortable(true)
                .setResizable(true);

        // Colonne joueur
        grid.addColumn(ButeurInfo::getNomJoueur)
                .setHeader("Joueur")
                .setAutoWidth(true)
                .setSortable(true)
                .setResizable(true);

        // Colonne équipe
        grid.addColumn(ButeurInfo::getNomEquipe)
                .setHeader("Équipe")
                .setAutoWidth(true)
                .setSortable(true)
                .setResizable(true);

        // Colonne nombre de buts avec mise en forme
        grid.addColumn(ButeurInfo::getNombreButs)
                .setHeader("Buts")
                .setWidth("100px")
                .setFlexGrow(0)
                .setSortable(true)
                .setComparator((b1, b2) -> Integer.compare(b2.getNombreButs(), b1.getNombreButs()));

        if (showTournoi) {
            grid.addColumn(ButeurInfo::getNomTournoi)
                    .setHeader("Tournoi")
                    .setAutoWidth(true)
                    .setSortable(true)
                    .setResizable(true);
        }

        // Appliquer le style au podium pour toute la ligne
        grid.setClassNameGenerator(buteur -> {
            if (buteur.getPlace() == 1) return "gold-row";
            if (buteur.getPlace() == 2) return "silver-row";
            if (buteur.getPlace() == 3) return "bronze-row";
            return "";
        });

        // Ajouter des styles
        grid.addClassName("buteurs-grid");
        grid.getStyle()
            .set("border-radius", "8px")
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");

        return grid;
    }

    private void loadButeursCurrentTournoi() {
        List<ButeurInfo> buteurs = new ArrayList<>();
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

        String sql = "SELECT j.id, j.prenom, j.nom, e.nom_equipe, COUNT(b.id) as nombreButs " +
                     "FROM joueur j " +
                     "LEFT JOIN but b ON j.id = b.joueur_id AND b.tournoi_id = ? " +
                     "LEFT JOIN joueur_equipe je ON j.id = je.joueur_id AND je.tournoi_id = ? " +
                     "LEFT JOIN equipe e ON je.equipe_id = e.id " +
                     "WHERE j.tournoi_id = ? " +
                     "GROUP BY j.id, j.prenom, j.nom, e.nom_equipe " +
                     "ORDER BY nombreButs DESC, j.nom ASC, j.prenom ASC " +
                     "LIMIT 10";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            pst.setInt(2, tournoiId);
            pst.setInt(3, tournoiId);
            ResultSet rs = pst.executeQuery();

            int place = 1;
            int previousButs = -1;
            int displayPlace = 1;
            while (rs.next()) {
                int nombreButs = rs.getInt("nombreButs");
                // Gérer les ex-aequo
                if (nombreButs != previousButs) {
                    displayPlace = place;
                }
                
                String nomComplet = rs.getString("prenom") + " " + rs.getString("nom");
                buteurs.add(new ButeurInfo(
                        displayPlace,
                        nomComplet,
                        rs.getString("nom_equipe") != null ? rs.getString("nom_equipe") : "Sans équipe",
                        nombreButs
                ));
                previousButs = nombreButs;
                place++;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        gridCurrentTournoi.setItems(buteurs);
    }

    private void loadButeursAllTournois() {
        List<ButeurInfo> buteurs = new ArrayList<>();

        String sql = "SELECT j.prenom, j.nom, e.nom_equipe, t.nom_tournoi, COUNT(b.id) as nombreButs " +
                     "FROM joueur j " +
                     "LEFT JOIN but b ON j.id = b.joueur_id " +
                     "LEFT JOIN joueur_equipe je ON j.id = je.joueur_id " +
                     "LEFT JOIN equipe e ON je.equipe_id = e.id " +
                     "LEFT JOIN tournoi t ON j.tournoi_id = t.id " +
                     "GROUP BY j.id, j.prenom, j.nom, e.nom_equipe, t.nom_tournoi " +
                     "ORDER BY nombreButs DESC, j.nom ASC, j.prenom ASC " +
                     "LIMIT 100";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();

            int place = 1;
            int previousButs = -1;
            int displayPlace = 1;
            while (rs.next()) {
                int nombreButs = rs.getInt("nombreButs");
                // Gérer les ex-aequo
                if (nombreButs != previousButs) {
                    displayPlace = place;
                }
                
                String nomComplet = rs.getString("prenom") + " " + rs.getString("nom");
                buteurs.add(new ButeurInfo(
                        displayPlace,
                        nomComplet,
                        rs.getString("nom_equipe") != null ? rs.getString("nom_equipe") : "Sans équipe",
                        nombreButs,
                        rs.getString("nom_tournoi") != null ? rs.getString("nom_tournoi") : "N/A"
                ));
                previousButs = nombreButs;
                place++;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        gridAllTournois.setItems(buteurs);
    }
}
