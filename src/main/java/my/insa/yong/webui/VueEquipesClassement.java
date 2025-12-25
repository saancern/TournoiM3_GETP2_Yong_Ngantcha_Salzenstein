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

import my.insa.yong.model.Classement;
import my.insa.yong.model.Classement.RankingInfoAllTournois;
import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;

public class VueEquipesClassement extends VerticalLayout {

    private final Grid<RankingInfoAllTournois> gridCurrentTournoi;
    private final Grid<RankingInfoAllTournois> gridAllTournois;
    private final VerticalLayout contentLayout;
    private final VerticalLayout leaderboardCurrentView;
    private final VerticalLayout leaderboardAllView;
    private final Button btnViewAllCurrent;
    private final Button btnBackCurrent;
    private final Button btnViewAllAll;
    private final Button btnBackAll;
    private List<RankingInfoAllTournois> currentEquipesCache = new ArrayList<>();
    private List<RankingInfoAllTournois> allEquipesCache = new ArrayList<>();

    public VueEquipesClassement() {
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
        gridAllTournois = createGrid(true);

        leaderboardCurrentView = new VerticalLayout();
        leaderboardCurrentView.setWidthFull();
        leaderboardCurrentView.setPadding(false);

        leaderboardAllView = new VerticalLayout();
        leaderboardAllView.setWidthFull();
        leaderboardAllView.setPadding(false);

        loadEquipesCurrentTournoi();
        loadEquipesAllTournois();

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
    private Grid<RankingInfoAllTournois> createGrid(boolean showTournoi) {
        Grid<RankingInfoAllTournois> grid = new Grid<>(RankingInfoAllTournois.class, false);
        grid.setSizeFull();
        grid.setPageSize(20);

        grid.addColumn(RankingInfoAllTournois::getPlaceFormatted).setHeader("Place").setWidth("100px").setFlexGrow(0);
        grid.addColumn(RankingInfoAllTournois::getNomEquipe).setHeader("Équipe").setAutoWidth(true);
        grid.addColumn(RankingInfoAllTournois::getPoints).setHeader("Points").setWidth("100px").setFlexGrow(0);
        grid.addColumn(RankingInfoAllTournois::getVictoires).setHeader("V").setWidth("80px").setFlexGrow(0);
        grid.addColumn(RankingInfoAllTournois::getMatchsNuls).setHeader("N").setWidth("80px").setFlexGrow(0);
        grid.addColumn(RankingInfoAllTournois::getDefaites).setHeader("D").setWidth("80px").setFlexGrow(0);

        if (showTournoi) {
            grid.addColumn(RankingInfoAllTournois::getNomTournoi).setHeader("Tournoi").setAutoWidth(true);
        }

        grid.setClassNameGenerator(equipe -> {
            if (equipe.getPlace() == 1) return "gold-row";
            if (equipe.getPlace() == 2) return "silver-row";
            if (equipe.getPlace() == 3) return "bronze-row";
            return "";
        });

        return grid;
    }

    private void loadEquipesCurrentTournoi() {
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            currentEquipesCache = Classement.chargerClassementTournoiActuelAvecPoints(con, tournoiId);
            gridCurrentTournoi.setItems(currentEquipesCache);
            updateLeaderboardCurrent();
        } catch (SQLException ex) {
            System.err.println("Erreur lors du chargement des équipes du tournoi actuel: " + ex.getMessage());
        }
    }

    private void loadEquipesAllTournois() {
        try (Connection con = ConnectionPool.getConnection()) {
            allEquipesCache = Classement.chargerClassementTousTournois(con);
            gridAllTournois.setItems(allEquipesCache);
            updateLeaderboardAll();
        } catch (SQLException ex) {
            System.err.println("Erreur lors du chargement des équipes de tous les tournois: " + ex.getMessage());
        }
    }

    private HorizontalLayout buildPodium(List<RankingInfoAllTournois> equipes, boolean showTournoi) {
        List<RankingInfoAllTournois> first = new ArrayList<>();
        List<RankingInfoAllTournois> second = new ArrayList<>();
        List<RankingInfoAllTournois> third = new ArrayList<>();

        for (RankingInfoAllTournois eq : equipes) {
            switch (eq.getPlace()) {
                case 1 -> first.add(eq);
                case 2 -> second.add(eq);
                case 3 -> third.add(eq);
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

    private VerticalLayout createPodiumColumn(String title, List<RankingInfoAllTournois> teams, String color, String columnClass, boolean showTournoi) {
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

        if (teams.isEmpty()) {
            Span empty = new Span("-");
            empty.addClassName("podium-player-empty");
            box.add(empty);
        } else {
            for (RankingInfoAllTournois team : teams) {
                Span teamSpan = new Span(team.getNomEquipe());
                teamSpan.addClassName("podium-player-name");
                String tooltip = team.getPoints() + " pts — " + team.getVictoires() + "V " + team.getMatchsNuls() + "N " + team.getDefaites() + "D";
                if (showTournoi) {
                    tooltip += " (" + team.getNomTournoi() + ")";
                }
                teamSpan.getElement().setProperty("title", tooltip);
                box.add(teamSpan);
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

    private List<RankingInfoAllTournois> onlyWithPoints(List<RankingInfoAllTournois> src) {
        List<RankingInfoAllTournois> out = new ArrayList<>();
        for (RankingInfoAllTournois eq : src) {
            if (eq.getPoints() > 0 || eq.getVictoires() > 0 || eq.getMatchsNuls() > 0) {
                out.add(eq);
            }
        }
        return out;
    }

    private void updateLeaderboardCurrent() {
        leaderboardCurrentView.removeAll();
        List<RankingInfoAllTournois> withPoints = onlyWithPoints(currentEquipesCache);
        leaderboardCurrentView.add(buildPodium(withPoints, false));
        if (withPoints.isEmpty()) {
            Paragraph p = new Paragraph("🏆 Aucun match joué pour le moment.");
            p.addClassName("no-data-message");
            leaderboardCurrentView.add(p);
        }
    }

    private void updateLeaderboardAll() {
        leaderboardAllView.removeAll();
        List<RankingInfoAllTournois> withPoints = onlyWithPoints(allEquipesCache);
        leaderboardAllView.add(buildPodium(withPoints, true));
        if (withPoints.isEmpty()) {
            Paragraph p = new Paragraph("🏆 Aucun match joué pour le moment.");
            p.addClassName("no-data-message");
            leaderboardAllView.add(p);
        }
    }
}
