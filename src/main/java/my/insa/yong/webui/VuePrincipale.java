package my.insa.yong.webui;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import my.insa.yong.model.PrincipaleClassement;
import my.insa.yong.model.UserSession;
import my.insa.yong.webui.components.BaseLayout;

/**
 *
 * @author saancern
 */
@Route(value = "")
@PageTitle("Accueil")
public class VuePrincipale extends BaseLayout {

    private final List<MatchInfo> ongoingMatches = new ArrayList<>();
    private int currentMatchIndex = 0;
    private VerticalLayout liveMatchContainer;
    private Span matchCounter;
    private H1 titre;

    public VuePrincipale() {
        // Wrapper avec gradient background pour centrer le contenu
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setSizeFull();
        wrapper.addClassName("app-container");
        wrapper.setJustifyContentMode(VerticalLayout.JustifyContentMode.CENTER);
        wrapper.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        
        if (UserSession.userConnected()) {
            // Contenu pour utilisateur connecté avec matchs en direct
            construireInterfaceAvecMatchsEnDirect(wrapper);
            
            // Ajouter mise à jour automatique toutes les 3 secondes (live update pour tous les utilisateurs)
            getUI().ifPresent(ui -> {
                ui.setPollInterval(3000);
                // Rafraîchir les données de match à chaque cycle de polling
                ui.addPollListener(e -> {
                    chargerMatchsEnCours();
                    afficherMatchCourant();
                    mettAJourCounter();
                });
            });
        } else {
            // Contenu pour utilisateur non connecté
            VerticalLayout container = new VerticalLayout();
            container.addClassName("form-container");
            container.addClassName("form-container-large");
            container.addClassName("fade-in");
            container.setAlignItems(Alignment.CENTER);
            container.setSpacing(true);
            construireInterfaceUtilisateurNonConnecte(container);
            wrapper.add(container);
        }
        
        // Ajouter le wrapper dans le AppLayout
        this.setContent(wrapper);
    }

    private void construireInterfaceAvecMatchsEnDirect(VerticalLayout wrapper) {
        // Titre
        titre = new H1("🏆 Tableau de Bord - Matchs en Direct");
        titre.addClassName("page-title");

        // Section des matchs en direct
        VerticalLayout liveSection = new VerticalLayout();
        liveSection.addClassName("live-matches-section");
        liveSection.setWidth("100%");
        liveSection.setMaxWidth("1200px");

        // Container pour afficher le match courant
        liveMatchContainer = new VerticalLayout();
        liveMatchContainer.setSizeUndefined();
        liveMatchContainer.setAlignItems(Alignment.CENTER);
        liveMatchContainer.setSpacing(true);

        // Charger les matchs en cours et afficher
        chargerMatchsEnCours();
        afficherMatchCourant();

        // Navigation entre matchs
        HorizontalLayout navigationLayout = new HorizontalLayout();
        navigationLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        navigationLayout.setSpacing(true);
        navigationLayout.setWidthFull();

        Button btnPrevious = new Button("← Précédent");
        btnPrevious.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnPrevious.addClickListener(e -> {
            if (!ongoingMatches.isEmpty()) {
                currentMatchIndex = (currentMatchIndex - 1 + ongoingMatches.size()) % ongoingMatches.size();
                afficherMatchCourant();
                mettAJourCounter();
            }
        });

        matchCounter = new Span((currentMatchIndex + 1) + " / " + ongoingMatches.size());
        matchCounter.addClassName("match-counter");

        Button btnNext = new Button("Suivant →");
        btnNext.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnNext.addClickListener(e -> {
            if (!ongoingMatches.isEmpty()) {
                currentMatchIndex = (currentMatchIndex + 1) % ongoingMatches.size();
                afficherMatchCourant();
                mettAJourCounter();
            }
        });

        Button btnRefresh = new Button("🔄 Actualiser");
        btnRefresh.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnRefresh.addClickListener(e -> {
            chargerMatchsEnCours();
            currentMatchIndex = 0;
            afficherMatchCourant();
            mettAJourCounter();
        });

        navigationLayout.add(btnPrevious, matchCounter, btnNext, btnRefresh);

        liveSection.add(liveMatchContainer, navigationLayout);
        wrapper.add(titre, liveSection);

        // Informations utilisateur
        HorizontalLayout userInfoSection = new HorizontalLayout();
        userInfoSection.setWidthFull();
        userInfoSection.setJustifyContentMode(JustifyContentMode.CENTER);
        userInfoSection.setSpacing(true);

        String username = UserSession.getCurrentUsername();
        String role = UserSession.getCurrentUserRoleDisplay();
        Span userInfo = new Span("👤 " + username + " (" + role + ")");
        userInfo.addClassName("user-info-badge");

        Button boutonChangerUtilisateur = new Button("Changer d'utilisateur");
        boutonChangerUtilisateur.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        boutonChangerUtilisateur.addClickListener(e -> {
            UserSession.logout();
            UserSession.clearCurrentTournoi();
            getUI().ifPresent(ui -> ui.navigate(VueConnexion.class));
        });

        userInfoSection.add(userInfo, boutonChangerUtilisateur);
        wrapper.add(userInfoSection);
    }

    private void mettAJourCounter() {
        if (matchCounter != null) {
            matchCounter.setText((currentMatchIndex + 1) + " / " + ongoingMatches.size());
        }
    }

