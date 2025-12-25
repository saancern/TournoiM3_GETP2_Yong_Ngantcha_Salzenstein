package my.insa.yong.webui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import my.insa.yong.webui.components.BaseLayout;

@PageTitle("Classement")
@Route(value = "classement", layout = BaseLayout.class)
public class VueClassement extends VerticalLayout {

    private final Tabs mainTabs;
    private final Tab tabJoueurs;
    private final Tab tabEquipes;
    private final VerticalLayout mainContentLayout;
    
    // Pour la section Joueurs (copié de VueBut_alle)
    private final VueJoueursClassement vueJoueurs;
    
    // Pour la section Équipes
    private final VueEquipesClassement vueEquipes;

    public VueClassement() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("app-container");

        H2 titre = new H2("🏆 Classement");
        titre.addClassName("page-title");
        add(titre);

        // Onglets principaux
        mainTabs = new Tabs();
        tabJoueurs = new Tab("Meilleurs Joueurs");
        tabEquipes = new Tab("Meilleures Équipes");
        mainTabs.add(tabJoueurs, tabEquipes);
        mainTabs.setWidthFull();

        mainContentLayout = new VerticalLayout();
        mainContentLayout.setSizeFull();
        mainContentLayout.setPadding(false);

        // Créer les vues
        vueJoueurs = new VueJoueursClassement();
        vueEquipes = new VueEquipesClassement();

        // Gérer les changements d'onglets principaux
        mainTabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab() == tabJoueurs) {
                afficherJoueurs();
            } else {
                afficherEquipes();
            }
        });

        add(mainTabs, mainContentLayout);
        expand(mainContentLayout);

        // Afficher les joueurs par défaut
        afficherJoueurs();
    }

    private void afficherJoueurs() {
        mainContentLayout.removeAll();
        mainContentLayout.add(vueJoueurs);
    }

    private void afficherEquipes() {
        mainContentLayout.removeAll();
        mainContentLayout.add(vueEquipes);
    }
}
