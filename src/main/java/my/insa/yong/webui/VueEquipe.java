package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import my.insa.yong.model.Equipe;
import my.insa.yong.model.Joueur;
import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.components.BaseLayout;

@Route("equipe")
@PageTitle("Équipe")
public class VueEquipe extends BaseLayout {

    private TextField nomEquipeField;
    private DatePicker dateCreationField;
    private MultiSelectComboBox<Joueur> joueursComboBox;
    private Button ajouterButton;
    private Button modifierButton;
    private Button supprimerButton;
    private Button submitButton;
    private ComboBox<Equipe> equipeSelector;
    private Grid<Equipe> equipesGrid;
    private H2 titreForm;
    
    private enum OperationMode {
        AJOUTER, MODIFIER, SUPPRIMER
    }
    
    private OperationMode currentMode = OperationMode.AJOUTER;

    public VueEquipe() {
        // Wrapper avec gradient background
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setSizeFull();
        wrapper.addClassName("app-container");
        wrapper.setPadding(true);
        
        // Layout principal avec deux colonnes
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(true);

        // Partie gauche - Formulaire
        VerticalLayout leftPanel = new VerticalLayout();
        leftPanel.setWidth("40%");
        leftPanel.setSpacing(true);
        leftPanel.setPadding(true);
        leftPanel.addClassName("form-container");
        leftPanel.addClassName("fade-in");

        construireFormulaire(leftPanel);

        // Partie droite - Tableau des équipes
        VerticalLayout rightPanel = new VerticalLayout();
        rightPanel.setSizeFull();
        rightPanel.setSpacing(true);
        rightPanel.setPadding(true);

        construireTableau(rightPanel);

        mainLayout.add(leftPanel, rightPanel);
        wrapper.add(mainLayout);
        this.setContent(wrapper);

        // Charger les données initiales
        chargerEquipes();
        chargerJoueurs();
    }

