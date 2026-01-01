package my.insa.yong.webui;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.shared.Registration;

import my.insa.yong.model.Classement;
import my.insa.yong.model.UserSession;
import my.insa.yong.webui.components.BaseLayout;

@Route(value = "")
@PageTitle("Accueil")
public class VuePrincipale extends BaseLayout {

    // ===== Réglages faciles =====
    private static final int POLL_INTERVAL_MS = 3000;
    private static final int CAROUSEL_INTERVAL_MS = 8000;
    private static final int MATCHES_PER_PAGE = 4; // 2x2
    // ===========================

    private final List<MatchInfo> ongoingMatches = new ArrayList<>();

    // matchId -> [scoreA, scoreB]
    private final Map<Integer, int[]> lastScoresByMatchId = new HashMap<>();
    private boolean firstLoadDone = false;

    private Registration pollRegistration;

    // Carousel
    private int currentPageIndex = 0;
    private long lastCarouselSwitchMs = 0L;

    // UI
    private Div matchesGridContainer;
    private Span pageIndicator;

    public VuePrincipale() {
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setSizeFull();
        wrapper.addClassName("app-container");
        wrapper.setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        if (UserSession.userConnected()) {
            wrapper.setJustifyContentMode(VerticalLayout.JustifyContentMode.START);
            construireInterfaceConnecte(wrapper);
        } else {
            wrapper.setJustifyContentMode(VerticalLayout.JustifyContentMode.CENTER);

            VerticalLayout container = new VerticalLayout();
            container.addClassName("form-container");
            container.addClassName("form-container-large");
            container.addClassName("fade-in");
            container.setAlignItems(Alignment.CENTER);
            container.setSpacing(true);

            construireInterfaceUtilisateurNonConnecte(container);
            wrapper.add(container);
        }

        this.setContent(wrapper);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        if (!UserSession.userConnected()) return;

        UI ui = attachEvent.getUI();

        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }

        ui.setPollInterval(POLL_INTERVAL_MS);
        pollRegistration = ui.addPollListener(e -> {
            chargerEtMettreAJour(true);
            gererCarousel();
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }
    }

    private void construireInterfaceConnecte(VerticalLayout wrapper) {
        H1 titre = new H1("🏆 Tableau de Bord - Matchs en Direct");
        titre.addClassName("page-title");

        HorizontalLayout userInfoSection = new HorizontalLayout();
        userInfoSection.setWidthFull();
        userInfoSection.setJustifyContentMode(JustifyContentMode.CENTER);
        userInfoSection.setAlignItems(Alignment.CENTER);
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

        pageIndicator = new Span("");
        pageIndicator.getStyle()
                .set("margin-top", "8px")
                .set("margin-bottom", "2px")
                .set("font-size", "0.9em")
                .set("opacity", "0.75");

        // ✅ Grille 2 colonnes avec PLUS D’AIR
        matchesGridContainer = new Div();
        matchesGridContainer.getStyle()
                .set("width", "100%")
                .set("max-width", "1350px")
                .set("margin", "10px auto 0 auto")
                .set("padding", "10px 14px")          // <-- air autour
                .set("display", "grid")
                .set("grid-template-columns", "repeat(2, minmax(0, 1fr))")
                .set("column-gap", "40px")            // <-- plus d’espace horizontal
                .set("row-gap", "22px")               // <-- plus d’espace vertical
                .set("align-items", "start");

        // Chargement initial
        chargerEtMettreAJour(false);
        lastCarouselSwitchMs = System.currentTimeMillis();

        wrapper.add(titre, userInfoSection, pageIndicator, matchesGridContainer);
    }

    private void chargerEtMettreAJour(boolean notifyGoals) {
        chargerMatchsEnCours();

        if (notifyGoals && firstLoadDone) {
            detecterButsEtNotifier();
        }

        int pageCount = getPageCount();
        if (pageCount <= 0) pageCount = 1;
        if (currentPageIndex >= pageCount) currentPageIndex = 0;

        afficherPageCourante();
        memoriserScoresActuels();

        firstLoadDone = true;
    }

