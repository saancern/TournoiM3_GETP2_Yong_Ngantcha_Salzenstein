package my.insa.yong.webui;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import my.insa.yong.model.Joueur;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.components.BaseLayout;
import my.insa.yong.model.UserSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

        add(titre, infoTournoi, avatarContainer);
    }

    private void chargerJoueurs() {
        String sql = "SELECT id, nom, prenom, age, sexe, taille FROM joueur ORDER BY nom, prenom";
        
        try (Connection connection = ConnectionPool.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                Joueur joueur = new Joueur(
                    rs.getInt("id"),
                    rs.getString("prenom"),
                    rs.getString("nom"),
                    rs.getInt("age"),
                    rs.getString("sexe"),
                    rs.getDouble("taille")
                );
                joueurs.add(joueur);
            }
            
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
        HorizontalLayout idInfo = creerInfoRow("ID", "#" + joueur.getId());
        HorizontalLayout sexeInfo = creerInfoRow("Sexe", capitalizeFirst(joueur.getSexe()));
        
        // Rechercher les équipes du joueur
        String equipesText = obtenirEquipesDuJoueur(joueur.getId());
        HorizontalLayout equipesInfo = creerInfoRow("Équipes", equipesText);

        infoLayout.add(idInfo, sexeInfo, equipesInfo);
        infoCard.add(infoLayout);

        // Bouton fermer
        Button fermer = new Button("Fermer", e -> dialog.close());
        fermer.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        fermer.addClassName("avatar-dialog-close-button");

        contenu.add(avatarGrand, nomComplet, infoCard, fermer);
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
        String sql = "SELECT e.nom_equipe FROM equipe e " +
                    "JOIN joueur_equipe je ON e.id = je.equipe_id " +
                    "WHERE je.joueur_id = ?";
        
        try (Connection connection = ConnectionPool.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, joueurId);
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
}