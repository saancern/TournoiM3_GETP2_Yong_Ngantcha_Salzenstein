package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import my.insa.yong.model.StatistiquesJoueur;
import my.insa.yong.model.StatistiquesJoueur.MatchHistorique;
import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.components.BaseLayout;

/**
 * Tableau de bord personnalisé pour les joueurs
 * Affiche le prochain match, les statistiques et l'historique
 * @author saancern
 */
@Route(value = "tableau-de-bord", layout = BaseLayout.class)
@PageTitle("Tableau de Bord")
public class VueDashboardJoueur extends VerticalLayout implements BeforeEnterObserver {

    private StatistiquesJoueur stats;

    public VueDashboardJoueur() {
        addClassName("dashboard-container");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Vérifier que l'utilisateur est un joueur
        if (!UserSession.joueurConnected()) {
            Notification.show("Accès réservé aux joueurs", 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            event.rerouteTo("");
            return;
        }

        Integer joueurId = UserSession.getCurrentJoueurId();
        if (joueurId == null) {
            Notification.show("Profil joueur non trouvé", 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            event.rerouteTo("");
            return;
        }

        chargerDashboard(joueurId);
    }

    private void chargerDashboard(int joueurId) {
        removeAll();

        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

            // Charger les statistiques
            stats = new StatistiquesJoueur(joueurId);
            stats.chargerStatistiques(con, tournoiId);

            // Créer le contenu du tableau de bord
            construireHeader();
            construireProchainMatch(con, joueurId, tournoiId);
            construireStatsCards();
            construireHistoriqueMatchs(con, joueurId, tournoiId);

        } catch (SQLException e) {
            Notification.show("Erreur lors du chargement du tableau de bord: " + e.getMessage(), 
                5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void construireHeader() {
        H2 titre = new H2("Tableau de Bord");
        titre.addClassName("dashboard-title");

        H3 nomJoueur = new H3(stats.getPrenomNom());
        nomJoueur.addClassName("dashboard-player-name");

        Span equipe = new Span("Équipe: " + stats.getEquipeActuelle());
        equipe.addClassName("dashboard-team");

        VerticalLayout header = new VerticalLayout(titre, nomJoueur, equipe);
        header.setSpacing(false);
        header.setPadding(false);
        header.addClassName("dashboard-header");

        add(header);
    }

    private void construireProchainMatch(Connection con, int joueurId, int tournoiId) throws SQLException {
        H3 titreProchain = new H3("Prochain Match");
        titreProchain.addClassName("section-title");

        MatchHistorique prochainMatch = StatistiquesJoueur.getProchainMatch(con, joueurId, tournoiId);

        Div matchCard = new Div();
        matchCard.addClassName("prochain-match-card");

        if (prochainMatch.getRound().equals("Aucun match à venir")) {
            Span aucunMatch = new Span("Aucun match prévu pour le moment");
            aucunMatch.addClassName("no-data-message");
            matchCard.add(aucunMatch);
        } else {
            // Round
            Span round = new Span(prochainMatch.getRound());
            round.addClassName("match-round");

            // Match info
            Span matchInfo = new Span(prochainMatch.getMatch());
            matchInfo.addClassName("match-info");

            // Terrain
            Span terrain = new Span("Terrain: " + prochainMatch.getTerrain());
            terrain.addClassName("match-terrain");

            VerticalLayout matchDetails = new VerticalLayout(round, matchInfo, terrain);
            matchDetails.setSpacing(false);
            matchDetails.setPadding(false);

            matchCard.add(matchDetails);
        }

        add(titreProchain, matchCard);
    }

    private void construireStatsCards() {
        H3 titreStats = new H3("Statistiques");
        titreStats.addClassName("section-title");

        // Première ligne de cards
        HorizontalLayout ligne1 = new HorizontalLayout();
        ligne1.setWidthFull();
        ligne1.addClassName("stats-row");

        ligne1.add(
            creerStatCard("Buts", String.valueOf(stats.getNombreButs()), "primary"),
            creerStatCard("Matchs", String.valueOf(stats.getMatchsJoues()), "success"),
            creerStatCard("Efficacité", stats.getEfficaciteFormatted() + " buts/match", "contrast")
        );

        // Deuxième ligne de cards
        HorizontalLayout ligne2 = new HorizontalLayout();
        ligne2.setWidthFull();
        ligne2.addClassName("stats-row");

        ligne2.add(
            creerStatCard("Victoires", String.valueOf(stats.getVictoires()), "success"),
            creerStatCard("Défaites", String.valueOf(stats.getDefaites()), "error"),
            creerStatCard("Nuls", String.valueOf(stats.getMatchsNuls()), "contrast")
        );

        // Troisième ligne avec stats avancées
        HorizontalLayout ligne3 = new HorizontalLayout();
        ligne3.setWidthFull();
        ligne3.addClassName("stats-row");

        ligne3.add(
            creerStatCard("Classement", stats.getClassementFormatted(), "primary"),
            creerStatCard("Taux Victoire", stats.getTauxVictoireFormatted(), "success"),
            creerStatCard("Moyenne", stats.getEfficaciteFormatted(), "contrast")
        );

        add(titreStats, ligne1, ligne2, ligne3);
    }

    private void construireHistoriqueMatchs(Connection con, int joueurId, int tournoiId) throws SQLException {
        H3 titreHistorique = new H3("Derniers Matchs");
        titreHistorique.addClassName("section-title");

        List<MatchHistorique> historique = stats.chargerHistoriqueMatchs(con, tournoiId);

        VerticalLayout historiqueContainer = new VerticalLayout();
        historiqueContainer.addClassName("historique-container");
        historiqueContainer.setSpacing(true);
        historiqueContainer.setPadding(false);

        if (historique.isEmpty()) {
            Span aucunMatch = new Span("Aucun match joué pour le moment");
            aucunMatch.addClassName("no-data-message");
            historiqueContainer.add(aucunMatch);
        } else {
            for (MatchHistorique match : historique) {
                historiqueContainer.add(creerMatchCard(match));
            }
        }

        add(titreHistorique, historiqueContainer);
    }

    private Div creerStatCard(String label, String valeur, String theme) {
        Div card = new Div();
        card.addClassName("stat-card");
        card.addClassName("stat-card-" + theme);

        Span labelSpan = new Span(label);
        labelSpan.addClassName("stat-label");

        Span valeurSpan = new Span(valeur);
        valeurSpan.addClassName("stat-value");

        card.add(labelSpan, valeurSpan);

        return card;
    }

    private Div creerMatchCard(MatchHistorique match) {
        Div card = new Div();
        card.addClassName("match-card");

        // Round
        Span round = new Span(match.getRound());
        round.addClassName("match-round");

        // Match info
        Span matchInfo = new Span(match.getMatch());
        matchInfo.addClassName("match-info");

        // Résultat avec couleur
        Span resultat = new Span(match.getResultat());
        resultat.addClassName("match-result");
        
        switch (match.getResultat()) {
            case "Victoire" -> resultat.addClassName("result-victoire");
            case "Défaite" -> resultat.addClassName("result-defaite");
            case "Nul" -> resultat.addClassName("result-nul");
            default -> resultat.addClassName("result-avenir");
        }

        // Score
        Span score = new Span(match.getScore());
        score.addClassName("match-score");

        // Mes buts
        Span mesButs = new Span(match.getMesButsFormatted());
        mesButs.addClassName("match-buts");

        HorizontalLayout topRow = new HorizontalLayout(round, matchInfo);
        topRow.setWidthFull();
        topRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        topRow.addClassName("match-top-row");

        HorizontalLayout bottomRow = new HorizontalLayout(resultat, score, mesButs);
        bottomRow.setWidthFull();
        bottomRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        bottomRow.addClassName("match-bottom-row");

        card.add(topRow, bottomRow);

        return card;
    }
}