    private void gererCarousel() {
        int pageCount = getPageCount();
        if (pageCount <= 1) return;

        long now = System.currentTimeMillis();
        if (now - lastCarouselSwitchMs >= CAROUSEL_INTERVAL_MS) {
            currentPageIndex = (currentPageIndex + 1) % pageCount;
            lastCarouselSwitchMs = now;
            afficherPageCourante();
        }
    }

    private int getPageCount() {
        if (ongoingMatches.isEmpty()) return 1;
        int total = ongoingMatches.size();
        int size = Math.max(1, MATCHES_PER_PAGE);
        return (total + size - 1) / size;
    }

    private void afficherPageCourante() {
        if (matchesGridContainer == null) return;
        matchesGridContainer.removeAll();

        if (ongoingMatches.isEmpty()) {
            matchesGridContainer.add(creerCarteMatch(new MatchInfo(0, 0, "À déterminer", "À déterminer", 0, 0, "-")));
            updatePageIndicator(1, 1);
            return;
        }

        int total = ongoingMatches.size();
        int size = Math.max(1, MATCHES_PER_PAGE);

        int pageCount = getPageCount();
        int start = currentPageIndex * size;
        int end = Math.min(start + size, total);

        List<MatchInfo> sub = ongoingMatches.subList(start, end);
        for (MatchInfo match : sub) {
            matchesGridContainer.add(creerCarteMatch(match));
        }

        updatePageIndicator(currentPageIndex + 1, pageCount);
    }

    private void updatePageIndicator(int page, int totalPages) {
        if (pageIndicator == null) return;
        pageIndicator.setText("Page " + page + " / " + totalPages + "  •  Auto-défilement : " + (CAROUSEL_INTERVAL_MS / 1000) + "s");
    }

    private Div creerCarteMatch(MatchInfo match) {
        Div card = new Div();

        // ✅ carte un peu plus “fine” + marge interne/respiration
        card.getStyle()
                .set("padding", "10px 14px")
                .set("border-radius", "18px")
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("width", "100%");

        H2 matchTitle = new H2("Match en Direct - Round " + match.roundNumber);
        matchTitle.getStyle()
                .set("margin", "4px 0 10px 0")
                .set("font-size", "1.45rem")
                .set("text-align", "center");

        HorizontalLayout matchLayout = new HorizontalLayout();
        matchLayout.setWidthFull();
        matchLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        matchLayout.setAlignItems(Alignment.CENTER);
        matchLayout.setSpacing(true);

        VerticalLayout teamALayout = creerLayoutEquipe(match.teamAName, match.scoreA, true);
        teamALayout.getStyle().set("flex", "1");

        Div vsSection = new Div();
        vsSection.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("min-width", "46px");

        Span vsText = new Span("VS");
        vsText.getStyle().set("opacity", "0.70").set("font-size", "0.95rem");

        vsSection.add(vsText);

        VerticalLayout teamBLayout = creerLayoutEquipe(match.teamBName, match.scoreB, false);
        teamBLayout.getStyle().set("flex", "1");

        matchLayout.add(teamALayout, vsSection, teamBLayout);

        Div terrainInfo = new Div();
        terrainInfo.getStyle()
                .set("margin-top", "10px")
                .set("display", "flex")
                .set("justify-content", "center")
                .set("gap", "8px");

        Span terrainLabel = new Span("📍 Terrain:");
        terrainLabel.getStyle().set("opacity", "0.8").set("font-weight", "600");

        Span terrainName = new Span(match.terrainName != null ? match.terrainName : "Non assigné");
        terrainName.getStyle().set("opacity", "0.9");

        terrainInfo.add(terrainLabel, terrainName);

        card.add(matchTitle, matchLayout, terrainInfo);
        return card;
    }

