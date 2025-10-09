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
        H2 titre = new H2("Page d'accueil");

        // Lien vers la page de connexion
        RouterLink lienConnexion = new RouterLink("Se connecter / S'inscrire", VueConnexion.class);
        lienConnexion.getStyle().set("font-size", "18px");
        lienConnexion.getStyle().set("color", "#2c3e50");

        // Lien vers la page d'ajout de joueur
        RouterLink lienJoueur = new RouterLink("Ajouter un joueur", VueJoueur.class);
        lienJoueur.getStyle().set("font-size", "18px");
        lienJoueur.getStyle().set("color", "#16a085");

        this.add(titre, lienConnexion, lienJoueur);
        this.setAlignItems(Alignment.CENTER);
        this.setSpacing(true);
    }

}
