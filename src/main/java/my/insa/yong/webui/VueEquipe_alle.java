package my.insa.yong.webui;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

import my.insa.yong.model.Equipe;
import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.components.BaseLayout;

@Route(value = "equipes", layout = BaseLayout.class)
@PageTitle("Galerie des Équipes")
@AnonymousAllowed
public class VueEquipe_alle extends VerticalLayout {

    private HorizontalLayout avatarContainer;
    private final List<Equipe> equipes;

    private String currentSortBy = "Nom";
    private String currentOrder = "A-Z";

    // Cache: nb joueurs par équipe
    private final Map<Integer, Integer> nbJoueursCache = new HashMap<>();

    // Cache logos
    private final Map<Integer, LogoData> logoCache = new HashMap<>();

    private static class LogoData {
        final byte[] bytes;
        final String mime;
        final String filename;

        LogoData(byte[] bytes, String mime, String filename) {
            this.bytes = bytes;
            this.mime = mime;
            this.filename = filename;
        }
    }

    public VueEquipe_alle() {
        this.equipes = new ArrayList<>();

        initializeLayout();
        chargerEquipes();
        creerCartesEquipes();
    }

    private void initializeLayout() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("avatar-gallery-container");

        H2 titre = new H2("Galerie des Équipes");
        titre.addClassName("avatar-gallery-title");

        Span infoTournoi = new Span("Tournoi : " + UserSession.getCurrentTournoiName()
                + " (" + UserSession.getCurrentTournoiSport() + ")");
        infoTournoi.addClassName("avatar-gallery-info");

        avatarContainer = new HorizontalLayout();
        avatarContainer.setWidthFull();
        avatarContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        avatarContainer.setAlignItems(FlexComponent.Alignment.START);
        avatarContainer.getStyle().set("flex-wrap", "wrap");
        avatarContainer.getStyle().set("gap", "20px");
        avatarContainer.addClassName("avatar-grid");

        HorizontalLayout sortLayout = new HorizontalLayout();
        sortLayout.setAlignItems(FlexComponent.Alignment.END);
        sortLayout.setSpacing(true);

        ComboBox<String> sortComboBox = new ComboBox<>("Trier par:");
        sortComboBox.setItems("Nom", "Date de création", "Nombre de joueurs", "ID");
        sortComboBox.setValue("Nom");
        sortComboBox.setWidth("220px");
        sortComboBox.setAllowCustomValue(false);
        sortComboBox.setAllowedCharPattern("^$");

        ComboBox<String> orderComboBox = new ComboBox<>("Ordre:");
        orderComboBox.setItems("A-Z", "Z-A");
        orderComboBox.setValue("A-Z");
        orderComboBox.setWidth("150px");
        orderComboBox.setAllowCustomValue(false);
        orderComboBox.setAllowedCharPattern("^$");

        sortComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                currentSortBy = event.getValue();
                chargerEquipes(currentSortBy, currentOrder);
                rafraichirCartes();
            }
        });

        orderComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                currentOrder = event.getValue();
                chargerEquipes(currentSortBy, currentOrder);
                rafraichirCartes();
            }
        });

        sortLayout.add(sortComboBox, orderComboBox);

        add(titre, infoTournoi, sortLayout, avatarContainer);
    }

    private void chargerEquipes() {
        chargerEquipes(currentSortBy, currentOrder);
    }

    private void chargerEquipes(String sortBy, String order) {
        equipes.clear();
        nbJoueursCache.clear();

        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
        String dir = order.equals("A-Z") ? "ASC" : "DESC";

        String orderBy;
        switch (sortBy) {
            case "Date de création" -> orderBy = "e.date_creation " + dir + ", e.nom_equipe ASC";
            case "Nombre de joueurs" -> orderBy = "nb_joueurs " + dir + ", e.nom_equipe ASC";
            case "ID" -> orderBy = "e.id " + dir + ", e.nom_equipe ASC";
            case "Nom" -> orderBy = "e.nom_equipe " + dir + ", e.date_creation ASC";
            default -> orderBy = "e.nom_equipe " + dir + ", e.date_creation ASC";
        }

        String sql = """
                SELECT e.id, e.nom_equipe, e.date_creation,
                       COUNT(je.joueur_id) AS nb_joueurs
                FROM equipe e
                LEFT JOIN joueur_equipe je
                       ON je.equipe_id = e.id AND je.tournoi_id = e.tournoi_id
                WHERE e.tournoi_id = ?
                GROUP BY e.id, e.nom_equipe, e.date_creation
                ORDER BY %s
                """.formatted(orderBy);

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, tournoiId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String nomEquipe = rs.getString("nom_equipe");
                    Date dc = rs.getDate("date_creation");
                    LocalDate dateCreation = (dc != null) ? dc.toLocalDate() : LocalDate.now();

                    Equipe eq = new Equipe(id, nomEquipe, dateCreation);
                    equipes.add(eq);

                    nbJoueursCache.put(id, rs.getInt("nb_joueurs"));
                }
            }

            // Précharger tous les logos du tournoi (1 requête)
            prechargerLogos(tournoiId);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void prechargerLogos(int tournoiId) {
        logoCache.clear();

        String sql = "SELECT id, logo, logo_mime, logo_nom FROM equipe WHERE tournoi_id = ? AND logo IS NOT NULL";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, tournoiId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    byte[] bytes = rs.getBytes("logo");
                    String mime = rs.getString("logo_mime");
                    String nom = rs.getString("logo_nom");

                    if (bytes != null && bytes.length > 0) {
                        if (mime == null || mime.isBlank()) mime = "image/png";
                        if (nom == null || nom.isBlank()) nom = "equipe-" + id;
                        logoCache.put(id, new LogoData(bytes, mime, nom));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void creerCartesEquipes() {
        for (Equipe equipe : equipes) {
            VerticalLayout card = creerCarteEquipe(equipe);
            avatarContainer.add(card);
        }
    }

    private void rafraichirCartes() {
        avatarContainer.removeAll();
        creerCartesEquipes();
    }

    private VerticalLayout creerCarteEquipe(Equipe equipe) {
        VerticalLayout cardLayout = new VerticalLayout();
        cardLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        cardLayout.setPadding(true);
        cardLayout.setSpacing(true);
        cardLayout.addClassName("avatar-card");
        cardLayout.setWidth("200px");
        cardLayout.getStyle().set("flex-shrink", "0");

        Avatar avatar = new Avatar();
        avatar.setName(equipe.getNomEquipe());
        avatar.setWidth("80px");
        avatar.setHeight("80px");
        avatar.setColorIndex(colorIndexFromId(equipe.getId()));

        // ✅ Logo à la place des initiales
        appliquerLogoSiDisponible(avatar, equipe.getId());

        H3 nom = new H3(equipe.getNomEquipe());
        nom.addClassName("avatar-card-name");

        Integer nb = nbJoueursCache.getOrDefault(equipe.getId(), 0);
        Span sub = new Span(nb + " joueur(s)");
        sub.getStyle().set("font-size", "0.9em").set("color", "var(--lumo-secondary-text-color)");

        cardLayout.addClickListener(e -> ouvrirDetailEquipe(equipe));

        cardLayout.add(avatar, nom, sub);
        return cardLayout;
    }

    private int colorIndexFromId(int id) {
        // Avatar color index: valeur stable (évite couleur aléatoire à chaque refresh)
        int idx = Math.abs(id) % 7;
        return idx;
    }

    private void appliquerLogoSiDisponible(Avatar avatar, int equipeId) {
        LogoData data = logoCache.get(equipeId);
        if (data == null) {
            // pas de logo => Vaadin affichera les initiales automatiquement
            return;
        }

        DownloadHandler handler = DownloadHandler.fromInputStream(event ->
                new DownloadResponse(
                        new ByteArrayInputStream(data.bytes),
                        data.filename,
                        data.mime,
                        data.bytes.length
                )
        ).inline();

        avatar.setImageHandler(handler);
    }

    private void ouvrirDetailEquipe(Equipe equipe) {
        Dialog dialog = new Dialog();
        dialog.addClassName("avatar-dialog");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        VerticalLayout contenu = new VerticalLayout();
        contenu.addClassName("avatar-dialog-content");

        Avatar avatarGrand = new Avatar();
        avatarGrand.setName(equipe.getNomEquipe());
        avatarGrand.setWidth("120px");
        avatarGrand.setHeight("120px");
        avatarGrand.setColorIndex(colorIndexFromId(equipe.getId()));
        appliquerLogoSiDisponible(avatarGrand, equipe.getId());

        H2 nomEquipe = new H2(equipe.getNomEquipe());
        nomEquipe.addClassName("avatar-dialog-title");

        // ---- Info équipe ----
        Card infoCard = new Card();
        infoCard.addClassName("avatar-dialog-info-card");

        VerticalLayout infoLayout = new VerticalLayout();
        infoLayout.setPadding(true);
        infoLayout.setSpacing(true);

        infoLayout.add(
                creerInfoRow("Créée le", String.valueOf(equipe.getDateCreation())),
                creerInfoRow("Nombre de joueurs", String.valueOf(nbJoueursCache.getOrDefault(equipe.getId(), 0))),
                creerInfoRow("Joueurs", obtenirJoueursEquipe(equipe.getId()))
        );

        infoCard.add(infoLayout);

        // ---- Matchs ----
        H4 titreMatchs = new H4("Liste des matchs");
        titreMatchs.getStyle().set("margin", "20px 0 10px 0");

        List<MatchInfo> matches = obtenirMatchsEquipe(equipe.getId());

        Card matchsCard = new Card();
        matchsCard.addClassName("avatar-dialog-matches-card");
        matchsCard.setWidthFull();

        VerticalLayout matchsLayout = new VerticalLayout();
        matchsLayout.setPadding(true);
        matchsLayout.setSpacing(true);

        if (matches.isEmpty()) {
            Span aucun = new Span("Aucun match trouvé");
            aucun.getStyle().set("font-style", "italic")
                    .set("color", "var(--lumo-secondary-text-color)");
            matchsLayout.add(aucun);
        } else {
            List<MatchInfo> limites = matches.subList(0, Math.min(matches.size(), 5));
            for (MatchInfo match : limites) {
                HorizontalLayout row = new HorizontalLayout();
                row.setWidthFull();
                row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
                row.setAlignItems(FlexComponent.Alignment.CENTER);
                row.getStyle().set("padding", "8px 0");

                VerticalLayout left = new VerticalLayout();
                left.setSpacing(false);
                left.setPadding(false);

                Span adv = new Span("vs " + match.getAdversaire());
                adv.getStyle().set("font-weight", "500");

                Span details = new Span(match.getRound() + " • " + match.getStatut());
                details.getStyle().set("font-size", "0.9em")
                        .set("color", "var(--lumo-secondary-text-color)");

                left.add(adv, details);

                Span score = new Span(match.getScore());
                score.getStyle().set("font-weight", "600")
                        .set("color", "var(--lumo-primary-text-color)");

                row.add(left, score);
                matchsLayout.add(row);
            }

            if (matches.size() > 5) {
                Span plus = new Span("... et " + (matches.size() - 5) + " autre(s) match(s)");
                plus.getStyle().set("font-size", "0.9em")
                        .set("color", "var(--lumo-secondary-text-color)")
                        .set("text-align", "center");
                matchsLayout.add(plus);
            }
        }

        matchsCard.add(matchsLayout);

        Button fermer = new Button("Fermer", e -> dialog.close());
        fermer.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        fermer.addClassName("avatar-dialog-close-button");

        contenu.add(avatarGrand, nomEquipe, infoCard, titreMatchs, matchsCard, fermer);
        dialog.add(contenu);
        dialog.open();
    }

    private HorizontalLayout creerInfoRow(String label, String valeur) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        row.setAlignItems(FlexComponent.Alignment.CENTER);

        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle().set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)");

        Span valeurSpan = new Span(valeur);
        valeurSpan.getStyle().set("color", "var(--lumo-body-text-color)");

        row.add(labelSpan, valeurSpan);
        return row;
    }

    private String obtenirJoueursEquipe(int equipeId) {
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
        List<String> joueurs = new ArrayList<>();

        String sql = """
                SELECT j.prenom, j.nom
                FROM joueur j
                JOIN joueur_equipe je
                  ON je.joueur_id = j.id AND je.tournoi_id = j.tournoi_id
                WHERE je.equipe_id = ? AND je.tournoi_id = ?
                ORDER BY j.nom, j.prenom
                """;

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, equipeId);
            pst.setInt(2, tournoiId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    joueurs.add(rs.getString("prenom") + " " + rs.getString("nom"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Erreur de chargement";
        }

        if (joueurs.isEmpty()) return "Aucun joueur";
        // Pour éviter un dialog trop long
        if (joueurs.size() > 10) {
            return String.join(", ", joueurs.subList(0, 10)) + " ... (+" + (joueurs.size() - 10) + ")";
        }
        return String.join(", ", joueurs);
    }

    private List<MatchInfo> obtenirMatchsEquipe(int equipeId) {
        List<MatchInfo> matches = new ArrayList<>();
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

        String sql = """
            SELECT
                r.id,
                r.round_number,
                r.score_a,
                r.score_b,
                r.played,
                CASE
                    WHEN r.equipe_a_id = ? THEN COALESCE(eb.nom_equipe, 'Bye')
                    ELSE COALESCE(ea.nom_equipe, 'Bye')
                END AS adversaire,
                CASE
                    WHEN r.score_a IS NOT NULL AND r.score_b IS NOT NULL
                    THEN CONCAT(r.score_a, ' - ', r.score_b)
                    ELSE 'Score non défini'
                END AS score
            FROM rencontre r
            JOIN equipe ea ON r.equipe_a_id = ea.id AND ea.tournoi_id = ?
            LEFT JOIN equipe eb ON r.equipe_b_id = eb.id AND eb.tournoi_id = ?
            WHERE r.tournoi_id = ?
              AND (r.equipe_a_id = ? OR r.equipe_b_id = ?)
            ORDER BY r.round_number DESC, r.id DESC
            """;

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, equipeId);
            pst.setInt(2, tournoiId);
            pst.setInt(3, tournoiId);
            pst.setInt(4, tournoiId);
            pst.setInt(5, equipeId);
            pst.setInt(6, equipeId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String round = "Round " + rs.getInt("round_number");
                    boolean played = rs.getBoolean("played");
                    String statut = played ? "Terminé" : "À venir";

                    String adversaire = rs.getString("adversaire");
                    if (adversaire == null || adversaire.isBlank()) adversaire = "Équipe inconnue";

                    String score = rs.getString("score");
                    if (score == null || score.isBlank()) score = "Score non défini";

                    matches.add(new MatchInfo(adversaire, statut, score, round));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            matches.add(new MatchInfo("Erreur", "Erreur de chargement", "N/A", "N/A"));
        }

        return matches;
    }

    private static class MatchInfo {
        private final String adversaire;
        private final String statut;
        private final String score;
        private final String round;

        public MatchInfo(String adversaire, String statut, String score, String round) {
            this.adversaire = adversaire;
            this.statut = statut;
            this.score = score;
            this.round = round;
        }

        public String getAdversaire() { return adversaire; }
        public String getStatut() { return statut; }
        public String getScore() { return score; }
        public String getRound() { return round; }
    }
}
