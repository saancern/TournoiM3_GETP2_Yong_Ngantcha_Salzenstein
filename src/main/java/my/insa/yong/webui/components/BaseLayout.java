package my.insa.yong.webui.components;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;

import my.insa.yong.model.Parametre;
import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.VueEquipe;
import my.insa.yong.webui.VueJoueur;
import my.insa.yong.webui.VueJoueur_alle;
import my.insa.yong.webui.VueMatch;
import my.insa.yong.webui.VueParametres;
import my.insa.yong.webui.VuePrincipale;
import my.insa.yong.webui.VueTerrain;

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
        System.out.println("Creating header...");
        String tournoiName = UserSession.getCurrentTournoiName();
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
        System.out.println("Header tournament info - Name: " + tournoiName + ", ID: " + tournoiId);
        
        H1 appName = new H1("Gestion de " + tournoiName + " (ID: " + tournoiId + ")");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        
        HorizontalLayout header = new HorizontalLayout();
        
        boolean userConnected = UserSession.userConnected();
        System.out.println("User connected: " + userConnected);
        
        if (userConnected) {
            // Ajouter le toggle du drawer seulement si connecté
            DrawerToggle toggle = new DrawerToggle();
            toggle.setAriaLabel("Menu toggle");
            
            // Tournament switch button accessible to all users
            System.out.println("Creating tournament switch button...");
            Button tournoiButton = new Button("Changer Tournoi");
            tournoiButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            tournoiButton.addClickListener(e -> showTournamentSelectionDialog());
            
            System.out.println("Tournament switch button created");
            
            // Informations utilisateur dans le header
            String username = UserSession.getCurrentUsername();
            String role = UserSession.getCurrentUserRoleDisplay();
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
            
            System.out.println("Adding components to header: toggle, appName, tournoiButton");
            header.add(toggle, appName, tournoiButton);
            header.setFlexGrow(1, appName);
            System.out.println("Adding user section to header");
            header.add(userSection);
            
        } else {
            System.out.println("User not connected, creating simple header");
            // Si pas connecté, juste le titre
            header.add(appName);
            header.setFlexGrow(1, appName);
        }
        
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);
        
        System.out.println("Adding header to navbar");
        addToNavbar(header);
        System.out.println("Header creation completed");
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
        nav.addItem(new SideNavItem("Terrains", VueTerrain.class));
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
    
    private void showTournamentSelectionDialog() {
        System.out.println("Opening tournament selection dialog...");
        
        // Load available tournaments
        List<Parametre> tournois = loadAvailableTournaments();
        
        if (tournois.isEmpty()) {
            Notification.show("Aucun tournoi disponible", 3000, Notification.Position.MIDDLE);
            return;
        }
        
        // Create simple confirmation dialog
        ConfirmDialog selectionDialog = new ConfirmDialog();
        selectionDialog.setHeader("Changer de Tournoi");
        
        // Build tournament list text
        StringBuilder tournoiList = new StringBuilder("Tournois disponibles:\n\n");
        Integer currentId = UserSession.getCurrentTournoiId().orElse(null);
        
        for (int i = 0; i < tournois.size(); i++) {
            Parametre tournoi = tournois.get(i);
            String marker = (currentId != null && currentId == tournoi.getId()) ? "► ACTUEL: " : "   ";
            tournoiList.append(String.format("%s%s (%s)\n", 
                marker, tournoi.getNomTournoi(), tournoi.getSport()));
        }
        
        // If there's only one tournament or user wants to cycle to next
        Parametre nextTournoi = getNextTournament(tournois, currentId);
        
        if (nextTournoi != null) {
            selectionDialog.setText(tournoiList.toString() + 
                "\nBasculer vers: " + nextTournoi.getNomTournoi() + " (" + nextTournoi.getSport() + ")?");
            
            selectionDialog.setCancelable(true);
            selectionDialog.setConfirmText("Changer");
            selectionDialog.setCancelText("Annuler");
            
            // Handle confirmation
            selectionDialog.addConfirmListener(event -> {
                System.out.println("User confirmed switch to: " + nextTournoi.getNomTournoi());
                switchTournamentWithCompleteUpdate(nextTournoi);
            });
        } else {
            selectionDialog.setText(tournoiList.toString() + 
                "\nAucun autre tournoi disponible.");
            selectionDialog.setCancelable(true);
            selectionDialog.setConfirmText("OK");
        }
        
        selectionDialog.open();
    }
    
    private Parametre getNextTournament(List<Parametre> tournois, Integer currentId) {
        if (tournois.size() <= 1) {
            return null; // No other tournament available
        }
        
        // Find current tournament index
        int currentIndex = -1;
        for (int i = 0; i < tournois.size(); i++) {
            if (tournois.get(i).getId() == (currentId != null ? currentId : -1)) {
                currentIndex = i;
                break;
            }
        }
        
        // Return next tournament (cycle to first if at end)
        if (currentIndex >= 0) {
            int nextIndex = (currentIndex + 1) % tournois.size();
            return tournois.get(nextIndex);
        } else {
            // Current tournament not found, return first available
            return tournois.get(0);
        }
    }
    
    private List<Parametre> loadAvailableTournaments() {
        System.out.println("Loading available tournaments...");
        
        try (Connection con = ConnectionPool.getConnection()) {
            List<Parametre> tournois = Parametre.tousLesParametres(con);
            System.out.println("Total tournaments loaded: " + tournois.size());
            
            for (Parametre tournoi : tournois) {
                System.out.println("Available tournament: " + tournoi.getNomTournoi() + " (ID: " + tournoi.getId() + ")");
            }
            
            return tournois;
            
        } catch (SQLException ex) {
            System.err.println("Error loading tournaments: " + ex.getMessage());
            ex.printStackTrace();
            Notification.show("Erreur lors du chargement des tournois", 3000, Notification.Position.MIDDLE);
            return new ArrayList<>();
        }
    }
    
    private void switchTournamentWithCompleteUpdate(Parametre tournoi) {
        try {
            System.out.println("switchTournamentWithCompleteUpdate called with: " + tournoi.getNomTournoi());
            
            // Get current tournament for comparison
            Integer currentTournoiId = UserSession.getCurrentTournoiId().orElse(null);
            System.out.println("Current tournament ID: " + currentTournoiId + ", New tournament ID: " + tournoi.getId());
            
            // Only switch if it's actually different
            if (currentTournoiId == null || !currentTournoiId.equals(tournoi.getId())) {
                System.out.println("Tournament is different, switching...");
                
                // 1. Update session with new tournament (includes ID and name)
                UserSession.setCurrentTournoi(tournoi.getId(), tournoi.getNomTournoi());
                System.out.println("✓ Session updated with new tournament ID and name");
                
                // 2. Show notification with tournament info
                Notification.show(String.format(
                    "Tournoi changé vers: %s (%s) - Rechargement...", 
                    tournoi.getNomTournoi(), 
                    tournoi.getSport()
                ), 2500, Notification.Position.TOP_CENTER);
                
                // 3. Force complete page reload - this will:
                //    - Recreate the header with new tournament info (sport, name)
                //    - Reload all Vue classes with new tournament context
                //    - Update all database queries to use new tournament tables
                getUI().ifPresent(ui -> {
                    System.out.println("✓ Executing browser reload to update header and all queries...");
                    ui.getPage().executeJs("window.location.reload();");
                });
            } else {
                System.out.println("Tournament is the same, no switch needed");
            }
            
        } catch (Exception e) {
            System.err.println("Error switching tournament: " + e.getMessage());
            e.printStackTrace();
            Notification.show("Erreur lors du changement de tournoi", 3000, Notification.Position.MIDDLE);
        }
    }
}