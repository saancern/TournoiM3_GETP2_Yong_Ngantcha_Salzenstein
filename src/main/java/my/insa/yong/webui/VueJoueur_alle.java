package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

import my.insa.yong.model.Joueur;
import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.components.BaseLayout;

@Route(value = "joueurs", layout = BaseLayout.class)
@PageTitle("Galerie des Joueurs")
@AnonymousAllowed
public class VueJoueur_alle extends VerticalLayout {

    private final VerticalLayout avatarContainer;
    private final List<Joueur> joueurs;
    private static final int AVATARS_PER_ROW = 5; // Nombre d'avatars par ligne

    public VueJoueur_alle() {
        this.joueurs = new ArrayList<>();
        this.avatarContainer = new VerticalLayout();
        
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
        Span infoTournoi = new Span("Tournoi : " + UserSession.getCurrentTournoiName() + 
                                   " (" + UserSession.getCurrentTournoiSport() + ")");
        infoTournoi.addClassName("avatar-gallery-info");

        // Container pour les avatars avec layout responsive (left to right, top to bottom)
        avatarContainer.addClassName("avatar-grid");
        
        // ComboBox pour le tri
        ComboBox<String> sortComboBox = new ComboBox<>("Trier par:");
        sortComboBox.setItems("Nom", "Âge", "Taille", "Sexe", "Prénom");
        sortComboBox.setValue("Nom"); // Valeur par défaut
        sortComboBox.setWidth("200px");
        
        // Listener pour le changement de tri
        sortComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                chargerJoueurs(event.getValue());
                rafraichirAvatars();
            }
        });
        
        add(titre, infoTournoi, sortComboBox, avatarContainer);
    }

    private void chargerJoueurs() {
        chargerJoueurs("Nom"); // Tri par défaut
    }
    
    private void chargerJoueurs(String sortBy) {
        joueurs.clear(); // Vider la liste avant de recharger
        
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
        try (Connection connection = ConnectionPool.getConnection()) {
            joueurs.addAll(Joueur.chargerJoueursPourTournoi(connection, tournoiId, sortBy));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void creerAvatars() {
        // Créer les lignes horizontales pour organiser les avatars de gauche à droite, de haut en bas
        HorizontalLayout currentRow = new HorizontalLayout(); // Initialiser dès le début
        currentRow.setWidthFull();
        currentRow.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        currentRow.setAlignItems(FlexComponent.Alignment.CENTER);
        currentRow.setSpacing(true);
        currentRow.getStyle().set("flex-wrap", "wrap");
        currentRow.addClassName("avatar-row");
        avatarContainer.add(currentRow);
        
        int compteur = 0;
        
        for (Joueur joueur : joueurs) {
            // Créer une nouvelle ligne si on a atteint AVATARS_PER_ROW éléments
            if (compteur > 0 && compteur % AVATARS_PER_ROW == 0) {
                currentRow = new HorizontalLayout();
                currentRow.setWidthFull();
                currentRow.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
                currentRow.setAlignItems(FlexComponent.Alignment.CENTER);
                currentRow.setSpacing(true);
                currentRow.getStyle().set("flex-wrap", "wrap");
                currentRow.addClassName("avatar-row");
                
                avatarContainer.add(currentRow);
            }
            
            // Créer et ajouter l'avatar card
            VerticalLayout avatarCard = creerAvatarCard(joueur);
            currentRow.add(avatarCard);
            
            compteur++;
        }
    }

    private void rafraichirAvatars() {
        // Vider le container des avatars
        avatarContainer.removeAll();
        
        // Recréer les avatars avec les nouvelles données triées
        creerAvatars();
    }

    private VerticalLayout creerAvatarCard(Joueur joueur) {
        // Container principal simple et clean
        VerticalLayout cardLayout = new VerticalLayout();
        cardLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        cardLayout.setPadding(true);
        cardLayout.setSpacing(true);
        cardLayout.addClassName("avatar-card");
        cardLayout.setWidth("180px"); // Largeur fixe pour uniformité
        cardLayout.getStyle().set("flex-shrink", "0"); // Empêche le rétrécissement

        // Avatar plus grand et coloré
        Avatar avatar = new Avatar();
        avatar.setName(joueur.getPrenom() + " " + joueur.getNom());
        avatar.setWidth("80px");
        avatar.setHeight("80px");
        avatar.setColorIndex(determinerColorIndex(joueur.getSexe()));

        // Nom du joueur
        H3 nomJoueur = new H3(joueur.getPrenom() + " " + joueur.getNom());
        nomJoueur.addClassName("avatar-card-name");

        // Click listener pour ouvrir les détails
        cardLayout.addClickListener(e -> ouvrirDetailJoueur(joueur));

        cardLayout.add(avatar, nomJoueur);
        
        return cardLayout;
    }

    private int determinerColorIndex(String sexe) {
        switch (sexe != null ? sexe.toUpperCase() : "INCONNU") {
            case "H":
            case "HOMME":
                return 1; // Bleu
            case "F":
            case "FEMME":
                return 2; // Rose/Rouge
            default:
                return 3; // Vert
        }
    }

    private void ouvrirDetailJoueur(Joueur joueur) {
        Dialog dialog = new Dialog();
        dialog.addClassName("avatar-dialog");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        // Contenu du dialog
        VerticalLayout contenu = new VerticalLayout();
        contenu.addClassName("avatar-dialog-content");

        // Avatar plus grand
        Avatar avatarGrand = new Avatar();
        avatarGrand.setName(joueur.getPrenom() + " " + joueur.getNom());
        avatarGrand.setWidth("120px");
        avatarGrand.setHeight("120px");
        avatarGrand.setColorIndex(determinerColorIndex(joueur.getSexe()));

        // Informations détaillées
        H2 nomComplet = new H2(joueur.getPrenom() + " " + joueur.getNom());
        nomComplet.addClassName("avatar-dialog-title");

        // Container avec informations (utilise Card)
        Card infoCard = new Card();
        infoCard.addClassName("avatar-dialog-info-card");
        
        VerticalLayout infoLayout = new VerticalLayout();
        infoLayout.setPadding(true);
        infoLayout.setSpacing(true);

        // Informations du joueur
        HorizontalLayout ageInfo = creerInfoRow("Âge", String.valueOf(joueur.getAge()));
        HorizontalLayout sexeInfo = creerInfoRow("Sexe", capitalizeFirst(joueur.getSexe()));
        
        // Rechercher les équipes du joueur
        String equipesText = obtenirEquipesDuJoueur(joueur.getId());
        HorizontalLayout equipesInfo = creerInfoRow("Équipes", equipesText);

        infoLayout.add(ageInfo, sexeInfo, equipesInfo);
        infoCard.add(infoLayout);

        // Section des matchs
        H4 titreMatchs = new H4("Liste des matchs");
        titreMatchs.getStyle().set("margin", "20px 0 10px 0");
        
        // Récupérer les matchs du joueur
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
            // Limiter à 5 matchs pour éviter que le dialog soit trop grand
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

        // Bouton fermer
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
        
        // Using unified schema with tournoi_id column
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
            // Ajouter un match d'erreur pour informer l'utilisateur
            matches.add(new MatchInfo("Erreur", "Erreur de chargement", "N/A", "N/A"));
        }
        
        return matches;
    }

    // Classe interne pour représenter les données d'un match
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