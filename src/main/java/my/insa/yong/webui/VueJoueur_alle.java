package my.insa.yong.webui;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

import my.insa.yong.model.Joueur;
import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.components.BaseLayout;

@Route(value = "joueurs", layout = BaseLayout.class)
@PageTitle("Galerie des Joueurs")
@AnonymousAllowed
public class VueJoueur_alle extends VerticalLayout {

    private HorizontalLayout avatarContainer;
    private final List<Joueur> joueurs;
    private String currentSortBy = "Nom";
    private String currentOrder = "A-Z";

    // ====== NEW: cache des photos pour éviter une requête DB par image ======
    private final Map<Integer, PhotoData> photoCache = new HashMap<>();

    private static class PhotoData {
        final byte[] bytes;
        final String mime;
        final String filename;

        PhotoData(byte[] bytes, String mime, String filename) {
            this.bytes = bytes;
            this.mime = mime;
            this.filename = filename;
        }
    }
    // ======================================================================

    public VueJoueur_alle() {
        this.joueurs = new ArrayList<>();

        initializeLayout();
        chargerJoueurs();
        creerAvatars();
    }

    private void initializeLayout() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("avatar-gallery-container");

        // Titre principal
        H2 titre = new H2("Galerie des Joueurs");
        titre.addClassName("avatar-gallery-title");

        // Info du tournoi
        Span infoTournoi = new Span("Tournoi : " + UserSession.getCurrentTournoiName()
                + " (" + UserSession.getCurrentTournoiSport() + ")");
        infoTournoi.addClassName("avatar-gallery-info");

        // Container pour les avatars avec layout responsive (left to right, top to bottom)
        avatarContainer = new HorizontalLayout();
        avatarContainer.setWidthFull();
        avatarContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        avatarContainer.setAlignItems(FlexComponent.Alignment.START);
        avatarContainer.getStyle().set("flex-wrap", "wrap");
        avatarContainer.getStyle().set("gap", "20px");
        avatarContainer.addClassName("avatar-grid");

        // Layout horizontal pour les contrôles de tri
        HorizontalLayout sortLayout = new HorizontalLayout();
        sortLayout.setAlignItems(FlexComponent.Alignment.END);
        sortLayout.setSpacing(true);

        // ComboBox pour le critère de tri
        ComboBox<String> sortComboBox = new ComboBox<>("Trier par:");
        sortComboBox.setItems("Nom", "Âge", "Taille", "Sexe", "Prénom");
        sortComboBox.setValue("Nom"); // Valeur par défaut
        sortComboBox.setWidth("200px");
        sortComboBox.setAllowCustomValue(false);
        sortComboBox.setAllowedCharPattern("^$"); // empêche la saisie

        // ComboBox pour l'ordre de tri
        ComboBox<String> orderComboBox = new ComboBox<>("Ordre:");
        orderComboBox.setItems("A-Z", "Z-A");
        orderComboBox.setValue("A-Z"); // Valeur par défaut
        orderComboBox.setWidth("150px");
        orderComboBox.setAllowCustomValue(false);
        orderComboBox.setAllowedCharPattern("^$"); // empêche la saisie

