package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;

import my.insa.yong.model.JoueurClassement;
import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;

public class VueJoueursClassement extends VerticalLayout {

    private Grid<JoueurClassement.ButeurInfo> gridCurrentTournoi;
    private Grid<JoueurClassement.ButeurInfoAllTournois> gridAllTournois;
    private VerticalLayout contentLayout;
    private VerticalLayout leaderboardCurrentView;
    private VerticalLayout leaderboardAllView;
    private Button btnViewAllCurrent;
    private Button btnBackCurrent;
    private Button btnViewAllAll;
    private Button btnBackAll;
    private List<JoueurClassement.ButeurInfo> currentButeursCache;
    private List<JoueurClassement.ButeurInfoAllTournois> allButeursCache;

    public VueJoueursClassement() {
        setSizeFull();
        setPadding(false);
        setSpacing(true);

        Tabs tabs = new Tabs();
        Tab tabCurrentTournoi = new Tab("Tournoi Actuel");
        Tab tabAllTournois = new Tab("Tous les Tournois");
        tabs.add(tabCurrentTournoi, tabAllTournois);
        tabs.setSelectedTab(tabCurrentTournoi);

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

        HorizontalLayout tabHeader = new HorizontalLayout();
        tabHeader.setWidthFull();
        tabHeader.setAlignItems(Alignment.CENTER);
        tabHeader.add(tabs);
        tabHeader.expand(tabs);
        tabHeader.add(btnViewAllCurrent, btnBackCurrent, btnViewAllAll, btnBackAll);

        contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setSpacing(true);
        contentLayout.setPadding(false);

        gridCurrentTournoi = createGrid(false);
        gridAllTournois = createAllGrid(true);

        leaderboardCurrentView = new VerticalLayout();
        leaderboardCurrentView.setWidthFull();
        leaderboardCurrentView.setPadding(false);

        leaderboardAllView = new VerticalLayout();
        leaderboardAllView.setWidthFull();
        leaderboardAllView.setPadding(false);

        currentButeursCache = new ArrayList<>();
        allButeursCache = new ArrayList<>();

        loadButeursCurrentTournoi();
        loadButeursAllTournois();

        showLeaderboardCurrent();

        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab() == tabCurrentTournoi) {
                showLeaderboardCurrent();
            } else {
                showLeaderboardAll();
            }
        });

        add(tabHeader, contentLayout);
        expand(contentLayout);
    }

    @SuppressWarnings("deprecation")
    private Grid<JoueurClassement.ButeurInfo> createGrid(boolean showTournoi) {
        Grid<JoueurClassement.ButeurInfo> grid = new Grid<>(JoueurClassement.ButeurInfo.class, false);
        grid.setSizeFull();
        grid.setPageSize(20);

        grid.addColumn(JoueurClassement.ButeurInfo::getPlaceFormatted).setHeader("Place").setWidth("100px").setFlexGrow(0);
        grid.addColumn(JoueurClassement.ButeurInfo::getNomJoueur).setHeader("Joueur").setAutoWidth(true);
        grid.addColumn(JoueurClassement.ButeurInfo::getNomEquipe).setHeader("Équipe").setAutoWidth(true);
        grid.addColumn(JoueurClassement.ButeurInfo::getNombreButs).setHeader("Buts").setWidth("100px").setFlexGrow(0);

        if (showTournoi && !showTournoi) { // This is intentionally false - for future all-tournaments grid
            // grid.addColumn(...).setHeader("Tournoi")...
        }

        grid.setClassNameGenerator(buteur -> {
            if (buteur.getPlace() == 1) return "gold-row";
            if (buteur.getPlace() == 2) return "silver-row";
            if (buteur.getPlace() == 3) return "bronze-row";
            return "";
        });

        return grid;
    }

    @SuppressWarnings("deprecation")
    private Grid<JoueurClassement.ButeurInfoAllTournois> createAllGrid(boolean showTournoi) {
        Grid<JoueurClassement.ButeurInfoAllTournois> grid = new Grid<>(JoueurClassement.ButeurInfoAllTournois.class, false);
        grid.setSizeFull();
        grid.setPageSize(20);

        grid.addColumn(JoueurClassement.ButeurInfoAllTournois::getPlaceFormatted).setHeader("Place").setWidth("100px").setFlexGrow(0);
        grid.addColumn(JoueurClassement.ButeurInfoAllTournois::getNomJoueur).setHeader("Joueur").setAutoWidth(true);
        grid.addColumn(JoueurClassement.ButeurInfoAllTournois::getNomEquipe).setHeader("Équipe").setAutoWidth(true);
        grid.addColumn(JoueurClassement.ButeurInfoAllTournois::getNombreButs).setHeader("Buts").setWidth("100px").setFlexGrow(0);

        if (showTournoi) {
            grid.addColumn(JoueurClassement.ButeurInfoAllTournois::getNomTournoi).setHeader("Tournoi").setAutoWidth(true);
        }

        grid.setClassNameGenerator(buteur -> {
            if (buteur.getPlace() == 1) return "gold-row";
            if (buteur.getPlace() == 2) return "silver-row";
            if (buteur.getPlace() == 3) return "bronze-row";
            return "";
        });

        return grid;
    }

    private void loadButeursCurrentTournoi() {
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

        try (Connection con = ConnectionPool.getConnection()) {
            List<JoueurClassement.ButeurInfo> buteurs = JoueurClassement.chargerButeursTournoiActuel(con, tournoiId);
            currentButeursCache = buteurs;
            gridCurrentTournoi.setItems(buteurs);
            updateLeaderboardCurrent();
        } catch (SQLException ex) {
            // Silent error handling
        }
    }

    private void loadButeursAllTournois() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<JoueurClassement.ButeurInfoAllTournois> buteurs = JoueurClassement.chargerButeursAllTournois(con);
            allButeursCache = buteurs;
            gridAllTournois.setItems(buteurs);
            updateLeaderboardAll();
        } catch (SQLException ex) {
            // Silent error handling
        }
    }

    private HorizontalLayout buildPodium(List<JoueurClassement.ButeurInfo> buteurs, boolean showTournoi) {
        List<JoueurClassement.ButeurInfo> first = new ArrayList<>();
        List<JoueurClassement.ButeurInfo> second = new ArrayList<>();
        List<JoueurClassement.ButeurInfo> third = new ArrayList<>();

        for (JoueurClassement.ButeurInfo b : buteurs) {
            switch (b.getPlace()) {
                case 1 -> first.add(b);
                case 2 -> second.add(b);
                case 3 -> third.add(b);
            }
        }

        HorizontalLayout podium = new HorizontalLayout();
        podium.setWidthFull();
        podium.setJustifyContentMode(JustifyContentMode.CENTER);
        podium.setAlignItems(Alignment.END);
        podium.setSpacing(true);
        podium.addClassName("podium-container");

        podium.add(createPodiumColumn("🥈 2ème", second, "silver", "podium-col-second"));
        podium.add(createPodiumColumn("🥇 1er", first, "gold", "podium-col-first"));
        podium.add(createPodiumColumn("🥉 3ème", third, "bronze", "podium-col-third"));

        return podium;
    }

    private HorizontalLayout buildPodiumAll(List<JoueurClassement.ButeurInfoAllTournois> buteurs) {
        List<JoueurClassement.ButeurInfoAllTournois> first = new ArrayList<>();
        List<JoueurClassement.ButeurInfoAllTournois> second = new ArrayList<>();
        List<JoueurClassement.ButeurInfoAllTournois> third = new ArrayList<>();

        for (JoueurClassement.ButeurInfoAllTournois b : buteurs) {
            switch (b.getPlace()) {
                case 1 -> first.add(b);
                case 2 -> second.add(b);
                case 3 -> third.add(b);
            }
        }

        HorizontalLayout podium = new HorizontalLayout();
        podium.setWidthFull();
        podium.setJustifyContentMode(JustifyContentMode.CENTER);
        podium.setAlignItems(Alignment.END);
        podium.setSpacing(true);
        podium.addClassName("podium-container");

        podium.add(createPodiumColumnAll("🥈 2ème", second, "silver", "podium-col-second"));
        podium.add(createPodiumColumnAll("🥇 1er", first, "gold", "podium-col-first"));
        podium.add(createPodiumColumnAll("🥉 3ème", third, "bronze", "podium-col-third"));

        return podium;
    }

    private VerticalLayout createPodiumColumn(String title, List<JoueurClassement.ButeurInfo> players, String color, String columnClass) {
        VerticalLayout col = new VerticalLayout();
        col.setAlignItems(Alignment.CENTER);
        col.setSpacing(false);
        col.setPadding(false);
        col.addClassName("podium-column");
        col.addClassName(columnClass);

        Span lbl = new Span(title);
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
            for (JoueurClassement.ButeurInfo player : players) {
                Span playerSpan = new Span(player.getNomJoueur());
                playerSpan.addClassName("podium-player-name");
                String tooltip = player.getNomEquipe() + " — " + player.getNombreButs() + " but" + (player.getNombreButs() > 1 ? "s" : "");
                playerSpan.getElement().setProperty("title", tooltip);
                box.add(playerSpan);
            }
        }

        col.add(box);
        return col;
    }

    private VerticalLayout createPodiumColumnAll(String title, List<JoueurClassement.ButeurInfoAllTournois> players, String color, String columnClass) {
        VerticalLayout col = new VerticalLayout();
        col.setAlignItems(Alignment.CENTER);
        col.setSpacing(false);
        col.setPadding(false);
        col.addClassName("podium-column");
        col.addClassName(columnClass);

        Span lbl = new Span(title);
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
            for (JoueurClassement.ButeurInfoAllTournois player : players) {
                Span playerSpan = new Span(player.getNomJoueur());
                playerSpan.addClassName("podium-player-name");
                String tooltip = player.getNomEquipe() + " — " + player.getNombreButs() + " but" + (player.getNombreButs() > 1 ? "s" : "") 
                    + " (" + player.getNomTournoi() + ")";
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
        contentLayout.add(leaderboardCurrentView);
    }

    private void showTableCurrent() {
        contentLayout.removeAll();
        btnViewAllCurrent.setVisible(false);
        btnBackCurrent.setVisible(true);
        btnViewAllAll.setVisible(false);
        btnBackAll.setVisible(false);
        contentLayout.add(gridCurrentTournoi);
    }

    private void showLeaderboardAll() {
        contentLayout.removeAll();
        btnViewAllCurrent.setVisible(false);
        btnBackCurrent.setVisible(false);
        btnViewAllAll.setVisible(true);
        btnBackAll.setVisible(false);
        contentLayout.add(leaderboardAllView);
    }

    private void showTableAll() {
        contentLayout.removeAll();
        btnViewAllCurrent.setVisible(false);
        btnBackCurrent.setVisible(false);
        btnViewAllAll.setVisible(false);
        btnBackAll.setVisible(true);
        contentLayout.add(gridAllTournois);
    }

    private List<JoueurClassement.ButeurInfo> onlyScorers(List<JoueurClassement.ButeurInfo> src) {
        List<JoueurClassement.ButeurInfo> out = new ArrayList<>();
        for (JoueurClassement.ButeurInfo b : src) {
            if (b.getNombreButs() > 0) out.add(b);
        }
        return out;
    }

    private List<JoueurClassement.ButeurInfoAllTournois> onlyScorersAll(List<JoueurClassement.ButeurInfoAllTournois> src) {
        List<JoueurClassement.ButeurInfoAllTournois> out = new ArrayList<>();
        for (JoueurClassement.ButeurInfoAllTournois b : src) {
            if (b.getNombreButs() > 0) out.add(b);
        }
        return out;
    }

    private void updateLeaderboardCurrent() {
        leaderboardCurrentView.removeAll();
        List<JoueurClassement.ButeurInfo> scorers = onlyScorers(currentButeursCache);
        leaderboardCurrentView.add(buildPodium(scorers, false));
        if (scorers.isEmpty()) {
            Paragraph p = new Paragraph("⚽ Aucun but marqué pour le moment.");
            p.addClassName("no-data-message");
            leaderboardCurrentView.add(p);
        }
    }

    private void updateLeaderboardAll() {
        leaderboardAllView.removeAll();
        List<JoueurClassement.ButeurInfoAllTournois> scorers = onlyScorersAll(allButeursCache);
        leaderboardAllView.add(buildPodiumAll(scorers));
        if (scorers.isEmpty()) {
            Paragraph p = new Paragraph("⚽ Aucun but marqué pour le moment.");
            p.addClassName("no-data-message");
            leaderboardAllView.add(p);
        }
    }
}