    private void construireFormulaire(VerticalLayout leftPanel) {
        // Titre du formulaire
        titreForm = new H2("Ajouter une équipe");
        titreForm.addClassName("page-title");

        // Boutons d'opération
        HorizontalLayout operationButtons = new HorizontalLayout();
        operationButtons.setSpacing(true);

        ajouterButton = new Button("Ajouter");
        ajouterButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        ajouterButton.addClickListener(e -> setMode(OperationMode.AJOUTER));

        modifierButton = new Button("Modifier");
        modifierButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        modifierButton.addClickListener(e -> setMode(OperationMode.MODIFIER));

        supprimerButton = new Button("Supprimer");
        supprimerButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        supprimerButton.addClickListener(e -> setMode(OperationMode.SUPPRIMER));

        operationButtons.add(ajouterButton, modifierButton, supprimerButton);

        // Sélecteur d'équipe (visible seulement pour Modifier/Supprimer)
        equipeSelector = new ComboBox<>("Sélectionner une équipe");
        equipeSelector.setItemLabelGenerator(equipe -> 
            String.format("%d - %s (%s)", equipe.getId(), equipe.getNomEquipe(), equipe.getDateCreation()));
        equipeSelector.setPlaceholder("Choisissez une équipe...");
        equipeSelector.setWidthFull();
        equipeSelector.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                remplirFormulaire(e.getValue());
            }
        });
        equipeSelector.setVisible(false);

        // Champs du formulaire
        nomEquipeField = new TextField("Nom de l'équipe");
        nomEquipeField.setPlaceholder("Entrez le nom de l'équipe");
        nomEquipeField.setRequired(true);
        nomEquipeField.addClassName("form-field");
        nomEquipeField.setWidthFull();

        dateCreationField = new DatePicker("Date de création");
        dateCreationField.setPlaceholder("Sélectionnez la date de création");
        dateCreationField.setRequired(true);
        dateCreationField.addClassName("form-field");
        dateCreationField.setWidthFull();
        dateCreationField.setValue(LocalDate.now()); // Date par défaut

        // Sélection des joueurs
        joueursComboBox = new MultiSelectComboBox<>();
        joueursComboBox.setLabel("Sélectionner les joueurs");
        joueursComboBox.setItemLabelGenerator(joueur -> 
            String.format("%s %s (%d ans)", joueur.getPrenom(), joueur.getNom(), joueur.getAge()));
        joueursComboBox.addClassName("form-field");
        joueursComboBox.setWidthFull();

        // Bouton de soumission
        submitButton = new Button("Valider");
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.addClassName("btn-primary");
        submitButton.addClickListener(e -> executerOperation());
        submitButton.setWidthFull();

        leftPanel.add(titreForm, operationButtons, equipeSelector, nomEquipeField, 
                     dateCreationField, joueursComboBox, submitButton);
    }

    private void construireTableau(VerticalLayout rightPanel) {
        H2 titreTable = new H2("Liste des équipes");
        titreTable.addClassName("page-title");

        // Créer le tableau des équipes
        equipesGrid = new Grid<>(Equipe.class, false);
        equipesGrid.addColumn(Equipe::getId).setHeader("ID").setWidth("80px").setFlexGrow(0);
        equipesGrid.addColumn(Equipe::getNomEquipe).setHeader("Nom").setAutoWidth(true);
        equipesGrid.addColumn(Equipe::getDateCreation).setHeader("Date de création").setAutoWidth(true);
        equipesGrid.addColumn(equipe -> {
            List<Joueur> joueurs = chargerJoueursEquipe(equipe.getId());
            return joueurs.stream()
                    .map(j -> j.getPrenom() + " " + j.getNom())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("Aucun joueur");
        }).setHeader("Joueurs").setAutoWidth(true);
        
        equipesGrid.setSizeFull();
        equipesGrid.addClassName("players-grid");

        rightPanel.add(titreTable, equipesGrid);
    }

    private void setMode(OperationMode mode) {
        currentMode = mode;
        
        switch (mode) {
            case AJOUTER:
                titreForm.setText("Ajouter une équipe");
                equipeSelector.setVisible(false);
                submitButton.setText("Ajouter");
                viderFormulaire();
                break;
            case MODIFIER:
                titreForm.setText("Modifier une équipe");
                equipeSelector.setVisible(true);
                submitButton.setText("Modifier");
                chargerEquipesPourSelection();
                break;
            case SUPPRIMER:
                titreForm.setText("Supprimer une équipe");
                equipeSelector.setVisible(true);
                submitButton.setText("Supprimer");
                chargerEquipesPourSelection();
                break;
        }
    }

    private void remplirFormulaire(Equipe equipe) {
        nomEquipeField.setValue(equipe.getNomEquipe());
        dateCreationField.setValue(equipe.getDateCreation());
        
        // Charger les joueurs de l'équipe
        List<Joueur> joueursEquipe = chargerJoueursEquipe(equipe.getId());
        Set<Joueur> joueursSelectionnes = new HashSet<>(joueursEquipe);
        joueursComboBox.setValue(joueursSelectionnes);
    }

    private void viderFormulaire() {
        nomEquipeField.clear();
        dateCreationField.setValue(LocalDate.now());
        joueursComboBox.clear();
    }

    private void chargerEquipesPourSelection() {
        List<Equipe> equipes = new ArrayList<>();
        try (Connection con = ConnectionPool.getConnection()) {
            String sql = "SELECT id, nom_equipe, date_creation FROM equipe ORDER BY nom_equipe";
            try (PreparedStatement pst = con.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                
                while (rs.next()) {
                    Equipe equipe = new Equipe(
                        rs.getInt("id"),
                        rs.getString("nom_equipe"),
                        rs.getDate("date_creation").toLocalDate()
                    );
                    equipes.add(equipe);
                }
            }
            equipeSelector.setItems(equipes);
        } catch (SQLException ex) {
            Notification.show("Erreur lors du chargement des équipes : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void executerOperation() {
        switch (currentMode) {
            case AJOUTER:
                ajouterEquipe();
                break;
            case MODIFIER:
                modifierEquipe();
                break;
            case SUPPRIMER:
                supprimerEquipe();
                break;
        }
    }

    private void ajouterEquipe() {
        String nomEquipe = nomEquipeField.getValue().trim();
        LocalDate dateCreation = dateCreationField.getValue();
        Set<Joueur> joueursSelectionnes = joueursComboBox.getValue();

        if (nomEquipe.isEmpty() || dateCreation == null) {
            Notification.show("Veuillez remplir tous les champs correctement.", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        // Validation du nombre de joueurs selon le tournoi actuel
        int maxJoueurs = UserSession.getCurrentTournoiNombreJoueursParEquipe();
        if (joueursSelectionnes.size() > maxJoueurs) {
            Notification.show("Trop de joueurs sélectionnés. Maximum autorisé : " + maxJoueurs + " joueurs pour ce tournoi.", 
                             3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            con.setAutoCommit(false);
            
            // Créer une nouvelle équipe (id = -1)
            Equipe nouvelleEquipe = new Equipe(nomEquipe, dateCreation);
            
            // Sauvegarder en utilisant ClasseMiroir
            int equipeId = nouvelleEquipe.saveInDB(con);
            
            // Ajouter les joueurs à l'équipe
            if (!joueursSelectionnes.isEmpty()) {
                String sqlJoueur = "INSERT INTO joueur_equipe (joueur_id, equipe_id) VALUES (?, ?)";
                try (PreparedStatement pstJoueur = con.prepareStatement(sqlJoueur)) {
                    for (Joueur joueur : joueursSelectionnes) {
                        pstJoueur.setInt(1, joueur.getId());
                        pstJoueur.setInt(2, equipeId);
                        pstJoueur.addBatch();
                    }
                    pstJoueur.executeBatch();
                }
            }
            
            con.commit();
            
            Notification.show("Équipe ajoutée avec succès ! (ID: " + equipeId + ")", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            viderFormulaire();
            chargerEquipes();
            
        } catch (SQLException ex) {
            Notification.show("Erreur lors de l'ajout : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void modifierEquipe() {
        Equipe equipeSelectionnee = equipeSelector.getValue();
        if (equipeSelectionnee == null) {
            Notification.show("Veuillez sélectionner une équipe à modifier.", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        String nomEquipe = nomEquipeField.getValue().trim();
        LocalDate dateCreation = dateCreationField.getValue();
        Set<Joueur> joueursSelectionnes = joueursComboBox.getValue();

        if (nomEquipe.isEmpty() || dateCreation == null) {
            Notification.show("Veuillez remplir tous les champs correctement.", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        // Validation du nombre de joueurs selon le tournoi actuel
        int maxJoueurs = UserSession.getCurrentTournoiNombreJoueursParEquipe();
        if (joueursSelectionnes.size() > maxJoueurs) {
            Notification.show("Trop de joueurs sélectionnés. Maximum autorisé : " + maxJoueurs + " joueurs pour ce tournoi.", 
                             3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            con.setAutoCommit(false);
            
            // Modifier l'équipe
            String sql = "UPDATE equipe SET nom_equipe = ?, date_creation = ? WHERE id = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, nomEquipe);
                pst.setDate(2, Date.valueOf(dateCreation));
                pst.setInt(3, equipeSelectionnee.getId());
                
                int rows = pst.executeUpdate();
                
                if (rows > 0) {
                    // Supprimer les anciens joueurs de l'équipe
                    String sqlDelete = "DELETE FROM joueur_equipe WHERE equipe_id = ?";
                    try (PreparedStatement pstDelete = con.prepareStatement(sqlDelete)) {
                        pstDelete.setInt(1, equipeSelectionnee.getId());
                        pstDelete.executeUpdate();
                    }
                    
                    // Ajouter les nouveaux joueurs
                    if (!joueursSelectionnes.isEmpty()) {
                        String sqlJoueur = "INSERT INTO joueur_equipe (joueur_id, equipe_id) VALUES (?, ?)";
                        try (PreparedStatement pstJoueur = con.prepareStatement(sqlJoueur)) {
                            for (Joueur joueur : joueursSelectionnes) {
                                pstJoueur.setInt(1, joueur.getId());
                                pstJoueur.setInt(2, equipeSelectionnee.getId());
                                pstJoueur.addBatch();
                            }
                            pstJoueur.executeBatch();
                        }
                    }
                    
                    con.commit();
                    
                    Notification.show("Équipe modifiée avec succès !", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    chargerEquipes();
                    chargerEquipesPourSelection();
                } else {
                    con.rollback();
                    Notification.show("Échec de la modification de l'équipe.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
            
        } catch (SQLException ex) {
            Notification.show("Erreur lors de la modification : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void supprimerEquipe() {
        Equipe equipeSelectionnee = equipeSelector.getValue();
        if (equipeSelectionnee == null) {
            Notification.show("Veuillez sélectionner une équipe à supprimer.", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            con.setAutoCommit(false);
            
            // Supprimer d'abord les joueurs de l'équipe
            String sqlJoueurs = "DELETE FROM joueur_equipe WHERE equipe_id = ?";
            try (PreparedStatement pstJoueurs = con.prepareStatement(sqlJoueurs)) {
                pstJoueurs.setInt(1, equipeSelectionnee.getId());
                pstJoueurs.executeUpdate();
            }
            
            // Puis supprimer l'équipe
            String sql = "DELETE FROM equipe WHERE id = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, equipeSelectionnee.getId());
                
                int rows = pst.executeUpdate();
                if (rows > 0) {
                    con.commit();
                    Notification.show("Équipe supprimée avec succès !", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    viderFormulaire();
                    chargerEquipes();
                    chargerEquipesPourSelection();
                } else {
                    con.rollback();
                    Notification.show("Échec de la suppression de l'équipe.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
            
        } catch (SQLException ex) {
            Notification.show("Erreur lors de la suppression : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void chargerEquipes() {
        List<Equipe> equipes = new ArrayList<>();
        try (Connection con = ConnectionPool.getConnection()) {
            String sql = "SELECT * FROM equipe ORDER BY nom_equipe, date_creation";
            try (PreparedStatement pst = con.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                
                while (rs.next()) {
                    Equipe equipe = new Equipe(
                        rs.getInt("id"),
                        rs.getString("nom_equipe"),
                        rs.getDate("date_creation").toLocalDate()
                    );
                    equipes.add(equipe);
                }
            }
            equipesGrid.setItems(equipes);
        } catch (SQLException ex) {
            Notification.show("Erreur lors du chargement des équipes : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void chargerJoueurs() {
        List<Joueur> joueurs = new ArrayList<>();
        try (Connection con = ConnectionPool.getConnection()) {
            String sql = "SELECT id, prenom, nom, age, sexe, taille FROM joueur ORDER BY nom, prenom";
            try (PreparedStatement pst = con.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                
                while (rs.next()) {
                    Joueur joueur = new Joueur(
                        rs.getInt("id"),
                        rs.getString("prenom"),
                        rs.getString("nom"),
                        rs.getInt("age"),
                        rs.getString("sexe"),
                        rs.getDouble("taille")
                    );
                    joueurs.add(joueur);
                }
            }
            joueursComboBox.setItems(joueurs);
        } catch (SQLException ex) {
            Notification.show("Erreur lors du chargement des joueurs : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private List<Joueur> chargerJoueursEquipe(int equipeId) {
        List<Joueur> joueurs = new ArrayList<>();
        try (Connection con = ConnectionPool.getConnection()) {
            String sql = "SELECT j.id, j.prenom, j.nom, j.age, j.sexe, j.taille " +
                        "FROM joueur j " +
                        "INNER JOIN joueur_equipe je ON j.id = je.joueur_id " +
                        "WHERE je.equipe_id = ? " +
                        "ORDER BY j.nom, j.prenom";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, equipeId);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        Joueur joueur = new Joueur(
                            rs.getInt("id"),
                            rs.getString("prenom"),
                            rs.getString("nom"),
                            rs.getInt("age"),
                            rs.getString("sexe"),
                            rs.getDouble("taille")
                        );
                        joueurs.add(joueur);
                    }
                }
            }
        } catch (SQLException ex) {
            // Log error but don't show notification for this helper method
        }
        return joueurs;
    }
}
