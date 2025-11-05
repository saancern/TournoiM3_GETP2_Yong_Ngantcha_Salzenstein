package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import my.insa.yong.model.GestionBdD;
import my.insa.yong.model.Parametre;
import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.components.BaseLayout;

/**
 *
 * @author saancern
 */
@Route(value = "parametres")
@PageTitle("Paramètres")
public class VueParametres extends BaseLayout {

    private TextField nomTournoiField;
    private TextField sportField;
    private NumberField nombreTerrainsField;
    private NumberField nombreJoueursParEquipeField;
    
    private Parametre parametreActuel;

    public VueParametres() {
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
        setContent(wrapper);
    }

    private void construireInterfaceUtilisateurConnecte(VerticalLayout container) {
        H1 title = new H1("Paramètres du Tournoi");
        title.addClassName("page-title");
        
        // Section des paramètres du tournoi
        VerticalLayout parametresSection = new VerticalLayout();
        parametresSection.setSpacing(true);
        parametresSection.setWidth("100%");
        
        H3 parametresTitle = new H3("Configuration du Tournoi");
        parametresTitle.addClassName("section-title");
        
        // Champ nom du tournoi
        nomTournoiField = new TextField("Nom du Tournoi");
        nomTournoiField.setWidth("100%");
        nomTournoiField.setPlaceholder("Entrez le nom du tournoi");
        nomTournoiField.addClassName("form-input");
        
        // Champ sport
        sportField = new TextField("Sport");
        sportField.setWidth("100%");
        sportField.setPlaceholder("Entrez le type de sport");
        sportField.addClassName("form-input");
        
        // Champ nombre de terrains
        nombreTerrainsField = new NumberField("Nombre de Terrains");
        nombreTerrainsField.setWidth("100%");
        nombreTerrainsField.setMin(1);
        nombreTerrainsField.setMax(20);
        nombreTerrainsField.setValue(1.0);
        nombreTerrainsField.setPlaceholder("Nombre de terrains disponibles");
        nombreTerrainsField.addClassName("form-input");
        
        // Champ nombre de joueurs par équipe
        nombreJoueursParEquipeField = new NumberField("Nombre de Joueurs par Équipe");
        nombreJoueursParEquipeField.setWidth("100%");
        nombreJoueursParEquipeField.setMin(1);
        nombreJoueursParEquipeField.setMax(15);
        nombreJoueursParEquipeField.setValue(5.0);
        nombreJoueursParEquipeField.setPlaceholder("Nombre de joueurs par équipe");
        nombreJoueursParEquipeField.addClassName("form-input");
        
        // Bouton pour sauvegarder les paramètres
        Button sauvegarderBtn = new Button("Sauvegarder les Paramètres");
        sauvegarderBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sauvegarderBtn.addClassName("btn-primary");
        sauvegarderBtn.addClickListener(e -> sauvegarderParametres());
        
        // Bouton pour créer un nouveau tournoi
        Button nouveauTournoiBtn = new Button("Créer un Nouveau Tournoi");
        nouveauTournoiBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        nouveauTournoiBtn.addClassName("btn-success");
        nouveauTournoiBtn.addClickListener(e -> creerNouveauTournoi());
        
        HorizontalLayout boutonsParametres = new HorizontalLayout(sauvegarderBtn, nouveauTournoiBtn);
        boutonsParametres.setSpacing(true);
        boutonsParametres.addClassName("button-group");
        
        parametresSection.add(parametresTitle, nomTournoiField, sportField, nombreTerrainsField, 
                             nombreJoueursParEquipeField, boutonsParametres);
        
        // Section de gestion des données
        VerticalLayout gestionSection = new VerticalLayout();
        gestionSection.setSpacing(true);
        gestionSection.setWidth("100%");
        
        H3 gestionTitle = new H3("Gestion des Données");
        gestionTitle.addClassName("section-title");
        
        // Bouton pour réinitialiser tout
        Button reinitialiserToutBtn = new Button("Réinitialiser Tout");
        reinitialiserToutBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        reinitialiserToutBtn.addClassName("btn-danger");
        reinitialiserToutBtn.addClickListener(e -> confirmerReinitializationTotale());
        
        // Bouton pour réinitialiser ce tournoi
        Button reinitialiserTournoiBtn = new Button("Réinitialiser ce Tournoi");
        reinitialiserTournoiBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        reinitialiserTournoiBtn.addClassName("btn-danger");
        reinitialiserTournoiBtn.addClickListener(e -> confirmerReinitializationTournoi());
        
        HorizontalLayout boutonsGestion = new HorizontalLayout(reinitialiserToutBtn, reinitialiserTournoiBtn);
        boutonsGestion.setSpacing(true);
        boutonsGestion.addClassName("button-group");
        
        gestionSection.add(gestionTitle, boutonsGestion);
        
        container.add(title, parametresSection, gestionSection);
        
        // Charger les paramètres existants
        chargerParametresExistants();
    }

