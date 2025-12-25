package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import my.insa.yong.model.EquipeClassement;
import my.insa.yong.model.EquipeClassement.RankingInfo;
import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.components.BaseLayout;

@PageTitle("Meilleures Équipes")
@Route(value = "equipes-classement", layout = BaseLayout.class)
public class VueEquipeClassement extends VerticalLayout {

    private final Grid<RankingInfo> equipesGrid;
    private final Paragraph noDataMessage;

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
        equipesGrid = new Grid<>(RankingInfo.class, false);
        equipesGrid.addColumn(RankingInfo::getPlaceFormatted).setHeader("Place").setAutoWidth(true);
        equipesGrid.addColumn(RankingInfo::getNomEquipe).setHeader("Équipe").setAutoWidth(true).setFlexGrow(1);
        equipesGrid.addColumn(r -> r.getPoints()).setHeader("Points").setAutoWidth(true);
        equipesGrid.addColumn(RankingInfo::getVictoires).setHeader("Victoires").setAutoWidth(true);
        equipesGrid.addColumn(RankingInfo::getMatchsNuls).setHeader("Nuls").setAutoWidth(true);
        equipesGrid.addColumn(RankingInfo::getDefaites).setHeader("Défaites").setAutoWidth(true);
        equipesGrid.addColumn(RankingInfo::getButs).setHeader("Buts").setAutoWidth(true);
        equipesGrid.setSizeFull();
        
        // Appliquer les styles selon la place
        equipesGrid.setPartNameGenerator(equipe -> {
            return switch (equipe.getPlace()) {
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
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            List<RankingInfo> equipes = EquipeClassement.chargerClassementTournoiActuel(con, tournoiId);
            
            equipesGrid.setItems(equipes);
            noDataMessage.setVisible(equipes.isEmpty());
        } catch (SQLException ex) {
            System.err.println("Erreur lors du chargement des meilleures équipes: " + ex.getMessage());
            noDataMessage.setText("Erreur lors du chargement des données : " + ex.getMessage());
            noDataMessage.setVisible(true);
        }
    }
}
