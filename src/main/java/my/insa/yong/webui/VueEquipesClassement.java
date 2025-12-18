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
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;

import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;

public class VueEquipesClassement extends VerticalLayout {

    private Grid<EquipeInfo> gridCurrentTournoi;
    private Grid<EquipeInfo> gridAllTournois;
    private VerticalLayout contentLayout;
    private VerticalLayout leaderboardCurrentView;
    private VerticalLayout leaderboardAllView;
    private Button btnViewAllCurrent;
    private Button btnBackCurrent;
    private Button btnViewAllAll;
    private Button btnBackAll;
    private List<EquipeInfo> currentEquipesCache = new ArrayList<>();
    private List<EquipeInfo> allEquipesCache = new ArrayList<>();

    public static class EquipeInfo {
        private int place;
        private String nomEquipe;
        private int points;
        private int victoires;
        private int defaites;
        private int matchsNuls;
        private String nomTournoi;

        public EquipeInfo(int place, String nomEquipe, int points, int victoires, int defaites, int matchsNuls) {
            this.place = place;
            this.nomEquipe = nomEquipe;
            this.points = points;
            this.victoires = victoires;
            this.defaites = defaites;
            this.matchsNuls = matchsNuls;
        }

        public EquipeInfo(int place, String nomEquipe, int points, int victoires, int defaites, int matchsNuls, String nomTournoi) {
            this.place = place;
            this.nomEquipe = nomEquipe;
            this.points = points;
            this.victoires = victoires;
            this.defaites = defaites;
            this.matchsNuls = matchsNuls;
            this.nomTournoi = nomTournoi;
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
        public String getNomEquipe() { return nomEquipe; }
        public int getPoints() { return points; }
        public int getVictoires() { return victoires; }
        public int getDefaites() { return defaites; }
        public int getMatchsNuls() { return matchsNuls; }
        public String getNomTournoi() { return nomTournoi; }
    }

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
    private Grid<EquipeInfo> createGrid(boolean showTournoi) {
        Grid<EquipeInfo> grid = new Grid<>(EquipeInfo.class, false);
        grid.setSizeFull();
        grid.setPageSize(20);

        grid.addColumn(EquipeInfo::getPlaceFormatted).setHeader("Place").setWidth("100px").setFlexGrow(0);
        grid.addColumn(EquipeInfo::getNomEquipe).setHeader("Équipe").setAutoWidth(true);
        grid.addColumn(EquipeInfo::getPoints).setHeader("Points").setWidth("100px").setFlexGrow(0);
        grid.addColumn(EquipeInfo::getVictoires).setHeader("V").setWidth("80px").setFlexGrow(0);
        grid.addColumn(EquipeInfo::getMatchsNuls).setHeader("N").setWidth("80px").setFlexGrow(0);
        grid.addColumn(EquipeInfo::getDefaites).setHeader("D").setWidth("80px").setFlexGrow(0);

        if (showTournoi) {
            grid.addColumn(EquipeInfo::getNomTournoi).setHeader("Tournoi").setAutoWidth(true);
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
        List<EquipeInfo> equipes = new ArrayList<>();
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

        String sql = "SELECT e.id, e.nom_equipe, " +
                     "SUM(CASE WHEN r.winner_id = e.id THEN 3 " +
                     "         WHEN r.winner_id IS NOT NULL AND r.winner_id != e.id THEN 0 " +
                     "         WHEN r.equipe1_id = e.id OR r.equipe2_id = e.id THEN 1 " +
                     "         ELSE 0 END) as points, " +
                     "SUM(CASE WHEN r.winner_id = e.id THEN 1 ELSE 0 END) as victoires, " +
                     "SUM(CASE WHEN r.winner_id IS NOT NULL AND r.winner_id != e.id AND (r.equipe1_id = e.id OR r.equipe2_id = e.id) THEN 1 ELSE 0 END) as defaites, " +
                     "SUM(CASE WHEN r.winner_id IS NULL AND (r.equipe1_id = e.id OR r.equipe2_id = e.id) THEN 1 ELSE 0 END) as matchsNuls " +
                     "FROM equipe e " +
                     "LEFT JOIN rencontre r ON (r.equipe1_id = e.id OR r.equipe2_id = e.id) AND r.tournoi_id = ? " +
                     "WHERE e.tournoi_id = ? " +
                     "GROUP BY e.id, e.nom_equipe " +
                     "ORDER BY points DESC, victoires DESC, e.nom_equipe ASC";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoiId);
            pst.setInt(2, tournoiId);
            ResultSet rs = pst.executeQuery();

            int rank = 0;
            Integer previousPoints = null;

            while (rs.next()) {
                int points = rs.getInt("points");
                if (previousPoints == null || points != previousPoints) {
                    rank++;
                }
                equipes.add(new EquipeInfo(rank, 
                    rs.getString("nom_equipe"),
                    points,
                    rs.getInt("victoires"),
                    rs.getInt("defaites"),
                    rs.getInt("matchsNuls")));
                previousPoints = points;
            }
        } catch (SQLException ex) {}