    private void construireInterfaceUtilisateurNonConnecte(VerticalLayout container) {
        H1 title = new H1("Accès Restreint");
        title.addClassName("page-title");
        
        H3 message = new H3("Vous devez être connecté pour accéder aux paramètres.");
        message.addClassName("section-title");
        
        container.add(title, message);
    }

    private void chargerParametresExistants() {
        try (Connection con = ConnectionPool.getConnection()) {
            // Charger le tournoi actuel depuis la session (ou ID=1 par défaut)
            Integer currentTournoiId = UserSession.getCurrentTournoiId().orElse(1);
            parametreActuel = Parametre.getParametreParId(con, currentTournoiId);
            
            if (parametreActuel != null) {
                // Charger les valeurs dans l'interface
                nomTournoiField.setValue(parametreActuel.getNomTournoi());
                sportField.setValue(parametreActuel.getSport());
                nombreTerrainsField.setValue((double) parametreActuel.getNombreTerrains());
                nombreJoueursParEquipeField.setValue((double) parametreActuel.getNombreJoueursParEquipe());
                
                // Mettre à jour la session avec les informations du tournoi
                UserSession.setCurrentTournoi(parametreActuel.getId(), parametreActuel.getNomTournoi());
                
            } else {
                // Si le tournoi actuel n'existe pas, fallback vers ID=1
                parametreActuel = Parametre.getParametreParId(con, 1);
                if (parametreActuel != null) {
                    // Charger les valeurs depuis le tournoi par défaut
                    nomTournoiField.setValue(parametreActuel.getNomTournoi());
                    sportField.setValue(parametreActuel.getSport());
                    nombreTerrainsField.setValue((double) parametreActuel.getNombreTerrains());
                    nombreJoueursParEquipeField.setValue((double) parametreActuel.getNombreJoueursParEquipe());
                    
                    // Mettre à jour la session
                    UserSession.setCurrentTournoi(parametreActuel.getId(), parametreActuel.getNomTournoi());
                } else {
                    // Si aucun tournoi ID=1 n'existe, le créer
                    creerTournoiParDefautAvecId1(con);
                }
            }
            
        } catch (SQLException ex) {
            Notification.show("Erreur lors du chargement des paramètres: " + ex.getLocalizedMessage(),
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void sauvegarderParametres() {
        if (nomTournoiField.getValue().trim().isEmpty()) {
            Notification.show("Le nom du tournoi ne peut pas être vide",
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        if (sportField.getValue().trim().isEmpty()) {
            Notification.show("Le sport ne peut pas être vide",
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        if (nombreTerrainsField.getValue() == null || nombreTerrainsField.getValue() < 1) {
            Notification.show("Le nombre de terrains doit être au moins 1",
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        if (nombreJoueursParEquipeField.getValue() == null || nombreJoueursParEquipeField.getValue() < 1) {
            Notification.show("Le nombre de joueurs par équipe doit être au moins 1",
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        try (Connection con = ConnectionPool.getConnection()) {
            if (parametreActuel == null) {
                // Créer un nouveau paramètre
                parametreActuel = new Parametre();
            }
            
            // Mettre à jour les valeurs
            parametreActuel.setNomTournoi(nomTournoiField.getValue().trim());
            parametreActuel.setSport(sportField.getValue().trim());
            parametreActuel.setNombreTerrains(nombreTerrainsField.getValue().intValue());
            parametreActuel.setNombreJoueursParEquipe(nombreJoueursParEquipeField.getValue().intValue());
            
            // Sauvegarder
            parametreActuel.sauvegarderOuModifier(con);
            
            // Mettre à jour la session avec les nouvelles informations du tournoi
            UserSession.setCurrentTournoi(parametreActuel.getId(), parametreActuel.getNomTournoi());
            
            Notification.show("Paramètres sauvegardés avec succès!",
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
        } catch (SQLException ex) {
            Notification.show("Erreur lors de la sauvegarde: " + ex.getLocalizedMessage(),
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void creerNouveauTournoi() {
        if (nomTournoiField.getValue().trim().isEmpty()) {
            Notification.show("Le nom du tournoi ne peut pas être vide",
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        if (sportField.getValue().trim().isEmpty()) {
            Notification.show("Le sport ne peut pas être vide",
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        if (nombreTerrainsField.getValue() == null || nombreTerrainsField.getValue() < 1) {
            Notification.show("Le nombre de terrains doit être au moins 1",
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        if (nombreJoueursParEquipeField.getValue() == null || nombreJoueursParEquipeField.getValue() < 1) {
            Notification.show("Le nombre de joueurs par équipe doit être au moins 1",
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        try (Connection con = ConnectionPool.getConnection()) {
            // Créer un nouveau paramètre (nouvel ID)
            Parametre nouveauParametre = new Parametre(
                nomTournoiField.getValue().trim(),
                sportField.getValue().trim(),
                nombreTerrainsField.getValue().intValue(),
                nombreJoueursParEquipeField.getValue().intValue()
            );
            
            // Sauvegarder le nouveau tournoi
            nouveauParametre.saveInDB(con);
            
            // Créer les tables spécifiques au tournoi
            int tournoiId = nouveauParametre.getId();
            if (tournoiId > 1) { // Only create tournament-specific tables for new tournaments (not default)
                try {
                    GestionBdD.createTournamentTables(con, tournoiId);
                    System.out.println("Tables du tournoi " + tournoiId + " créées automatiquement.");
                } catch (SQLException ex) {
                    System.err.println("Erreur lors de la création des tables du tournoi " + tournoiId + ": " + ex.getMessage());
                    // Continue anyway - tournament is created, tables can be created later if needed
                }
            }
            
            // Mettre à jour le paramètre actuel
            parametreActuel = nouveauParametre;
            
            // Mettre à jour la session avec le nouveau tournoi
            UserSession.setCurrentTournoi(parametreActuel.getId(), parametreActuel.getNomTournoi());
            
            Notification.show("Nouveau tournoi créé avec succès! (ID: " + tournoiId + ")",
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
        } catch (SQLException ex) {
            Notification.show("Erreur lors de la création du nouveau tournoi: " + ex.getLocalizedMessage(),
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void confirmerReinitializationTotale() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmer la réinitialisation totale");
        dialog.setText("Êtes-vous sûr de vouloir réinitialiser TOUT le système ? Cela supprimera le schéma complet et le recréera !");
        dialog.setCancelable(true);
        dialog.setConfirmText("Réinitialiser tout");
        dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
        
        dialog.addConfirmListener(e -> reinitialiserTout());
        dialog.open();
    }

    private void confirmerReinitializationTournoi() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmer la réinitialisation du tournoi");
        dialog.setText("Êtes-vous sûr de vouloir réinitialiser ce tournoi ? Cela supprimera le schéma et le recréera avec des données fraîches !");
        dialog.setCancelable(true);
        dialog.setConfirmText("Réinitialiser ce tournoi");
        dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
        
        dialog.addConfirmListener(e -> reinitialiserTournoi());
        dialog.open();
    }

    private void reinitialiserTout() {
        try (Connection con = ConnectionPool.getConnection()) {
            // Supprimer le schéma complet
            GestionBdD.deleteSchema(con);
            
            // Recréer le schéma
            GestionBdD.creeSchema(con);
            
            Notification.show("Système entièrement réinitialisé avec succès!",
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            // Recharger les paramètres par défaut
            chargerParametresExistants();
            
        } catch (SQLException ex) {
            Notification.show("Erreur lors de la réinitialisation totale: " + ex.getLocalizedMessage(),
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    private void reinitialiserTournoi() {
        try (Connection con = ConnectionPool.getConnection()) {
            // Obtenir l'ID du tournoi actuel
            Integer currentTournoiId = UserSession.getCurrentTournoiId().orElse(1);
            
            if (currentTournoiId == 1) {
                // Pour le tournoi par défaut (ID=1), vider seulement les données, pas les tables
                String[] tables = {"but", "rencontre", "joueur_equipe", "equipe", "joueur"};
                
                for (String table : tables) {
                    String sql = "DELETE FROM " + table;
                    try (PreparedStatement pst = con.prepareStatement(sql)) {
                        pst.executeUpdate();
                        System.out.println("Données supprimées de la table: " + table);
                    }
                }
                System.out.println("Données du tournoi par défaut réinitialisées.");
                
            } else {
                // Pour les tournois spécifiques (ID > 1), supprimer les tables spécifiques
                GestionBdD.deleteTournamentTables(con, currentTournoiId);
                System.out.println("Tables du tournoi " + currentTournoiId + " supprimées.");
            }
            
            Notification.show("Tournoi réinitialisé avec succès!",
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            // Recharger les paramètres existants (qui doivent être préservés)
            chargerParametresExistants();
            
        } catch (SQLException ex) {
            Notification.show("Erreur lors de la réinitialisation du tournoi: " + ex.getLocalizedMessage(),
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    /**
     * Crée le tournoi par défaut avec ID=1
     */
    private void creerTournoiParDefautAvecId1(Connection con) throws SQLException {
        // Utiliser les valeurs spécifiées: ID=1, "Tournoi", "Foot", 10, 11
        String insertSql = "INSERT INTO tournoi (id, nom_tournoi, sport, nombre_terrains, nombre_joueurs_par_equipe) " +
                          "VALUES (1, 'Tournoi', 'Foot', 10, 11)";
        
        try (PreparedStatement pst = con.prepareStatement(insertSql)) {
            pst.executeUpdate();
            
            // Créer l'objet paramètre correspondant
            parametreActuel = new Parametre(1, "Tournoi", "Foot", 10, 11);
            
            // Charger les valeurs dans l'interface
            nomTournoiField.setValue(parametreActuel.getNomTournoi());
            sportField.setValue(parametreActuel.getSport());
            nombreTerrainsField.setValue((double) parametreActuel.getNombreTerrains());
            nombreJoueursParEquipeField.setValue((double) parametreActuel.getNombreJoueursParEquipe());
            
            // Mettre à jour la session
            UserSession.setCurrentTournoi(parametreActuel.getId(), parametreActuel.getNomTournoi());
            
            Notification.show("Tournoi par défaut créé avec succès!",
                             3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        }
    }
}
