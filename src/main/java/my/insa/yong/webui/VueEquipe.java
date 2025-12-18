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
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            String sql = "SELECT id, nom_equipe, date_creation FROM equipe WHERE tournoi_id=? ORDER BY nom_equipe";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, tournoiId);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        Equipe equipe = new Equipe(
                                rs.getInt("id"),
                                rs.getString("nom_equipe"),
                                rs.getDate("date_creation").toLocalDate()
                        );
                        equipes.add(equipe);
                    }
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
        if (joueursSelectionnes.size() < minJoueurs) {
            Notification.show("⚠️ ERREUR : Une équipe doit contenir au minimum " + minJoueurs + " joueurs. Vous en avez sélectionné " + joueursSelectionnes.size() + ".",
                    4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {

            // --- 1. VÉRIFICATION DU QUOTA D'ÉQUIPES (Blocage à l'inscription) ---
            int maxEquipes = getMaxEquipesAutorisees(con); // Ex: 6 équipes
            
            // Récupère l'ID du tournoi actuel pour utiliser la bonne table d'équipes
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

            String sqlCount = "SELECT COUNT(*) FROM equipe WHERE tournoi_id=?";
            int equipesActuelles;
            try (PreparedStatement pstCount = con.prepareStatement(sqlCount)) {
                pstCount.setInt(1, tournoiId);
                try (ResultSet rs = pstCount.executeQuery()) {
                    rs.next();
                    equipesActuelles = rs.getInt(1);
                }
            }

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
            String sqlCheckJoueurs = "SELECT joueur_id FROM joueur_equipe WHERE tournoi_id=? AND joueur_id IN (" +
                    String.join(",", joueursSelectionnes.stream().map(j -> String.valueOf(j.getId())).toList()) +
                    ")";
            
            if (!joueursSelectionnes.isEmpty()) {
                try (PreparedStatement pstCheck = con.prepareStatement(sqlCheckJoueurs)) {
                    pstCheck.setInt(1, tournoiId);
                    try (ResultSet rsCheck = pstCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            Notification.show("❌ ERREUR : Un ou plusieurs joueurs sélectionnés sont déjà inscrits dans une autre équipe. " +
                                    "Un joueur ne peut être dans qu'une seule équipe à la fois.",
                                    5000, Notification.Position.TOP_CENTER)
                                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                            return;
                        }
                    }
                }
            }
            // --- FIN VÉRIFICATION JOUEURS ---

            con.setAutoCommit(false); // Début de la transaction

            // Créer et sauvegarder l'équipe (Utilise la logique Multi-Tournoi pour déterminer la table)
            Equipe nouvelleEquipe = new Equipe(nomEquipe, dateCreation);
            int equipeId = nouvelleEquipe.saveInDB(con);

            // Ajouter les joueurs à l'équipe (Utilise la logique Multi-Tournoi pour déterminer la table)
            if (!joueursSelectionnes.isEmpty()) {
                String sqlJoueur = "INSERT INTO joueur_equipe (joueur_id, equipe_id, tournoi_id) VALUES (?, ?, ?)";

                try (PreparedStatement pstJoueur = con.prepareStatement(sqlJoueur)) {
                    for (Joueur joueur : joueursSelectionnes) {
                        pstJoueur.setInt(1, joueur.getId());
                        pstJoueur.setInt(2, equipeId);
                        pstJoueur.setInt(3, tournoiId);
                        pstJoueur.addBatch();
                    }
                    pstJoueur.executeBatch();
                }
            }

            con.commit(); // Validation de la transaction

            Notification.show("Équipe ajoutée avec succès ! (ID: " + equipeId + ")", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            viderFormulaire();
            chargerEquipes();

        } catch (SQLException ex) {
            // Gestion du rollback
            try {
                ConnectionPool.getConnection().rollback();
            } catch (SQLException ignore) {
            }

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
        if (joueursSelectionnes.size() < minJoueurs) {
            Notification.show("⚠️ ERREUR : Une équipe doit contenir au minimum " + minJoueurs + " joueurs. Vous en avez sélectionné " + joueursSelectionnes.size() + ".",
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
            String sqlCheckJoueurs = "SELECT joueur_id FROM joueur_equipe WHERE tournoi_id=? AND joueur_id IN (" +
                    String.join(",", joueursSelectionnes.stream().map(j -> String.valueOf(j.getId())).toList()) +
                    ") AND equipe_id != ?";
            
            if (!joueursSelectionnes.isEmpty()) {
                try (PreparedStatement pstCheck = con.prepareStatement(sqlCheckJoueurs)) {
                    pstCheck.setInt(1, tournoiId);
                    pstCheck.setInt(2, equipeSelectionnee.getId());
                    try (ResultSet rsCheck = pstCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            Notification.show("❌ ERREUR : Un ou plusieurs joueurs sélectionnés sont déjà inscrits dans une autre équipe. " +
                                    "Un joueur ne peut être dans qu'une seule équipe à la fois.",
                                    5000, Notification.Position.TOP_CENTER)
                                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                            return;
                        }
                    }
                }
            }
            // --- FIN VÉRIFICATION JOUEURS ---
            
            con.setAutoCommit(false);

            // Modifier l'équipe
            String sql = "UPDATE equipe SET nom_equipe = ?, date_creation = ? WHERE id = ? AND tournoi_id = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, nomEquipe);
                pst.setDate(2, Date.valueOf(dateCreation));
                pst.setInt(3, equipeSelectionnee.getId());
                pst.setInt(4, tournoiId);

                int rows = pst.executeUpdate();

                if (rows > 0) {
                    // Supprimer les anciens joueurs de l'équipe
                    String sqlDelete = "DELETE FROM joueur_equipe WHERE equipe_id = ? AND tournoi_id = ?";
                    try (PreparedStatement pstDelete = con.prepareStatement(sqlDelete)) {
                        pstDelete.setInt(1, equipeSelectionnee.getId());
                        pstDelete.setInt(2, tournoiId);
                        pstDelete.executeUpdate();
                    }

                    // Ajouter les nouveaux joueurs
                    if (!joueursSelectionnes.isEmpty()) {
                        String sqlJoueur = "INSERT INTO joueur_equipe (joueur_id, equipe_id, tournoi_id) VALUES (?, ?, ?)";
                        try (PreparedStatement pstJoueur = con.prepareStatement(sqlJoueur)) {
                            for (Joueur joueur : joueursSelectionnes) {
                                pstJoueur.setInt(1, joueur.getId());
                                pstJoueur.setInt(2, equipeSelectionnee.getId());
                                pstJoueur.setInt(3, tournoiId);
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

            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

            // Supprimer d'abord les joueurs de l'équipe
            String sqlJoueurs = "DELETE FROM joueur_equipe WHERE equipe_id = ? AND tournoi_id = ?";
            try (PreparedStatement pstJoueurs = con.prepareStatement(sqlJoueurs)) {
                pstJoueurs.setInt(1, equipeSelectionnee.getId());
                pstJoueurs.setInt(2, tournoiId);
                pstJoueurs.executeUpdate();
            }

            // Puis supprimer l'équipe
            String sql = "DELETE FROM equipe WHERE id = ? AND tournoi_id = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, equipeSelectionnee.getId());
                pst.setInt(2, tournoiId);

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
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            String sql = "SELECT * FROM equipe WHERE tournoi_id=? ORDER BY nom_equipe, date_creation";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, tournoiId);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        Equipe equipe = new Equipe(
                                rs.getInt("id"),
                                rs.getString("nom_equipe"),
                                rs.getDate("date_creation").toLocalDate()
                        );
                        equipes.add(equipe);
                    }
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
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            String sql = "SELECT id, prenom, nom, age, sexe, taille FROM joueur WHERE tournoi_id=? ORDER BY nom, prenom";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, tournoiId);
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
        List<Joueur> joueurs = new ArrayList<>();
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            String sql = "SELECT j.id, j.prenom, j.nom, j.age, j.sexe, j.taille "
                    + "FROM joueur j "
                    + "INNER JOIN joueur_equipe je ON j.id = je.joueur_id "
                    + "WHERE je.equipe_id = ? AND j.tournoi_id = ? AND je.tournoi_id = ? "
                    + "ORDER BY j.nom, j.prenom";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, equipeId);
                pst.setInt(2, tournoiId);
                pst.setInt(3, tournoiId);
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

    private int getMaxEquipesAutorisees(Connection con) throws SQLException {
        // Récupère l'ID du tournoi actuel (par défaut 1 si non défini)
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

        // Assurez-vous que l'import my.insa.yong.model.Parametre; est au début du fichier
        Parametre params = Parametre.getParametreParId(con, tournoiId);

        if (params != null) {
            // La règle est : Nombre max d'équipes = Nombre de Terrains * 2 
            return params.getNombreTerrains() * 2;
        }
        // S'il y a un problème de lecture des paramètres (très rare si tout compile), pas de blocage.
        return Integer.MAX_VALUE;
    }
}
