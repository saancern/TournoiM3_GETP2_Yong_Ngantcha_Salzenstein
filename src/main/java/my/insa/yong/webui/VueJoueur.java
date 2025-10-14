package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import my.insa.yong.model.Joueur;
import my.insa.yong.utils.database.ConnectionSimpleSGBD;

@Route("joueur")
@PageTitle("Joueur")
public class VueJoueur extends VerticalLayout {

    private TextField prenomField;
    private TextField nomField;
    private NumberField ageField;
    private ComboBox<String> sexeField;
    private NumberField tailleField;
    private Button ajouterButton;
    private Button modifierButton;
    private Button supprimerButton;
    private Button submitButton;
    private ComboBox<Joueur> playerSelector;
    private Grid<Joueur> joueursGrid;
    private H2 titreForm;
    
    private enum OperationMode {
        AJOUTER, MODIFIER, SUPPRIMER
    }
    
    private OperationMode currentMode = OperationMode.AJOUTER;

    public VueJoueur() {
        this.addClassName("app-container");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Créer le layout principal horizontal (gauche/droite)
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(true);

        // Partie gauche - Formulaire avec opérations
        VerticalLayout leftPanel = new VerticalLayout();
        leftPanel.addClassName("form-container");
        leftPanel.addClassName("fade-in");
        leftPanel.setWidth("400px");
        leftPanel.setSpacing(true);
        leftPanel.setPadding(true);

        // Titre du formulaire (changera selon l'opération)
        titreForm = new H2("Détails du joueur");
        titreForm.addClassName("page-title");

        // Boutons d'opération en haut
        HorizontalLayout operationButtons = new HorizontalLayout();
        operationButtons.setWidthFull();
        operationButtons.setSpacing(true);

        ajouterButton = new Button("Ajouter");
        ajouterButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        ajouterButton.addClickListener(e -> setMode(OperationMode.AJOUTER));

        modifierButton = new Button("Modifier");
        modifierButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        modifierButton.addClickListener(e -> setMode(OperationMode.MODIFIER));

        supprimerButton = new Button("Supprimer");
        supprimerButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        supprimerButton.addClickListener(e -> setMode(OperationMode.SUPPRIMER));

        operationButtons.add(ajouterButton, modifierButton, supprimerButton);

        // Sélecteur de joueur (visible seulement pour Modifier/Supprimer)
        playerSelector = new ComboBox<>("Sélectionner un joueur");
        playerSelector.setItemLabelGenerator(joueur -> 
            String.format("%d - %s %s", joueur.getId(), joueur.getPrenom(), joueur.getNom()));
        playerSelector.setPlaceholder("Choisissez un joueur...");
        playerSelector.setWidthFull();
        playerSelector.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                remplirFormulaire(e.getValue());
            }
        });
        playerSelector.setVisible(false);

        // Champs du formulaire
        prenomField = new TextField("Prénom");
        prenomField.setPlaceholder("Entrez le prénom du joueur");
        prenomField.setRequired(true);
        prenomField.addClassName("form-field");
        prenomField.setWidthFull();

        nomField = new TextField("Nom");
        nomField.setPlaceholder("Entrez le nom du joueur");
        nomField.setRequired(true);
        nomField.addClassName("form-field");
        nomField.setWidthFull();

        ageField = new NumberField("Âge");
        ageField.setPlaceholder("Entrez l'âge du joueur");
        ageField.setRequired(true);
        ageField.setMin(1);
        ageField.setMax(150);
        ageField.addClassName("form-field");
        ageField.setWidthFull();

        sexeField = new ComboBox<>("Sexe");
        sexeField.setItems("H", "F");
        sexeField.setPlaceholder("Sélectionnez le sexe");
        sexeField.setRequired(true);
        sexeField.addClassName("form-field");
        sexeField.setWidthFull();

        tailleField = new NumberField("Taille (cm)");
        tailleField.setPlaceholder("Entrez la taille du joueur en cm");
        tailleField.setRequired(true);
        tailleField.setMin(50);
        tailleField.setMax(250);
        tailleField.addClassName("form-field");
        tailleField.setWidthFull();

        // Bouton de soumission (texte changera selon l'opération)
        submitButton = new Button("Valider");
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.addClassName("btn-primary");
        submitButton.addClickListener(e -> executerOperation());
        submitButton.setWidthFull();

        leftPanel.add(titreForm, operationButtons, playerSelector, prenomField, nomField, ageField, sexeField, tailleField, submitButton);

        // Partie droite - Tableau des joueurs
        VerticalLayout rightPanel = new VerticalLayout();
        rightPanel.setSizeFull();
        rightPanel.setSpacing(true);
        rightPanel.setPadding(true);

        H2 titreTable = new H2("Liste des joueurs");
        titreTable.addClassName("page-title");

        // Créer le tableau des joueurs
        joueursGrid = new Grid<>(Joueur.class, false);
        joueursGrid.addColumn(Joueur::getId).setHeader("ID").setWidth("80px").setFlexGrow(0);
        joueursGrid.addColumn(Joueur::getPrenom).setHeader("Prénom").setAutoWidth(true);
        joueursGrid.addColumn(Joueur::getNom).setHeader("Nom").setAutoWidth(true);
        joueursGrid.addColumn(Joueur::getAge).setHeader("Âge").setAutoWidth(true);
        joueursGrid.addColumn(Joueur::getSexe).setHeader("Sexe").setAutoWidth(true);
        joueursGrid.addColumn(joueur -> String.format("%.1f cm", joueur.getTaille()))
                   .setHeader("Taille").setAutoWidth(true);
        
        joueursGrid.setSizeFull();
        joueursGrid.addClassName("players-grid");

        rightPanel.add(titreTable, joueursGrid);

        // Ajouter les panels au layout principal
        mainLayout.add(leftPanel, rightPanel);
        mainLayout.setFlexGrow(0, leftPanel);  // Panel gauche taille fixe
        mainLayout.setFlexGrow(1, rightPanel); // Panel droit prend l'espace restant

        this.add(mainLayout);

        // Charger les joueurs au démarrage
        chargerJoueurs();
        
        // Initialiser en mode Ajouter
        setMode(OperationMode.AJOUTER);
    }

    /**
     * Change le mode d'opération (Ajouter, Modifier, Supprimer)
     */
    private void setMode(OperationMode mode) {
        this.currentMode = mode;
        
        switch (mode) {
            case AJOUTER:
                titreForm.setText("Détails du joueur");
                ajouterButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                modifierButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                modifierButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                supprimerButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                supprimerButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
                
                playerSelector.setVisible(false);
                prenomField.setReadOnly(false);
                nomField.setReadOnly(false);
                tailleField.setReadOnly(false);
                ageField.setReadOnly(false);
                sexeField.setReadOnly(false);
                viderFormulaire();
                break;
                
            case MODIFIER:
                titreForm.setText("Détails du joueur");
                ajouterButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                ajouterButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                modifierButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                supprimerButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                supprimerButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
                
                playerSelector.setVisible(true);
                prenomField.setReadOnly(false);
                nomField.setReadOnly(false);
                tailleField.setReadOnly(false);
                ageField.setReadOnly(false);
                sexeField.setReadOnly(false);
                viderFormulaire();
                chargerJoueursSelector();
                break;
                
            case SUPPRIMER:
                titreForm.setText("Détails du joueur");
                ajouterButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                ajouterButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                modifierButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                modifierButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                supprimerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                
                playerSelector.setVisible(true);
                prenomField.setReadOnly(true);
                nomField.setReadOnly(true);
                tailleField.setReadOnly(true);
                ageField.setReadOnly(true);
                sexeField.setReadOnly(true);
                submitButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                submitButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
                viderFormulaire();
                chargerJoueursSelector();
                break;
        }
    }

    /**
     * Remplit le formulaire avec les données du joueur sélectionné
     */
    private void remplirFormulaire(Joueur joueur) {
        prenomField.setValue(joueur.getPrenom());
        nomField.setValue(joueur.getNom());
        tailleField.setValue(joueur.getTaille());
        ageField.setValue((double) joueur.getAge());
        sexeField.setValue(joueur.getSexe());
    }

    /**
     * Vide le formulaire
     */
    private void viderFormulaire() {
        prenomField.clear();
        nomField.clear();
        tailleField.clear();
        playerSelector.clear();
        ageField.clear();
        sexeField.clear();
    }

    /**
     * Charge les joueurs dans le sélecteur
     */
    private void chargerJoueursSelector() {
        List<Joueur> joueurs = new ArrayList<>();
        
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
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
            
            playerSelector.setItems(joueurs);
            
        } catch (SQLException ex) {
            Notification.show("Erreur lors du chargement des joueurs : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Exécute l'opération selon le mode courant
     */
    private void executerOperation() {
        switch (currentMode) {
            case AJOUTER:
                ajouterJoueur();
                break;
            case MODIFIER:
                modifierJoueur();
                break;
            case SUPPRIMER:
                supprimerJoueur();
                break;
        }
    }

    /**
     * Ajoute un nouveau joueur
     */
    private void ajouterJoueur() {
        String prenom = prenomField.getValue().trim();
        String nom = nomField.getValue().trim();
        Double taille = tailleField.getValue();
        Double ageDouble = ageField.getValue();
        String sexe = sexeField.getValue();

        if (prenom.isEmpty() || nom.isEmpty() || taille == null || taille <= 0 || 
            ageDouble == null || ageDouble <= 0 || sexe == null || sexe.isEmpty()) {
            Notification.show("Veuillez remplir tous les champs correctement.", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        int age = ageDouble.intValue();

        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            String sql = "INSERT INTO joueur (prenom, nom, age, sexe, taille) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, prenom);
                pst.setString(2, nom);
                pst.setInt(3, age);
                pst.setString(4, sexe);
                pst.setDouble(5, taille);
                
                int rows = pst.executeUpdate();
                if (rows > 0) {
                    Notification.show("Joueur ajouté avec succès !", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    viderFormulaire();
                    chargerJoueurs();
                } else {
                    Notification.show("Échec de l'ajout du joueur.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        } catch (SQLException ex) {
            Notification.show("Erreur lors de l'ajout : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Modifie un joueur existant
     */
    private void modifierJoueur() {
        Joueur joueurSelectionne = playerSelector.getValue();
        if (joueurSelectionne == null) {
            Notification.show("Veuillez sélectionner un joueur à modifier.", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        String prenom = prenomField.getValue().trim();
        String nom = nomField.getValue().trim();
        Double taille = tailleField.getValue();
        Double ageDouble = ageField.getValue();
        String sexe = sexeField.getValue();

        if (prenom.isEmpty() || nom.isEmpty() || taille == null || taille <= 0 || 
            ageDouble == null || ageDouble <= 0 || sexe == null || sexe.isEmpty()) {
            Notification.show("Veuillez remplir tous les champs correctement.", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        int age = ageDouble.intValue();

        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            String sql = "UPDATE joueur SET prenom = ?, nom = ?, age = ?, sexe = ?, taille = ? WHERE id = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, prenom);
                pst.setString(2, nom);
                pst.setInt(3, age);
                pst.setString(4, sexe);
                pst.setDouble(5, taille);
                pst.setInt(6, joueurSelectionne.getId());
                
                int rows = pst.executeUpdate();
                if (rows > 0) {
                    Notification.show("Joueur modifié avec succès !", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    viderFormulaire();
                    chargerJoueurs();
                    chargerJoueursSelector();
                } else {
                    Notification.show("Échec de la modification du joueur.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        } catch (SQLException ex) {
            Notification.show("Erreur lors de la modification : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Supprime un joueur
     */
    private void supprimerJoueur() {
        Joueur joueurSelectionne = playerSelector.getValue();
        if (joueurSelectionne == null) {
            Notification.show("Veuillez sélectionner un joueur à supprimer.", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            String sql = "DELETE FROM joueur WHERE id = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, joueurSelectionne.getId());
                
                int rows = pst.executeUpdate();
                if (rows > 0) {
                    Notification.show("Joueur supprimé avec succès !", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    viderFormulaire();
                    chargerJoueurs();
                    chargerJoueursSelector();
                    // Revenir au mode Ajouter après suppression
                    setMode(OperationMode.AJOUTER);
                } else {
                    Notification.show("Échec de la suppression du joueur.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        } catch (SQLException ex) {
            Notification.show("Erreur lors de la suppression : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Charge tous les joueurs depuis la base de données et les affiche dans le tableau
     */
    private void chargerJoueurs() {
        List<Joueur> joueurs = new ArrayList<>();
        
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            String sql = "SELECT * FROM joueur ORDER BY nom, prenom";
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
            
            // Mettre à jour le tableau
            joueursGrid.setItems(joueurs);
            
        } catch (SQLException ex) {
            Notification notification = Notification.show("Erreur lors du chargement des joueurs : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
