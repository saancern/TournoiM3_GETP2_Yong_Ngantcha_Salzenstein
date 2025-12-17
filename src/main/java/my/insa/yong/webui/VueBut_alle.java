package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

    // Keep this only for table mode if you want; leaderboard uses a fresh message now
    private Paragraph noDataMessage;

    private VerticalLayout leaderboardCurrentView;
    private VerticalLayout leaderboardAllView;

    private Button btnViewAllCurrent;
    private Button btnBackCurrent;
    private Button btnViewAllAll;
    private Button btnBackAll;

    private List<ButeurInfo> currentButeursCache = new ArrayList<>();
    private List<ButeurInfo> allButeursCache = new ArrayList<>();

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

        public ButeurInfo(int matchsJoues, double moyenne, String nomEquipe, String nomJoueur, String nomTournoi, int nombreButs, int place) {
            this.matchsJoues = matchsJoues;
            this.moyenne = moyenne;
            this.nomEquipe = nomEquipe;
            this.nomJoueur = nomJoueur;
            this.nomTournoi = nomTournoi;
            this.nombreButs = nombreButs;
            this.place = place;
        }

        public int getPlace() { return place; }

        public String getPlaceFormatted() {
            return switch (place) {
                case 1 -> "🥇 1er";
                case 2 -> "🥈 2ème";
                case 3 -> "🥉 3ème";
                default -> String.valueOf(place);
            };
        }

        public String getNomJoueur() { return nomJoueur; }
        public String getNomEquipe() { return nomEquipe; }
        public int getNombreButs() { return nombreButs; }
        public String getNombreButsFormatted() { return "⚽ " + nombreButs; }
        public String getNomTournoi() { return nomTournoi; }
        public int getMatchsJoues() { return matchsJoues; }
        public String getMoyenneFormatted() { return String.format("%.2f", moyenne); }

        public void setPlace(int place) { this.place = place; }
        public double getMoyenne() { return moyenne; }
        public void setMoyenne(double moyenne) { this.moyenne = moyenne; }
        public void setMatchsJoues(int matchsJoues) { this.matchsJoues = matchsJoues; }
        public void setNombreButs(int nombreButs) { this.nombreButs = nombreButs; }
        public void setNomEquipe(String nomEquipe) { this.nomEquipe = nomEquipe; }
        public void setNomJoueur(String nomJoueur) { this.nomJoueur = nomJoueur; }
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

        // Message table-only (leaderboard uses a new instance to avoid visibility/parent issues)
        noDataMessage = new Paragraph("⚽ Aucun but marqué pour le moment. Les statistiques s'afficheront dès qu'un but sera enregistré.");
        noDataMessage.addClassName("no-data-message");
        noDataMessage.setVisible(false);

        // Créer les onglets
        Tabs tabs = new Tabs();
        Tab tabCurrentTournoi = new Tab("Tournoi Actuel");
        Tab tabAllTournois = new Tab("Tous les Tournois");
        tabs.add(tabCurrentTournoi, tabAllTournois);
        tabs.setSelectedTab(tabCurrentTournoi);

        // Setup buttons
        btnViewAllCurrent = new Button("Voir tout");
        btnBackCurrent = new Button("Retour");
        btnBackCurrent.setVisible(false);
        btnViewAllCurrent.addClickListener(e -> showTableCurrent());
        btnBackCurrent.addClickListener(e -> showLeaderboardCurrent());

        btnViewAllAll = new Button("Voir tout");
        btnBackAll = new Button("Retour");
        btnBackAll.setVisible(false);
        btnViewAllAll.addClickListener(e -> showTableAll());
        btnBackAll.addClickListener(e -> showLeaderboardAll());

        // Tab header with buttons
        HorizontalLayout tabHeader = new HorizontalLayout();
        tabHeader.setWidthFull();
        tabHeader.setAlignItems(Alignment.CENTER);
        tabHeader.add(tabs);
        tabHeader.expand(tabs);
        tabHeader.add(btnViewAllCurrent, btnBackCurrent, btnViewAllAll, btnBackAll);

        // Layout pour le contenu des onglets
        contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setSpacing(true);
        contentLayout.setPadding(false);

        // Créer les grilles
        gridCurrentTournoi = createGrid(false);
        gridAllTournois = createGrid(true);

        // Initialize leaderboard views
        leaderboardCurrentView = new VerticalLayout();
        leaderboardCurrentView.setWidthFull();
        leaderboardCurrentView.setPadding(false);

        leaderboardAllView = new VerticalLayout();
        leaderboardAllView.setWidthFull();
        leaderboardAllView.setPadding(false);

        // Charger les données
        loadButeursCurrentTournoi();
        loadButeursAllTournois();

        // Show leaderboard by default
        showLeaderboardCurrent();

        // Gérer les changements d'onglet
        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab() == tabCurrentTournoi) {
                showLeaderboardCurrent();
            } else {
                showLeaderboardAll();
            }
        });

        add(tabHeader, contentLayout);

        // IMPORTANT: ensure content area actually gets space
        expand(contentLayout);
    }

    @SuppressWarnings("deprecation")
    private Grid<ButeurInfo> createGrid(boolean showTournoi) {
        Grid<ButeurInfo> grid = new Grid<>(ButeurInfo.class, false);
        grid.setSizeFull();
        grid.setPageSize(20);
        grid.setColumnReorderingAllowed(true);

        grid.addColumn(ButeurInfo::getPlaceFormatted)
                .setHeader("Place")
                .setWidth("100px")
                .setFlexGrow(0)
                .setFrozen(true)
                .setSortable(true)
                .setResizable(true);

        grid.addColumn(ButeurInfo::getNomJoueur)
                .setHeader("Joueur")
                .setAutoWidth(true)
                .setSortable(true)
                .setResizable(true);

        grid.addColumn(ButeurInfo::getNomEquipe)
                .setHeader("Équipe")
                .setAutoWidth(true)
                .setSortable(true)
                .setResizable(true);

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

        grid.setClassNameGenerator(buteur -> {
            if (buteur.getPlace() == 1) return "gold-row";
            if (buteur.getPlace() == 2) return "silver-row";
            if (buteur.getPlace() == 3) return "bronze-row";
            return "";
        });

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
                     "ORDER BY nombreButs DESC, j.nom ASC, j.prenom ASC";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, tournoiId);
            pst.setInt(2, tournoiId);
            pst.setInt(3, tournoiId);

            ResultSet rs = pst.executeQuery();

            // Dense ranking: 1,1,2,3... (no gaps)
            int rank = 0;
            Integer previousButs = null;

            while (rs.next()) {
                int nombreButs = rs.getInt("nombreButs");

                if (previousButs == null || nombreButs != previousButs) {
                    rank++;
                }

                String nomComplet = rs.getString("prenom") + " " + rs.getString("nom");

                buteurs.add(new ButeurInfo(
                        rank,
                        nomComplet,
                        rs.getString("nom_equipe") != null ? rs.getString("nom_equipe") : "Sans équipe",
                        nombreButs
                ));

                previousButs = nombreButs;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        currentButeursCache = buteurs;
        gridCurrentTournoi.setItems(buteurs);
        updateLeaderboardCurrent();
    }

    private void loadButeursAllTournois() {
        List<ButeurInfo> buteurs = new ArrayList<>();

        String sql = "SELECT j.id, j.prenom, j.nom, e.nom_equipe, t.nom_tournoi, COUNT(b.id) as nombreButs " +
                     "FROM joueur j " +
                     "LEFT JOIN but b ON j.id = b.joueur_id " +
                     "LEFT JOIN joueur_equipe je ON j.id = je.joueur_id " +
                     "LEFT JOIN equipe e ON je.equipe_id = e.id " +
                     "LEFT JOIN tournoi t ON j.tournoi_id = t.id " +
                     "GROUP BY j.id, j.prenom, j.nom, e.nom_equipe, t.nom_tournoi " +
                     "ORDER BY nombreButs DESC, j.nom ASC, j.prenom ASC";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            ResultSet rs = pst.executeQuery();

            // Dense ranking: 1,1,2,3... (no gaps)
            int rank = 0;
            Integer previousButs = null;

            while (rs.next()) {
                int nombreButs = rs.getInt("nombreButs");

                if (previousButs == null || nombreButs != previousButs) {
                    rank++;
                }

                String nomComplet = rs.getString("prenom") + " " + rs.getString("nom");

                buteurs.add(new ButeurInfo(
                        rank,
                        nomComplet,
                        rs.getString("nom_equipe") != null ? rs.getString("nom_equipe") : "Sans équipe",
                        nombreButs,
                        rs.getString("nom_tournoi") != null ? rs.getString("nom_tournoi") : "N/A"
                ));

                previousButs = nombreButs;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        allButeursCache = buteurs;
        gridAllTournois.setItems(buteurs);
        updateLeaderboardAll();
    }

    private Paragraph buildNoDataMessage() {
        Paragraph p = new Paragraph("⚽ Aucun but marqué pour le moment. Les statistiques s'afficheront dès qu'un but sera enregistré.");
        p.addClassName("no-data-message");
        return p;
    }


    private HorizontalLayout buildPodium(List<ButeurInfo> buteurs, boolean showTournoi) {
        List<ButeurInfo> first = new ArrayList<>();
        List<ButeurInfo> second = new ArrayList<>();
        List<ButeurInfo> third = new ArrayList<>();

        for (ButeurInfo b : buteurs) {
            switch (b.getPlace()) {
                case 1 -> first.add(b);
                case 2 -> second.add(b);
                case 3 -> third.add(b);
                default -> { /* ignore */ }
            }
        }

        HorizontalLayout podium = new HorizontalLayout();
        podium.setWidthFull();
        podium.setJustifyContentMode(JustifyContentMode.CENTER);
        podium.setAlignItems(Alignment.END);
        podium.setSpacing(true);
        podium.addClassName("podium-container");

        podium.add(createPodiumColumn("🥈 2ème", second, "silver", "podium-col-second", showTournoi));
        podium.add(createPodiumColumn("🥇 1er", first, "gold", "podium-col-first", showTournoi));
        podium.add(createPodiumColumn("🥉 3ème", third, "bronze", "podium-col-third", showTournoi));

        return podium;
    }

    private VerticalLayout createPodiumColumn(String title, List<ButeurInfo> players, String color, String columnClass, boolean showTournoi) {
        VerticalLayout col = new VerticalLayout();
        col.setAlignItems(Alignment.CENTER);
        col.setSpacing(false);
        col.setPadding(false);
        col.addClassName("podium-column");
        col.addClassName(columnClass);

        Label lbl = new Label(title);
        lbl.addClassName("podium-title");
        col.add(lbl);

        Div box = new Div();
        box.addClassName("podium-box");
        box.addClassName("podium-box-" + color);

        if (players.isEmpty()) {
            Span empty = new Span("-");
            empty.addClassName("podium-player-empty");
            box.add(empty);
        } else {
            for (ButeurInfo player : players) {
                Span playerSpan = new Span(player.getNomJoueur());
                playerSpan.addClassName("podium-player-name");

                String tooltip = player.getNomEquipe() + " — " + player.getNombreButs() + " but" + (player.getNombreButs() > 1 ? "s" : "");
                if (showTournoi) {
                    tooltip += " (" + player.getNomTournoi() + ")";
                }
                playerSpan.getElement().setProperty("title", tooltip);

                box.add(playerSpan);
            }
        }

        col.add(box);
        return col;
    }

    private void showLeaderboardCurrent() {
        contentLayout.removeAll();
        btnViewAllCurrent.setVisible(true);
        btnBackCurrent.setVisible(false);
        btnViewAllAll.setVisible(false);
        btnBackAll.setVisible(false);
        gridCurrentTournoi.setVisible(false);
        contentLayout.add(leaderboardCurrentView);
    }

    private void showTableCurrent() {
        contentLayout.removeAll();
        btnViewAllCurrent.setVisible(false);
        btnBackCurrent.setVisible(true);
        btnViewAllAll.setVisible(false);
        btnBackAll.setVisible(false);
        gridCurrentTournoi.setVisible(true);
        contentLayout.add(gridCurrentTournoi);
    }

    private void showLeaderboardAll() {
        contentLayout.removeAll();
        btnViewAllCurrent.setVisible(false);
        btnBackCurrent.setVisible(false);
        btnViewAllAll.setVisible(true);
        btnBackAll.setVisible(false);
        gridAllTournois.setVisible(false);
        contentLayout.add(leaderboardAllView);
    }

    private void showTableAll() {
        contentLayout.removeAll();
        btnViewAllCurrent.setVisible(false);
        btnBackCurrent.setVisible(false);
        btnViewAllAll.setVisible(false);
        btnBackAll.setVisible(true);
        gridAllTournois.setVisible(true);
        contentLayout.add(gridAllTournois);
    }
    private List<ButeurInfo> onlyScorers(List<ButeurInfo> src) {
    List<ButeurInfo> out = new ArrayList<>();
    for (ButeurInfo b : src) {
        if (b.getNombreButs() > 0) out.add(b);
    }
    return out;
}
private void updateLeaderboardCurrent() {
    leaderboardCurrentView.removeAll();

    List<ButeurInfo> scorers = onlyScorers(currentButeursCache);
    leaderboardCurrentView.add(buildPodium(scorers, false));

    if (scorers.isEmpty()) {
        leaderboardCurrentView.add(buildNoDataMessage());
    }
}

private void updateLeaderboardAll() {
    leaderboardAllView.removeAll();

    List<ButeurInfo> scorers = onlyScorers(allButeursCache);
    leaderboardAllView.add(buildPodium(scorers, true));

    if (scorers.isEmpty()) {
        leaderboardAllView.add(buildNoDataMessage());
    }
}

}