        // Listener pour le changement de critère de tri
        sortComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                currentSortBy = event.getValue();
                chargerJoueurs(currentSortBy, currentOrder);
                rafraichirAvatars();
            }
        });

        // Listener pour le changement d'ordre
        orderComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                currentOrder = event.getValue();
                chargerJoueurs(currentSortBy, currentOrder);
                rafraichirAvatars();
            }
        });

        sortLayout.add(sortComboBox, orderComboBox);

        add(titre, infoTournoi, sortLayout, avatarContainer);
    }

    private void chargerJoueurs() {
        chargerJoueurs(currentSortBy, currentOrder);
    }

    private void chargerJoueurs(String sortBy, String order) {
        joueurs.clear(); // Vider la liste avant de recharger

        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
        try (Connection connection = ConnectionPool.getConnection()) {
            String direction = order.equals("A-Z") ? "ASC" : "DESC";
            String sortCriteria;

            switch (sortBy) {
                case "Nom":
                    // IMPORTANT: Dans la BDD, nom et prenom sont inversés !
                    sortCriteria = "prenom " + direction + ", nom " + direction;
                    break;
                case "Prénom":
                    // IMPORTANT: Dans la BDD, nom et prenom sont inversés !
                    sortCriteria = "nom " + direction + ", prenom " + direction;
                    break;
                case "Âge":
                    sortCriteria = "age " + direction + ", prenom ASC";
                    break;
                case "Taille":
                    sortCriteria = "taille " + direction + ", prenom ASC";
                    break;
                case "Sexe":
                    sortCriteria = "sexe " + direction + ", prenom ASC, nom ASC";
                    break;
                default:
                    sortCriteria = "prenom " + direction + ", nom " + direction;
                    break;
            }

            joueurs.addAll(Joueur.chargerJoueursPourTournoi(connection, tournoiId, sortCriteria));

            // ===== NEW: charger toutes les photos en une fois =====
            prechargerPhotos(tournoiId);
            // =====================================================

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===== NEW: précharge toutes les photos du tournoi dans photoCache =====
    private void prechargerPhotos(int tournoiId) {
        photoCache.clear();

        String sql = "SELECT id, photo, photo_mime, photo_nom FROM joueur "
                + "WHERE tournoi_id = ? AND photo IS NOT NULL";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, tournoiId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    byte[] bytes = rs.getBytes("photo");
                    String mime = rs.getString("photo_mime");
                    String nom = rs.getString("photo_nom");

                    if (bytes != null && bytes.length > 0) {
                        if (mime == null || mime.isBlank()) mime = "image/png";
                        if (nom == null || nom.isBlank()) nom = "joueur-" + id;

                        photoCache.put(id, new PhotoData(bytes, mime, nom));
                    }
                }
            }
        } catch (SQLException e) {
            // non bloquant
            e.printStackTrace();
        }
    }
    // =====================================================================

    private void creerAvatars() {
        for (Joueur joueur : joueurs) {
            VerticalLayout avatarCard = creerAvatarCard(joueur);
            avatarContainer.add(avatarCard);
        }
    }

    private void rafraichirAvatars() {
        avatarContainer.removeAll();
        creerAvatars();
    }

    private VerticalLayout creerAvatarCard(Joueur joueur) {
        VerticalLayout cardLayout = new VerticalLayout();
        cardLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        cardLayout.setPadding(true);
        cardLayout.setSpacing(true);
        cardLayout.addClassName("avatar-card");
        cardLayout.setWidth("180px");
        cardLayout.getStyle().set("flex-shrink", "0");

        // Avatar
        Avatar avatar = new Avatar();
        avatar.setName(joueur.getPrenom() + " " + joueur.getNom());
        avatar.setWidth("80px");
        avatar.setHeight("80px");
        avatar.setColorIndex(determinerColorIndex(joueur.getSexe()));

        // ===== NEW: photo à la place des initiales =====
        appliquerPhotoSiDisponible(avatar, joueur.getId());
        // ===============================================

        H3 nomJoueur = new H3(joueur.getPrenom() + " " + joueur.getNom());
        nomJoueur.addClassName("avatar-card-name");

        cardLayout.addClickListener(e -> ouvrirDetailJoueur(joueur));
        cardLayout.add(avatar, nomJoueur);

        return cardLayout;
    }

    // ===== NEW: branche la photo (BLOB) au composant Avatar =====
    private void appliquerPhotoSiDisponible(Avatar avatar, int joueurId) {
        PhotoData data = photoCache.get(joueurId);
        if (data == null) {
            // pas de photo => Vaadin affichera les initiales automatiquement
            return;
        }

        DownloadHandler handler = DownloadHandler.fromInputStream(event -> {
            return new DownloadResponse(
                    new ByteArrayInputStream(data.bytes),
                    data.filename,
                    data.mime,
                    data.bytes.length
            );
        }).inline(); // IMPORTANT : image inline, pas en téléchargement

        avatar.setImageHandler(handler);
    }
    // =============================================================

    private int determinerColorIndex(String sexe) {
        return switch (sexe != null ? sexe.toUpperCase() : "INCONNU") {
            case "H", "HOMME" -> 1;
            case "F", "FEMME" -> 2;
            default -> 3;
        };
    }

    private void ouvrirDetailJoueur(Joueur joueur) {
        Dialog dialog = new Dialog();
        dialog.addClassName("avatar-dialog");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        VerticalLayout contenu = new VerticalLayout();
        contenu.addClassName("avatar-dialog-content");

        // Avatar grand
        Avatar avatarGrand = new Avatar();
        avatarGrand.setName(joueur.getPrenom() + " " + joueur.getNom());
        avatarGrand.setWidth("120px");
        avatarGrand.setHeight("120px");
        avatarGrand.setColorIndex(determinerColorIndex(joueur.getSexe()));

        // ===== NEW: photo aussi dans le dialog =====
        appliquerPhotoSiDisponible(avatarGrand, joueur.getId());
        // ===========================================

        H2 nomComplet = new H2(joueur.getPrenom() + " " + joueur.getNom());
        nomComplet.addClassName("avatar-dialog-title");

        Card infoCard = new Card();
        infoCard.addClassName("avatar-dialog-info-card");

        VerticalLayout infoLayout = new VerticalLayout();
        infoLayout.setPadding(true);
        infoLayout.setSpacing(true);

        HorizontalLayout ageInfo = creerInfoRow("Âge", String.valueOf(joueur.getAge()));
        HorizontalLayout sexeInfo = creerInfoRow("Sexe", capitalizeFirst(joueur.getSexe()));

        String equipesText = obtenirEquipesDuJoueur(joueur.getId());
        HorizontalLayout equipesInfo = creerInfoRow("Équipes", equipesText);

        infoLayout.add(ageInfo, sexeInfo, equipesInfo);
        infoCard.add(infoLayout);

        H4 titreMatchs = new H4("Liste des matchs");
        titreMatchs.getStyle().set("margin", "20px 0 10px 0");

        List<MatchInfo> matches = obtenirMatchsDuJoueur(joueur.getId());

        Card matchsCard = new Card();
        matchsCard.addClassName("avatar-dialog-matches-card");
        matchsCard.setWidthFull();

        VerticalLayout matchsLayout = new VerticalLayout();
        matchsLayout.setPadding(true);
        matchsLayout.setSpacing(true);

        if (matches.isEmpty()) {
            Span aucunMatch = new Span("Aucun match trouvé");
            aucunMatch.getStyle().set("font-style", "italic")
                    .set("color", "var(--lumo-secondary-text-color)");
            matchsLayout.add(aucunMatch);
        } else {
            List<MatchInfo> matchsLimites = matches.subList(0, Math.min(matches.size(), 5));

            for (MatchInfo match : matchsLimites) {
                HorizontalLayout matchRow = new HorizontalLayout();
                matchRow.setWidthFull();
                matchRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
                matchRow.setAlignItems(FlexComponent.Alignment.CENTER);
                matchRow.getStyle().set("padding", "8px 0");

                VerticalLayout matchInfo = new VerticalLayout();
                matchInfo.setSpacing(false);
                matchInfo.setPadding(false);

                Span adversaire = new Span("vs " + match.getAdversaire());
                adversaire.getStyle().set("font-weight", "500");

                Span details = new Span(match.getDate() + " • " + match.getStatut());
                details.getStyle().set("font-size", "0.9em")
                        .set("color", "var(--lumo-secondary-text-color)");

                matchInfo.add(adversaire, details);

                Span score = new Span(match.getScore());
                score.getStyle().set("font-weight", "600")
                        .set("color", "var(--lumo-primary-text-color)");

                matchRow.add(matchInfo, score);
                matchsLayout.add(matchRow);
            }

            if (matches.size() > 5) {
                Span plusDeMatchs = new Span("... et " + (matches.size() - 5) + " autre(s) match(s)");
                plusDeMatchs.getStyle().set("font-size", "0.9em")
                        .set("color", "var(--lumo-secondary-text-color)")
                        .set("text-align", "center");
                matchsLayout.add(plusDeMatchs);
            }
        }

        matchsCard.add(matchsLayout);

        Button fermer = new Button("Fermer", e -> dialog.close());
        fermer.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        fermer.addClassName("avatar-dialog-close-button");

        contenu.add(avatarGrand, nomComplet, infoCard, titreMatchs, matchsCard, fermer);
        dialog.add(contenu);
        dialog.open();
    }

    private HorizontalLayout creerInfoRow(String label, String valeur) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        row.setAlignItems(FlexComponent.Alignment.CENTER);

        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle()
                .set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)");

        Span valeurSpan = new Span(valeur);
        valeurSpan.getStyle()
                .set("color", "var(--lumo-body-text-color)");

        row.add(labelSpan, valeurSpan);
        return row;
    }

    private String obtenirEquipesDuJoueur(int joueurId) {
        List<String> equipes = new ArrayList<>();
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
        String sql = "SELECT e.nom_equipe FROM equipe e " +
                "JOIN joueur_equipe je ON e.id = je.equipe_id " +
                "WHERE je.joueur_id = ? AND je.tournoi_id = ? AND e.tournoi_id = ?";

        try (Connection connection = ConnectionPool.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, joueurId);
            pstmt.setInt(2, tournoiId);
            pstmt.setInt(3, tournoiId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    equipes.add(rs.getString("nom_equipe"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Erreur de chargement";
        }

        return equipes.isEmpty() ? "Aucune équipe" : String.join(", ", equipes);
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return "Non spécifié";
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private List<MatchInfo> obtenirMatchsDuJoueur(int joueurId) {
        List<MatchInfo> matches = new ArrayList<>();

        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

        String sql = """
            SELECT DISTINCT
                r.id as match_id,
                r.score_a,
                r.score_b,
                r.played,
                r.round_number,
                CASE
                    WHEN je1.equipe_id = r.equipe_a_id THEN eb.nom_equipe
                    ELSE ea.nom_equipe
                END as adversaire,
                CASE
                    WHEN r.score_a IS NOT NULL AND r.score_b IS NOT NULL
                    THEN CONCAT(r.score_a, ' - ', r.score_b)
                    ELSE 'Score non défini'
                END as score
            FROM rencontre r
            JOIN equipe ea ON r.equipe_a_id = ea.id AND ea.tournoi_id = ?
            LEFT JOIN equipe eb ON r.equipe_b_id = eb.id AND eb.tournoi_id = ?
            JOIN joueur_equipe je1 ON (je1.equipe_id = r.equipe_a_id OR je1.equipe_id = r.equipe_b_id)
                AND je1.joueur_id = ? AND je1.tournoi_id = ?
            WHERE r.tournoi_id = ?
            ORDER BY r.round_number DESC, r.id DESC
            """;

        try (Connection connection = ConnectionPool.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, tournoiId);
            pstmt.setInt(2, tournoiId);
            pstmt.setInt(3, joueurId);
            pstmt.setInt(4, tournoiId);
            pstmt.setInt(5, tournoiId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String roundInfo = "Round " + rs.getInt("round_number");
                    boolean played = rs.getBoolean("played");
                    String statut = played ? "Terminé" : "À venir";
                    String adversaire = rs.getString("adversaire") != null ?
                            rs.getString("adversaire") : "Équipe inconnue";
                    String score = rs.getString("score") != null ?
                            rs.getString("score") : "Score non défini";

                    matches.add(new MatchInfo(adversaire, statut, score, roundInfo));
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
        private final String date;

        public MatchInfo(String adversaire, String statut, String score, String date) {
            this.adversaire = adversaire;
            this.statut = statut;
            this.score = score;
            this.date = date;
        }

        public String getAdversaire() { return adversaire; }
        public String getStatut() { return statut; }
        public String getScore() { return score; }
        public String getDate() { return date; }
    }
}