    private VerticalLayout creerLayoutEquipe(String teamName, int score, boolean isTeamA) {
        VerticalLayout teamLayout = new VerticalLayout();
        teamLayout.setAlignItems(Alignment.CENTER);
        teamLayout.setSpacing(true);
        teamLayout.setPadding(true);

        // ✅ un peu plus compact
        teamLayout.getStyle()
                .set("background", "rgba(255,255,255,0.75)")
                .set("border-radius", "14px")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("min-height", "135px")          // <-- réduit un peu
                .set("justify-content", "center")
                .set("padding", "12px");

        if (isTeamA) {
            teamLayout.getStyle().set("border-left", "6px solid var(--lumo-primary-color)");
        } else {
            teamLayout.getStyle().set("border-right", "6px solid #f59e0b");
        }

        H2 teamNameSpan = new H2(teamName != null ? teamName : "À déterminer");
        teamNameSpan.getStyle()
                .set("margin", "0")
                .set("font-size", "1.35rem")
                .set("text-align", "center");

        Div scoreBox = new Div();
        scoreBox.getStyle()
                .set("padding", "12px 18px")
                .set("border-radius", "14px")
                .set("min-width", "86px")
                .set("text-align", "center")
                .set("background", "linear-gradient(135deg, rgba(99,102,241,0.95), rgba(124,58,237,0.95))");

        Span scoreSpan = new Span(String.valueOf(score));
        scoreSpan.getStyle()
                .set("color", "white")
                .set("font-size", "2.35rem")
                .set("font-weight", "800")
                .set("text-shadow", "0 1px 2px rgba(0,0,0,0.25)");

        scoreBox.add(scoreSpan);

        teamLayout.add(teamNameSpan, scoreBox);
        return teamLayout;
    }

    private void chargerMatchsEnCours() {
        ongoingMatches.clear();
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

        try {
            List<Classement.DashboardMatchInfo> matches = Classement.chargerTousLesMatchs(tournoiId);
            for (Classement.DashboardMatchInfo match : matches) {
                ongoingMatches.add(convertToMatchInfo(match));
            }
        } catch (SQLException ex) {
            System.err.println("Erreur lors du chargement des matchs en cours: " + ex.getMessage());
        }

        if (ongoingMatches.isEmpty()) {
            ongoingMatches.add(new MatchInfo(0, 0, "À déterminer", "À déterminer", 0, 0, "-"));
        }
    }

    private MatchInfo convertToMatchInfo(Classement.DashboardMatchInfo modelMatch) {
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

    private void detecterButsEtNotifier() {
        for (MatchInfo m : ongoingMatches) {
            if (m.id <= 0) continue;

            int[] old = lastScoresByMatchId.get(m.id);
            if (old == null) continue;

            int deltaA = m.scoreA - old[0];
            int deltaB = m.scoreB - old[1];

            if (deltaA > 0) notifierBut(m.teamAName, deltaA, m);
            if (deltaB > 0) notifierBut(m.teamBName, deltaB, m);
        }
    }

    private void notifierBut(String equipe, int nbButs, MatchInfo m) {
        String eq = (equipe == null || equipe.isBlank()) ? "Une équipe" : equipe;

        String msg = (nbButs == 1)
                ? "⚽ BUT pour " + eq + " ! (Round " + m.roundNumber + ") → " + m.scoreA + " - " + m.scoreB
                : "⚽ " + nbButs + " buts pour " + eq + " ! (Round " + m.roundNumber + ") → " + m.scoreA + " - " + m.scoreB;

        Notification n = Notification.show(msg, 2500, Notification.Position.TOP_END);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void memoriserScoresActuels() {
        for (MatchInfo m : ongoingMatches) {
            if (m.id <= 0) continue;
            lastScoresByMatchId.put(m.id, new int[]{m.scoreA, m.scoreB});
        }
    }

    private static class MatchInfo {
        int id;
        int roundNumber;
        String teamAName;
        String teamBName;
        int scoreA;
        int scoreB;
        String terrainName;

        MatchInfo(int matchId, int matchRound, String nameA, String nameB,
                  int scoreTeamA, int scoreTeamB, String terrain) {
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
