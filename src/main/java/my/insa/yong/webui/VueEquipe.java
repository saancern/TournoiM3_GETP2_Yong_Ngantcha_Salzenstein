package my.insa.yong.webui;

import java.sql.Connection;
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
import my.insa.yong.model.Parametre;
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
        // Initialiser les champs pour éviter les NullPointerException
        nomEquipeField = new TextField("Nom de l'équipe");
        dateCreationField = new DatePicker("Date de création");
        joueursComboBox = new MultiSelectComboBox<>();
        equipeSelector = new ComboBox<>("Sélectionner une équipe");
        titreForm = new H2("Ajouter une équipe");
        
        // Wrapper avec gradient background
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setSizeFull();
        wrapper.addClassName("app-container");
        wrapper.setPadding(true);

        // Layout principal avec deux colonnes
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(true);

        // Partie gauche - Formulaire (seulement si admin)
        if (UserSession.adminConnected()) {
            VerticalLayout leftPanel = new VerticalLayout();
            leftPanel.setWidth("40%");
            leftPanel.setSpacing(true);
            leftPanel.setPadding(true);
            leftPanel.addClassName("form-container");
            leftPanel.addClassName("fade-in");

            construireFormulaire(leftPanel);
            mainLayout.add(leftPanel);
        }

        // Partie droite - Tableau des équipes
        VerticalLayout rightPanel = new VerticalLayout();
        rightPanel.setSizeFull();
        rightPanel.setSpacing(true);
        rightPanel.setPadding(true);

        construireTableau(rightPanel);

        // Si utilisateur normal, le tableau prend toute la largeur
        if (!UserSession.adminConnected()) {
            rightPanel.setWidth("100%");
        }

        mainLayout.add(rightPanel);
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
        equipeSelector.setItemLabelGenerator(equipe
                -> String.format("%d - %s (%s)", equipe.getId(), equipe.getNomEquipe(), equipe.getDateCreation()));
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
        joueursComboBox.setItemLabelGenerator(joueur
                -> String.format("%s %s (%d ans)", joueur.getPrenom(), joueur.getNom(), joueur.getAge()));
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
            case AJOUTER -> {
                titreForm.setText("Ajouter une équipe");
                equipeSelector.setVisible(false);
                submitButton.setText("Ajouter");
                viderFormulaire();
            }
            case MODIFIER -> {
                titreForm.setText("Modifier une équipe");
                equipeSelector.setVisible(true);
                submitButton.setText("Modifier");
                chargerEquipesPourSelection();
            }
            case SUPPRIMER -> {
                titreForm.setText("Supprimer une équipe");
                equipeSelector.setVisible(true);
                submitButton.setText("Supprimer");
                chargerEquipesPourSelection();
            }
        }
    }

    private void remplirFormulaire(Equipe equipe) {
        nomEquipeField.setValue(equipe.getNomEquipe());
        dateCreationField.setValue(equipe.getDateCreation());

        // Charger les joueurs de l'équipe
        List<Joueur> joueursEquipe = chargerJoueursEquipe(equipe.getId());
        Set<Joueur> joueursSelectionnes = new HashSet<>(joueursEquipe);
        
        // Recharger les joueurs disponibles pour la modification (inclut ceux de l'équipe actuelle)
        chargerJoueursDisponiblesPourModification(equipe.getId());
        
        joueursComboBox.setValue(joueursSelectionnes);
    }

    private void viderFormulaire() {
        nomEquipeField.clear();
        dateCreationField.setValue(LocalDate.now());
        joueursComboBox.clear();
    }

    private void chargerEquipesPourSelection() {
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            List<Equipe> equipes = Equipe.chargerEquipesPourTournoi(con, tournoiId);
            equipeSelector.setItems(equipes);
        } catch (SQLException ex) {
            Notification.show("Erreur lors du chargement des équipes : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void executerOperation() {
        switch (currentMode) {
            case AJOUTER -> ajouterEquipe();
            case MODIFIER -> modifierEquipe();
            case SUPPRIMER -> supprimerEquipe();
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

        // Validation du nombre minimum de joueurs selon le tournoi actuel
        int minJoueurs = UserSession.getCurrentTournoiNombreJoueursParEquipe();
        if (joueursSelectionnes == null || joueursSelectionnes.isEmpty() || joueursSelectionnes.size() < minJoueurs) {
            Notification.show("⚠️ ERREUR : Une équipe doit contenir au minimum " + minJoueurs + " joueurs. Vous en avez sélectionné " + (joueursSelectionnes == null ? 0 : joueursSelectionnes.size()) + ".",
                    4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {

            // --- 1. VÉRIFICATION DU QUOTA D'ÉQUIPES (Blocage à l'inscription) ---
            int maxEquipes = getMaxEquipesAutorisees(con); // Ex: 6 équipes
            
            // Récupère l'ID du tournoi actuel pour utiliser la bonne table d'équipes
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            int equipesActuelles = Equipe.compterEquipesPourTournoi(con, tournoiId);

            // BLOCAGE si le quota est atteint ou dépassé (Ex: 6 >= 6)
            if (equipesActuelles >= maxEquipes) {
                Notification.show("IMPOSSIBLE D'AJOUTER : La limite de " + maxEquipes + " équipes pour ce tournoi (" + (maxEquipes / 2) + " terrains) est atteinte.",
                        5000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            // --- FIN VÉRIFICATION DU QUOTA ---

            // Validation du nombre de joueurs par équipe (contrainte séparée)
            int maxJoueurs = UserSession.getCurrentTournoiNombreJoueursParEquipe();
            if (joueursSelectionnes.size() > maxJoueurs) {
                Notification.show("Trop de joueurs sélectionnés. Maximum autorisé : " + maxJoueurs + " joueurs pour ce tournoi.",
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            // --- 2. VÉRIFICATION : Aucun joueur ne doit être dans une autre équipe ---
            for (Joueur joueur : joueursSelectionnes) {
                if (Equipe.joueurDansAutreEquipe(con, joueur.getId(), tournoiId, -1)) {
                    Notification.show("❌ ERREUR : Un ou plusieurs joueurs sélectionnés sont déjà inscrits dans une autre équipe. " +
                            "Un joueur ne peut être dans qu'une seule équipe à la fois.",
                            5000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
            }
            // --- FIN VÉRIFICATION JOUEURS ---

            con.setAutoCommit(false); // Début de la transaction

            // Créer et sauvegarder l'équipe
            Equipe nouvelleEquipe = new Equipe(nomEquipe, dateCreation);
            int equipeId = nouvelleEquipe.saveInDB(con);

            // Ajouter les joueurs à l'équipe
            if (!joueursSelectionnes.isEmpty()) {
                Equipe.ajouterJoueursAEquipe(con, equipeId, new ArrayList<>(joueursSelectionnes), tournoiId);
            }

            con.commit(); // Validation de la transaction

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
        int minJoueurs = UserSession.getCurrentTournoiNombreJoueursParEquipe();
        if (joueursSelectionnes == null || joueursSelectionnes.isEmpty() || joueursSelectionnes.size() < minJoueurs) {
            Notification.show("⚠️ ERREUR : Une équipe doit contenir au minimum " + minJoueurs + " joueurs. Vous en avez sélectionné " + (joueursSelectionnes == null ? 0 : joueursSelectionnes.size()) + ".",
                    4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        // Validation du nombre maximum de joueurs selon le tournoi actuel
        int maxJoueurs = UserSession.getCurrentTournoiNombreJoueursParEquipe();
        if (joueursSelectionnes.size() > maxJoueurs) {
            Notification.show("Trop de joueurs sélectionnés. Maximum autorisé : " + maxJoueurs + " joueurs pour ce tournoi.",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            
            // --- VÉRIFICATION : Aucun joueur ne doit être dans une autre équipe (sauf celle-ci) ---
            for (Joueur joueur : joueursSelectionnes) {
                if (Equipe.joueurDansAutreEquipe(con, joueur.getId(), tournoiId, equipeSelectionnee.getId())) {
                    Notification.show("❌ ERREUR : Un ou plusieurs joueurs sélectionnés sont déjà inscrits dans une autre équipe. " +
                            "Un joueur ne peut être dans qu'une seule équipe à la fois.",
                            5000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
            }
            // --- FIN VÉRIFICATION JOUEURS ---
            
            con.setAutoCommit(false);

            // Modifier l'équipe
            equipeSelectionnee.setNomEquipe(nomEquipe);
            equipeSelectionnee.setDateCreation(dateCreation);
            Equipe.modifierEquipe(con, equipeSelectionnee, tournoiId);

            // Supprimer les anciens joueurs de l'équipe
            Equipe.supprimerJoueursEquipe(con, equipeSelectionnee.getId(), tournoiId);

            // Ajouter les nouveaux joueurs
            if (!joueursSelectionnes.isEmpty()) {
                Equipe.ajouterJoueursAEquipe(con, equipeSelectionnee.getId(), new ArrayList<>(joueursSelectionnes), tournoiId);
            }

            con.commit();

            Notification.show("Équipe modifiée avec succès !", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            chargerEquipes();
            chargerEquipesPourSelection();

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
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

            // Supprimer d'abord les joueurs de l'équipe
            Equipe.supprimerJoueursEquipe(con, equipeSelectionnee.getId(), tournoiId);

            // Puis supprimer l'équipe
            Equipe.supprimerEquipe(con, equipeSelectionnee.getId(), tournoiId);
            
            con.commit();
            
            Notification.show("Équipe supprimée avec succès !", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            viderFormulaire();
            chargerEquipes();
            chargerEquipesPourSelection();

        } catch (SQLException ex) {
            Notification.show("Erreur lors de la suppression : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void chargerEquipes() {
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            List<Equipe> equipes = Equipe.chargerEquipesPourTournoi(con, tournoiId);
            equipesGrid.setItems(equipes);
        } catch (SQLException ex) {
            Notification.show("Erreur lors du chargement des équipes : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void chargerJoueurs() {
        List<Joueur> joueurs = new ArrayList<>();
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            // Exclure les joueurs déjà assignés à une équipe
            String sql = "SELECT id, prenom, nom, age, sexe, taille FROM joueur " +
                         "WHERE tournoi_id=? AND id NOT IN " +
                         "(SELECT joueur_id FROM joueur_equipe WHERE tournoi_id=?) " +
                         "ORDER BY nom, prenom";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, tournoiId);
                pst.setInt(2, tournoiId);
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
            joueursComboBox.setItems(joueurs);
        } catch (SQLException ex) {
            Notification.show("Erreur lors du chargement des joueurs : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void chargerJoueursDisponiblesPourModification(int equipeId) {
        List<Joueur> joueurs = new ArrayList<>();
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            // Charger les joueurs non assignés OU ceux de l'équipe actuelle
            String sql = "SELECT id, prenom, nom, age, sexe, taille FROM joueur " +
                         "WHERE tournoi_id=? AND (id NOT IN " +
                         "(SELECT joueur_id FROM joueur_equipe WHERE tournoi_id=? AND equipe_id != ?) " +
                         "OR id IN (SELECT joueur_id FROM joueur_equipe WHERE tournoi_id=? AND equipe_id = ?)) " +
                         "ORDER BY nom, prenom";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, tournoiId);
                pst.setInt(2, tournoiId);
                pst.setInt(3, equipeId);
                pst.setInt(4, tournoiId);
                pst.setInt(5, equipeId);
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
            joueursComboBox.setItems(joueurs);
        } catch (SQLException ex) {
            Notification.show("Erreur lors du chargement des joueurs : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private List<Joueur> chargerJoueursEquipe(int equipeId) {
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            return Equipe.chargerJoueursEquipe(con, equipeId, tournoiId);
        } catch (SQLException ex) {
            // Log error but don't show notification for this helper method
        }
        return new ArrayList<>();
    }

    private int getMaxEquipesAutorisees(Connection con) throws SQLException {
        // Récupère l'ID du tournoi actuel (par défaut 1 si non défini)
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

        // Assurez-vous que l'import my.insa.yong.model.Parametre; est au début du fichier
        Parametre params = Parametre.getParametreById(con, tournoiId);

        if (params != null) {
            // La règle est : Nombre max d'équipes = Nombre de Terrains * 2 
            return params.getNombreTerrains() * 2;
        }
        // S'il y a un problème de lecture des paramètres (très rare si tout compile), pas de blocage.
        return Integer.MAX_VALUE;
    }
}