    private void afficherMatchCourant() {
        liveMatchContainer.removeAll();

        if (ongoingMatches.isEmpty()) {
            return;
        }

        MatchInfo match = ongoingMatches.get(currentMatchIndex);

        // Titre du match
        H2 matchTitle = new H2("Match en Direct - Round " + match.roundNumber);
        matchTitle.addClassName("match-title");

        // Layout principal du match
        HorizontalLayout matchLayout = new HorizontalLayout();
        matchLayout.setWidthFull();
        matchLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        matchLayout.setAlignItems(Alignment.CENTER);
        matchLayout.setSpacing(true);
        matchLayout.addClassName("live-match-display");

        // Équipe A
        VerticalLayout teamALayout = creerLayoutEquipe(
            match.teamAName, 
            match.scoreA, 
            true
        );

        // VS avec score global
        Div vsSection = new Div();
        vsSection.addClassName("vs-section");
        H1 scoreDisplay = new H1(match.scoreA + " - " + match.scoreB);
        scoreDisplay.addClassName("score-display");
        Span vsText = new Span("VS");
        vsText.addClassName("vs-text");
        vsSection.add(vsText, scoreDisplay);

        // Équipe B
        VerticalLayout teamBLayout = creerLayoutEquipe(
            match.teamBName, 
            match.scoreB, 
            false
        );

        matchLayout.add(teamALayout, vsSection, teamBLayout);

        // Informations du terrain
        Div terrainInfo = new Div();
        terrainInfo.addClassName("terrain-info");
        Span terrainLabel = new Span("📍 Terrain:");
        terrainLabel.addClassName("terrain-label");
        Span terrainName = new Span(match.terrainName != null ? match.terrainName : "Non assigné");
        terrainName.addClassName("terrain-name");
        terrainInfo.add(terrainLabel, terrainName);

        liveMatchContainer.add(matchTitle, matchLayout, terrainInfo);
    }

    private VerticalLayout creerLayoutEquipe(String teamName, int score, boolean isTeamA) {
        VerticalLayout teamLayout = new VerticalLayout();
        teamLayout.setAlignItems(Alignment.CENTER);
        teamLayout.setSpacing(true);
        teamLayout.addClassName("team-card");
        if (isTeamA) {
            teamLayout.addClassName("team-card-a");
        } else {
            teamLayout.addClassName("team-card-b");
        }

        H2 teamNameSpan = new H2(teamName);
        teamNameSpan.addClassName("team-name");

        Div scoreBox = new Div();
        scoreBox.addClassName("score-box");
        Span scoreSpan = new Span(String.valueOf(score));
        scoreSpan.addClassName("team-score");
        scoreBox.add(scoreSpan);

        teamLayout.add(teamNameSpan, scoreBox);
        return teamLayout;
    }

    private void chargerMatchsEnCours() {
        ongoingMatches.clear();
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

        try {
            my.insa.yong.model.PrincipaleClassement.MatchInfo modelMatch = PrincipaleClassement.chargerDernierMatch(tournoiId);
            ongoingMatches.add(convertToMatchInfo(modelMatch));
        } catch (SQLException ex) {
            System.err.println("Erreur lors du chargement des matchs en cours: " + ex.getMessage());
        }

        // Ajouter un match par défaut si aucun match n'existe
        if (ongoingMatches.isEmpty()) {
            ongoingMatches.add(new MatchInfo(0, 0, "À déterminer", "À déterminer", 0, 0, "-"));
        }

        currentMatchIndex = 0;
    }

    private MatchInfo convertToMatchInfo(my.insa.yong.model.PrincipaleClassement.MatchInfo modelMatch) {
        return new MatchInfo(
            modelMatch.getId(),
            modelMatch.getRoundNumber(),
            modelMatch.getTeamAName(),
            modelMatch.getTeamBName(),
            modelMatch.getScoreA(),
            modelMatch.getScoreB(),
            modelMatch.getTerrainName()
        );
    }

// Classe interne pour stocker les informations d'un match
private static class MatchInfo {
    @SuppressWarnings("unused")
    int id;
    int roundNumber;
    String teamAName;
    String teamBName;
    int scoreA;
    int scoreB;
    String terrainName;

    MatchInfo(int matchId, int matchRound, String nameA, String nameB, int scoreTeamA, int scoreTeamB, String terrain) {
        this.id = matchId;
        this.roundNumber = matchRound;
        this.teamAName = nameA;
        this.teamBName = nameB;
        this.scoreA = scoreTeamA;
        this.scoreB = scoreTeamB;
        this.terrainName = terrain;
    }
}

private void construireInterfaceUtilisateurNonConnecte(VerticalLayout container) {
        H1 welcomeTitle = new H1("Bienvenue sur l'application de gestion de tournois !");
        welcomeTitle.addClassName("page-title");

        Paragraph description = new Paragraph(
            "Gérez facilement vos tournois sportifs avec notre application intuitive. " +
            "Connectez-vous pour accéder à vos matchs en direct, gérer les équipes, " +
            "et bien plus encore."
        );
        description.addClassName("description-text");

        RouterLink lienConnexion = new RouterLink("Se connecter / S'inscrire", VueConnexion.class);
        lienConnexion.addClassName("link-button-primary");

        container.add(welcomeTitle, description, lienConnexion);
    }
}