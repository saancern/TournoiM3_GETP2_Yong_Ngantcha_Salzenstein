package my.insa.yong.webui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

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
        
        H2 titre = new H2("Bienvenue dans l'application Tournoi");
        titre.addClassName("page-title");

        // Lien vers la page de connexion
        RouterLink lienConnexion = new RouterLink("Se connecter / S'inscrire", VueConnexion.class);
        lienConnexion.addClassName("nav-link");
        lienConnexion.getStyle().set("font-size", "18px");
        lienConnexion.getStyle().set("color", "var(--primary-color)");
        lienConnexion.getStyle().set("text-decoration", "none");
        lienConnexion.getStyle().set("padding", "12px 24px");
        lienConnexion.getStyle().set("border", "2px solid var(--primary-color)");
        lienConnexion.getStyle().set("border-radius", "6px");
        lienConnexion.getStyle().set("transition", "all 0.3s ease");

        // Lien vers la page d'ajout de joueur
        RouterLink lienJoueur = new RouterLink("Ajouter un joueur", VueJoueur.class);
        lienJoueur.addClassName("nav-link");
        lienJoueur.getStyle().set("font-size", "18px");
        lienJoueur.getStyle().set("color", "var(--success-color)");
        lienJoueur.getStyle().set("text-decoration", "none");
        lienJoueur.getStyle().set("padding", "12px 24px");
        lienJoueur.getStyle().set("border", "2px solid var(--success-color)");
        lienJoueur.getStyle().set("border-radius", "6px");
        lienJoueur.getStyle().set("transition", "all 0.3s ease");

        container.add(titre, lienConnexion, lienJoueur);
        this.add(container);
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.CENTER);
        this.setSizeFull();
    }

}
