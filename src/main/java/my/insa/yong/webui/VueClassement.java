package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.components.BaseLayout;

@PageTitle("Classement")
@Route(value = "classement", layout = BaseLayout.class)
public class VueClassement extends VerticalLayout {

    private Tabs mainTabs;
    private Tab tabJoueurs;
    private Tab tabEquipes;
    private VerticalLayout mainContentLayout;
    
    // Pour la section Joueurs (copié de VueBut_alle)
    private VueJoueursClassement vueJoueurs;
    
    // Pour la section Équipes
    private VueEquipesClassement vueEquipes;

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
