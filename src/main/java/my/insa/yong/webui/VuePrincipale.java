package my.insa.yong.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import my.insa.yong.model.UserSession;
import my.insa.yong.webui.components.BaseLayout;

/**
 *
 * @author saancern
 */
@Route(value = "")
@PageTitle("Accueil")
public class VuePrincipale extends BaseLayout {

    public VuePrincipale() {
        // Wrapper avec gradient background pour centrer le contenu
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setSizeFull();
        wrapper.addClassName("app-container");
        wrapper.setJustifyContentMode(VerticalLayout.JustifyContentMode.CENTER);
        wrapper.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        
        // Container principal pour le contenu
        VerticalLayout container = new VerticalLayout();
        container.addClassName("form-container");
        container.addClassName("form-container-large");
        container.addClassName("fade-in");
        container.setAlignItems(Alignment.CENTER);
        container.setSpacing(true);
        
        if (UserSession.userConnected()) {
            // Contenu pour utilisateur connecté
            construireInterfaceUtilisateurConnecte(container);
        } else {
            // Contenu pour utilisateur non connecté
            construireInterfaceUtilisateurNonConnecte(container);
        }
        
        wrapper.add(container);
        
        // Ajouter le wrapper dans le AppLayout
        this.setContent(wrapper);
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
        if (UserSession.adminConnected()) {
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
            UserSession.logout();
            UserSession.clearCurrentTournoi();
            // Naviguer vers la page de connexion
            getUI().ifPresent(ui -> ui.navigate(VueConnexion.class));
        });

        // Layout pour les boutons
        HorizontalLayout layoutBoutons = new HorizontalLayout(boutonChangerUtilisateur);
        layoutBoutons.addClassName("button-group");

        container.add(titre, sousTitre, infoAcces, layoutBoutons);
    }

}
