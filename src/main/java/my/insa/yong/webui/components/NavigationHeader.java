package my.insa.yong.webui.components;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;

import my.insa.yong.model.UserSession;
import my.insa.yong.webui.VueEquipe;
import my.insa.yong.webui.VueJoueur;
import my.insa.yong.webui.VuePrincipale;

/**
 * Composant de navigation partagé pour toutes les pages
 * @author saancern
 */
public class NavigationHeader extends VerticalLayout {
    
    public NavigationHeader() {
        this.setSpacing(false);
        this.setPadding(false);
        this.setWidthFull();
        this.addClassName("nav-header");
        
        // Layout principal du header
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setSpacing(true);
        headerLayout.setPadding(true);
        headerLayout.addClassName("nav-header");
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        
        // Partie gauche - Logo et navigation principale
        HorizontalLayout leftSection = new HorizontalLayout();
        leftSection.setSpacing(true);
        leftSection.setAlignItems(Alignment.CENTER);
        
        // Logo/Titre de l'application
        H2 logo = new H2("Gestion de Tournoi");
        logo.addClassName("nav-title");
        logo.getStyle().set("margin", "0");
        logo.getStyle().set("color", "var(--white)");
        
        leftSection.add(logo);
        
        // Navigation principale (visible seulement si connecté)
        if (UserSession.userConnected()) {
            HorizontalLayout navLinks = new HorizontalLayout();
            navLinks.setSpacing(true);
            navLinks.addClassName("nav-links");
            navLinks.setAlignItems(Alignment.CENTER);
            
            // Lien Accueil
            RouterLink lienAccueil = new RouterLink("Accueil", VuePrincipale.class);
            lienAccueil.addClassName("nav-link");
            
            // Lien Joueur
            RouterLink lienJoueur = new RouterLink("Joueur", VueJoueur.class);
            lienJoueur.addClassName("nav-link");
            
            // Lien Équipe
            RouterLink lienEquipe = new RouterLink("Équipe", VueEquipe.class);
            lienEquipe.addClassName("nav-link");
            
            navLinks.add(lienAccueil, lienJoueur, lienEquipe);
            leftSection.add(navLinks);
        }
        
        // Partie droite - Actions utilisateur
        HorizontalLayout rightSection = new HorizontalLayout();
        rightSection.setSpacing(true);
        rightSection.setAlignItems(Alignment.CENTER);
        
        if (UserSession.userConnected()) {
            // Informations utilisateur
            String username = UserSession.getCurrentUsername();
            String role = UserSession.getCurrentUserRoleDisplay();
            
            HorizontalLayout userInfo = new HorizontalLayout();
            userInfo.setSpacing(false);
            userInfo.setAlignItems(Alignment.CENTER);
            
            H2 userLabel = new H2("Bonjour " + username + " (" + role + ")");
            userLabel.addClassName("nav-title");
            userLabel.getStyle().set("margin", "0");
            userLabel.getStyle().set("color", "var(--white)");
            userLabel.getStyle().set("font-size", "1rem");
            
            userInfo.add(userLabel);
            rightSection.add(userInfo);
        } 
        
        headerLayout.add(leftSection, rightSection);
        this.add(headerLayout);
    }
}