        currentEquipesCache = equipes;
        gridCurrentTournoi.setItems(equipes);
        updateLeaderboardCurrent();
    }

    private void loadEquipesAllTournois() {
        List<EquipeInfo> equipes = new ArrayList<>();
        String sql = "SELECT e.id, e.nom_equipe, t.nom_tournoi, " +
                     "SUM(CASE WHEN r.winner_id = e.id THEN 3 " +
                     "         WHEN r.winner_id IS NOT NULL AND r.winner_id != e.id THEN 0 " +
                     "         WHEN r.equipe1_id = e.id OR r.equipe2_id = e.id THEN 1 " +
                     "         ELSE 0 END) as points, " +
                     "SUM(CASE WHEN r.winner_id = e.id THEN 1 ELSE 0 END) as victoires, " +
                     "SUM(CASE WHEN r.winner_id IS NOT NULL AND r.winner_id != e.id AND (r.equipe1_id = e.id OR r.equipe2_id = e.id) THEN 1 ELSE 0 END) as defaites, " +
                     "SUM(CASE WHEN r.winner_id IS NULL AND (r.equipe1_id = e.id OR r.equipe2_id = e.id) THEN 1 ELSE 0 END) as matchsNuls " +
                     "FROM equipe e " +
                     "LEFT JOIN rencontre r ON (r.equipe1_id = e.id OR r.equipe2_id = e.id) " +
                     "LEFT JOIN tournoi t ON e.tournoi_id = t.id " +
                     "GROUP BY e.id, e.nom_equipe, t.nom_tournoi " +
                     "ORDER BY points DESC, victoires DESC, e.nom_equipe ASC";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();
            int rank = 0;
            Integer previousPoints = null;

            while (rs.next()) {
                int points = rs.getInt("points");
                if (previousPoints == null || points != previousPoints) {
                    rank++;
                }
                equipes.add(new EquipeInfo(rank,
                    rs.getString("nom_equipe"),
                    points,
                    rs.getInt("victoires"),
                    rs.getInt("defaites"),
                    rs.getInt("matchsNuls"),
                    rs.getString("nom_tournoi") != null ? rs.getString("nom_tournoi") : "N/A"));
                previousPoints = points;
            }
        } catch (SQLException ex) {}

        allEquipesCache = equipes;
        gridAllTournois.setItems(equipes);
        updateLeaderboardAll();
    }

    private HorizontalLayout buildPodium(List<EquipeInfo> equipes, boolean showTournoi) {
        List<EquipeInfo> first = new ArrayList<>();
        List<EquipeInfo> second = new ArrayList<>();
        List<EquipeInfo> third = new ArrayList<>();

        for (EquipeInfo eq : equipes) {
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

        podium.add(createPodiumColumn("🥈 2ème", second, "silver", showTournoi));
        podium.add(createPodiumColumn("🥇 1er", first, "gold", showTournoi));
        podium.add(createPodiumColumn("🥉 3ème", third, "bronze", showTournoi));

        return podium;
    }

    private VerticalLayout createPodiumColumn(String title, List<EquipeInfo> teams, String color, boolean showTournoi) {
        VerticalLayout col = new VerticalLayout();
        col.setAlignItems(Alignment.CENTER);
        col.setSpacing(false);
        col.setPadding(false);
        col.addClassName("podium-column");

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
            for (EquipeInfo team : teams) {
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

    private List<EquipeInfo> onlyWithPoints(List<EquipeInfo> src) {
        List<EquipeInfo> out = new ArrayList<>();
        for (EquipeInfo eq : src) {
            if (eq.getPoints() > 0 || eq.getVictoires() > 0 || eq.getMatchsNuls() > 0) {
                out.add(eq);
            }
        }
        return out;
    }

    private void updateLeaderboardCurrent() {
        leaderboardCurrentView.removeAll();
        List<EquipeInfo> withPoints = onlyWithPoints(currentEquipesCache);
        leaderboardCurrentView.add(buildPodium(withPoints, false));
        if (withPoints.isEmpty()) {
            Paragraph p = new Paragraph("🏆 Aucun match joué pour le moment.");
            p.addClassName("no-data-message");
            leaderboardCurrentView.add(p);
        }
    }

    private void updateLeaderboardAll() {
        leaderboardAllView.removeAll();
        List<EquipeInfo> withPoints = onlyWithPoints(allEquipesCache);
        leaderboardAllView.add(buildPodium(withPoints, true));
        if (withPoints.isEmpty()) {
            Paragraph p = new Paragraph("🏆 Aucun match joué pour le moment.");
            p.addClassName("no-data-message");
            leaderboardAllView.add(p);
        }
    }
}
