package my.insa.yong.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import my.insa.yong.model.UserSession;

/**
 *
 * @author saancern
 */
@Route(value = "")
@PageTitle("Accueil")
public class VuePrincipale extends VerticalLayout {

    public VuePrincipale() {
        this.addClassName("app-container");
        this.addClassName("centered-layout");
        
        // Container principal
        VerticalLayout container = new VerticalLayout();
        container.addClassName("form-container");
        container.addClassName("form-container-large");
        container.addClassName("fade-in");
        container.setAlignItems(Alignment.CENTER);
        container.setSpacing(true);
        
        if (UserSession.isUserLoggedIn()) {
            // Contenu pour utilisateur connecté
            construireInterfaceUtilisateurConnecte(container);
        } else {
            // Contenu pour utilisateur non connecté
            construireInterfaceUtilisateurNonConnecte(container);
        }
        
        this.add(container);
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.CENTER);
        this.setSizeFull();
    }
    
    private void construireInterfaceUtilisateurNonConnecte(VerticalLayout container) {
        H1 titre = new H1("Bienvenue dans l'application Tournoi");
        titre.addClassName("page-title");

        H3 sousTitre = new H3("Veuillez vous connecter pour accéder à l'application");
        sousTitre.addClassName("page-subtitle");

        // Lien vers la page de connexion
        RouterLink lienConnexion = new RouterLink("Se connecter / S'inscrire", VueConnexion.class);
        lienConnexion.addClassName("link-button-primary");

        container.add(titre, sousTitre, lienConnexion);
    }
    
    private void construireInterfaceUtilisateurConnecte(VerticalLayout container) {
        // Titre personnalisé avec nom de l'utilisateur
        String username = UserSession.getCurrentUsername();
        H1 titre = new H1("Bienvenue " + username + " !");
        titre.addClassName("page-title");

        // Sous-titre avec le rôle de l'utilisateur
        String role = UserSession.getCurrentUserRoleDisplay();
        H3 sousTitre = new H3("Vous êtes connecté(e) avec les privilèges : " + role);
        sousTitre.addClassName("page-subtitle");
        
        // Informations sur le niveau d'accès
        H3 infoAcces = new H3();
        if (UserSession.isCurrentUserAdmin()) {
            infoAcces.setText("Mode Administrateur - Accès complet");
            infoAcces.addClassName("text-warning");
        } else {
            infoAcces.setText("Mode Utilisateur - Accès standard");
            infoAcces.addClassName("text-success");
        }
        infoAcces.addClassName("section-title");

        // Bouton pour changer d'utilisateur
        Button boutonChangerUtilisateur = new Button("Changer d'utilisateur");
        boutonChangerUtilisateur.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        boutonChangerUtilisateur.addClassName("btn-primary");
        boutonChangerUtilisateur.addClickListener(e -> {
            // Déconnecter l'utilisateur actuel
            UserSession.clearCurrentUser();
            // Rafraîchir la page pour afficher l'interface de connexion
            getUI().ifPresent(ui -> ui.getPage().reload());
        });

        // Lien vers la page d'ajout de joueur
        RouterLink lienJoueur = new RouterLink("Joueur", VueJoueur.class);
        lienJoueur.addClassName("link-button-success");
        
        // Layout pour les boutons
        HorizontalLayout layoutBoutons = new HorizontalLayout(boutonChangerUtilisateur, lienJoueur);
        layoutBoutons.addClassName("button-group");

        container.add(titre, sousTitre, infoAcces, layoutBoutons);
    }

}
