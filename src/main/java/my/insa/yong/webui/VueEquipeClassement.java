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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.components.BaseLayout;

@PageTitle("Meilleures Équipes")
@Route(value = "equipes-classement", layout = BaseLayout.class)
public class VueEquipeClassement extends VerticalLayout {

    private Grid<EquipeInfo> equipesGrid;
    private Paragraph noDataMessage;

    public static class EquipeInfo {
        private int place;
        private String nomEquipe;
        private int victoires;
        private int defaites;
        private int matchsNuls;
        private int buts;

        public EquipeInfo(int place, String nomEquipe, int victoires, int defaites, int matchsNuls, int buts) {
            this.place = place;
            this.nomEquipe = nomEquipe;
            this.victoires = victoires;
            this.defaites = defaites;
            this.matchsNuls = matchsNuls;
            this.buts = buts;
        }

        public String getPlaceFormatted() {
            return switch (place) {
                case 1 -> "🥇 1er";
                case 2 -> "🥈 2ème";
                case 3 -> "🥉 3ème";
                default -> String.valueOf(place);
            };
        }

        public String getNomEquipe() { return nomEquipe; }
        public int getVictoires() { return victoires; }
        public int getDefaites() { return defaites; }
        public int getMatchsNuls() { return matchsNuls; }
        public int getButs() { return buts; }
        public int getPoints() { return victoires * 3 + matchsNuls; }
    }

    public VueEquipeClassement() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("app-container");

        // Titre
        H2 titre = new H2("🏆 Classement des Meilleures Équipes");
        titre.addClassName("page-title");
        add(titre);

        // Grille des équipes
        equipesGrid = new Grid<>(EquipeInfo.class, false);
        equipesGrid.addColumn(EquipeInfo::getPlaceFormatted).setHeader("Place").setAutoWidth(true);
        equipesGrid.addColumn(EquipeInfo::getNomEquipe).setHeader("Équipe").setAutoWidth(true).setFlexGrow(1);
        equipesGrid.addColumn(EquipeInfo::getPoints).setHeader("Points").setAutoWidth(true);
        equipesGrid.addColumn(EquipeInfo::getVictoires).setHeader("Victoires").setAutoWidth(true);
        equipesGrid.addColumn(EquipeInfo::getMatchsNuls).setHeader("Nuls").setAutoWidth(true);
        equipesGrid.addColumn(EquipeInfo::getDefaites).setHeader("Défaites").setAutoWidth(true);
        equipesGrid.addColumn(EquipeInfo::getButs).setHeader("Buts").setAutoWidth(true);
        equipesGrid.setSizeFull();
        equipesGrid.setClassNameGenerator(equipe -> {
            return switch (equipe.place) {
                case 1 -> "gold-row";
                case 2 -> "silver-row";
                case 3 -> "bronze-row";
                default -> "";
            };
        });

        // Message vide
        noDataMessage = new Paragraph("⚽ Aucune donnée disponible pour le moment.");
        noDataMessage.addClassName("no-data-message");
        noDataMessage.setVisible(false);

        add(equipesGrid, noDataMessage);

        // Charger les données
        chargerMeilleuresEquipes();
    }

    private void chargerMeilleuresEquipes() {
        List<EquipeInfo> equipes = new ArrayList<>();
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            
            String sql = "SELECT e.nom_equipe, " +
                         "SUM(CASE WHEN r.winner_id = e.id THEN 1 ELSE 0 END) as victoires, " +
                         "SUM(CASE WHEN r.winner_id IS NULL AND r.played = TRUE THEN 1 ELSE 0 END) as matchs_nuls, " +
                         "SUM(CASE WHEN (r.equipe_a_id = e.id OR r.equipe_b_id = e.id) " +
                         "     AND r.winner_id IS NOT NULL AND r.winner_id != e.id THEN 1 ELSE 0 END) as defaites, " +
                         "COALESCE(COUNT(b.id), 0) as total_buts " +
                         "FROM equipe e " +
                         "LEFT JOIN rencontre r ON (r.equipe_a_id = e.id OR r.equipe_b_id = e.id) " +
                         "     AND r.tournoi_id = e.tournoi_id " +
                         "LEFT JOIN but b ON b.equipe_id = e.id AND b.tournoi_id = e.tournoi_id " +
                         "WHERE e.tournoi_id = ? " +
                         "GROUP BY e.id, e.nom_equipe " +
                         "ORDER BY victoires DESC, total_buts DESC";
            
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, tournoiId);
                try (ResultSet rs = pst.executeQuery()) {
                    int place = 1;
                    while (rs.next()) {
                        String nomEquipe = rs.getString("nom_equipe");
                        int victoires = rs.getInt("victoires");
                        int defaites = rs.getInt("defaites");
                        int matchsNuls = rs.getInt("matchs_nuls");
                        int buts = rs.getInt("total_buts");
                        
                        equipes.add(new EquipeInfo(place++, nomEquipe, victoires, defaites, matchsNuls, buts));
                    }
                }
            }
            
            equipesGrid.setItems(equipes);
            noDataMessage.setVisible(equipes.isEmpty());
        } catch (SQLException ex) {
            ex.printStackTrace();
            noDataMessage.setText("Erreur lors du chargement des données : " + ex.getMessage());
            noDataMessage.setVisible(true);
        }
    }
}
