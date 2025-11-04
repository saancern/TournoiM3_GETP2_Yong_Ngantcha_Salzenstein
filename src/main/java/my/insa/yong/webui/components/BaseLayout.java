package my.insa.yong.webui.components;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;

import my.insa.yong.model.UserSession;
import my.insa.yong.webui.VueEquipe;
import my.insa.yong.webui.VueJoueur;
import my.insa.yong.webui.VueJoueur_alle;
import my.insa.yong.webui.VueMatch;
import my.insa.yong.webui.VueParametres;
import my.insa.yong.webui.VuePrincipale;

/**
 * Layout de base avec navigation utilisant AppLayout de Vaadin
 * @author saancern
 */
public class BaseLayout extends AppLayout {
    
    public BaseLayout() {
        // Ajouter la classe CSS pour le gradient background
        this.addClassName("app-container");
        
        createHeader();
        createDrawer();
    }
    
    private void createHeader() {
        String tournoiName = UserSession.getCurrentTournoiName();
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
        H1 appName = new H1("Gestion de " + tournoiName + " (ID: " + tournoiId + ")");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        
        HorizontalLayout header = new HorizontalLayout();
        
        if (UserSession.userConnected()) {
            // Ajouter le toggle du drawer seulement si connecté
            DrawerToggle toggle = new DrawerToggle();
            toggle.setAriaLabel("Menu toggle");
            
            // Informations utilisateur dans le header
            String username = UserSession.getCurrentUsername();
            String role = UserSession.getCurrentUserRoleDisplay();
            //String tournoi = UserSession.getCurrentTournoiName();
            String sport = UserSession.getCurrentTournoiSport();
            
            Span userInfo = new Span("Bonjour " + username + " (" + role + ") | Sport: " + sport);
            userInfo.addClassNames(LumoUtility.FontWeight.MEDIUM);
            
            // Bouton de déconnexion
            Button logoutBtn = new Button("Déconnexion", e -> {
                UserSession.logout();
                UserSession.clearCurrentTournoi(); // Effacer aussi le tournoi actuel
                getUI().ifPresent(ui -> ui.getPage().reload());
            });
            logoutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            
            HorizontalLayout userSection = new HorizontalLayout(userInfo, logoutBtn);
            userSection.setAlignItems(Alignment.CENTER);
            userSection.setSpacing(true);
            
            header.add(toggle, appName);
            header.setFlexGrow(1, appName);
            header.add(userSection);
            
        } else {
            // Si pas connecté, juste le titre
            header.add(appName);
            header.setFlexGrow(1, appName);
        }
        
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);
        
        addToNavbar(header);
    }
    
    private void createDrawer() {
        if (!UserSession.userConnected()) {
            return; // Pas de drawer si pas connecté
        }
        
        SideNav nav = new SideNav();
        
        // Navigation principale
        nav.addItem(new SideNavItem("Accueil", VuePrincipale.class));
        
        // Différents modes de vue joueurs selon le rôle
        if (UserSession.adminConnected()) {
            nav.addItem(new SideNavItem("Joueurs", VueJoueur.class));
        } else {
            nav.addItem(new SideNavItem("Joueurs", VueJoueur_alle.class));
        }
        
        nav.addItem(new SideNavItem("Équipes", VueEquipe.class));
        nav.addItem(new SideNavItem("Matchs", VueMatch.class));
        
        // Paramètres réservés aux administrateurs
        if (UserSession.adminConnected()) {
            nav.addItem(new SideNavItem("Paramètres", VueParametres.class));
        }
        
        // Informations sur le rôle de l'utilisateur
        H2 roleTitle = new H2("Privilèges");
        roleTitle.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.Margin.Top.LARGE, LumoUtility.Margin.Bottom.SMALL);
        
        Span roleInfo = new Span();
        if (UserSession.adminConnected()) {
            roleInfo.setText("Mode Administrateur - Accès complet");
            roleInfo.addClassNames(LumoUtility.TextColor.WARNING);
        } else {
            roleInfo.setText("Mode Utilisateur - Accès standard");
            roleInfo.addClassNames(LumoUtility.TextColor.SUCCESS);
        }
        
        Scroller scroller = new Scroller(nav);
        scroller.setClassName(LumoUtility.Padding.SMALL);
        
        addToDrawer(scroller);
    }
}