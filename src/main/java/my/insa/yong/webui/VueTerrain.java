package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import my.insa.yong.model.Terrain;
import my.insa.yong.model.TerrainClassement;
import my.insa.yong.model.TerrainClassement.MatchInfo;
import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.components.BaseLayout;

@Route("terrain")
@PageTitle("Terrains")
public class VueTerrain extends BaseLayout {

    private TextField nomTerrainField;
    private IntegerField numeroField;
    private ComboBox<Terrain> terrainSelector;
    private Button ajouterButton;
    private Button modifierButton;
    private Button supprimerButton;
    private Button submitButton;
    private Grid<Terrain> terrainsGrid;
    private H2 titreForm;

    private Grid<MatchInfo> matchsGrid;
    private ComboBox<Terrain> matchTerrainSelector;
    private Button lierButton;

    private Grid<MatchInfo> terrainsMatchsGrid;

    private enum OperationMode {
        AJOUTER, MODIFIER, SUPPRIMER
    }

    private OperationMode currentMode = OperationMode.AJOUTER;

    public VueTerrain() {
        // Initialiser les champs
        nomTerrainField = new TextField("Nom du terrain");
        numeroField = new IntegerField("Numéro du terrain");
        terrainSelector = new ComboBox<>("Sélectionner un terrain");
        titreForm = new H2("Ajouter un terrain");

        // Wrapper avec background
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setSizeFull();
        wrapper.addClassName("app-container");
        wrapper.setPadding(true);

        // Layout principal
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(true);

        // Partie gauche - Formulaire (Admin only)
        if (UserSession.adminConnected()) {
            VerticalLayout leftPanel = new VerticalLayout();
            leftPanel.setWidth("40%");
            leftPanel.setSpacing(true);
            leftPanel.setPadding(true);
            leftPanel.addClassName("form-container");
            leftPanel.addClassName("fade-in");

            construireFormulaire(leftPanel);
            construireLiaisonMatchs(leftPanel);
            mainLayout.add(leftPanel);
        }

        // Partie droite - Tableau des terrains
        VerticalLayout rightPanel = new VerticalLayout();
        rightPanel.setSizeFull();
        rightPanel.setSpacing(true);
        rightPanel.setPadding(true);

        construireTableau(rightPanel);

        if (!UserSession.adminConnected()) {
            rightPanel.setWidth("100%");
        }

        mainLayout.add(rightPanel);
        wrapper.add(mainLayout);
        this.setContent(wrapper);

        // Charger les données initiales
        chargerTerrains();
        chargerMatchs();
    }

    private void construireFormulaire(VerticalLayout leftPanel) {
        titreForm = new H2("Ajouter un terrain");
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

        // Sélecteur de terrain
        terrainSelector = new ComboBox<>("Sélectionner un terrain");
        terrainSelector.setItemLabelGenerator(terrain
                -> String.format("Terrain %d - %s", terrain.getNumero(), terrain.getNomTerrain()));
        terrainSelector.setPlaceholder("Choisissez un terrain...");
        terrainSelector.setWidthFull();
        terrainSelector.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                remplirFormulaire(e.getValue());
            }
        });
        terrainSelector.setVisible(false);

        // Champs du formulaire
        nomTerrainField = new TextField("Nom du terrain");
        nomTerrainField.setPlaceholder("Ex: Terrain Central");
        nomTerrainField.setRequired(true);
        nomTerrainField.setWidthFull();

        numeroField = new IntegerField("Numéro du terrain");
        numeroField.setPlaceholder("Ex: 1");
        numeroField.setRequired(true);
        numeroField.setMin(1);
        numeroField.setWidthFull();

        // Bouton de soumission
        submitButton = new Button("Ajouter");
        submitButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        submitButton.setWidthFull();
        submitButton.addClickListener(e -> {
            if (validerFormulaire()) {
                switch (currentMode) {
                    case AJOUTER -> ajouterTerrain();
                    case MODIFIER -> modifierTerrain();
                    case SUPPRIMER -> supprimerTerrain();
                }
            }
        });

        leftPanel.add(
                titreForm,
                operationButtons,
                terrainSelector,
                nomTerrainField,
                numeroField,
                submitButton
        );
    }

    private void construireLiaisonMatchs(VerticalLayout leftPanel) {
        H3 titreLiaison = new H3("Assigner terrain à un match");
        titreLiaison.addClassName("page-title");

        matchTerrainSelector = new ComboBox<>("Sélectionner un terrain");
        matchTerrainSelector.setItemLabelGenerator(terrain
                -> String.format("Terrain %d - %s", terrain.getNumero(), terrain.getNomTerrain()));
        matchTerrainSelector.setPlaceholder("Terrain à assigner...");
        matchTerrainSelector.setWidthFull();

        // Grid des matchs sans terrain assigné
        matchsGrid = new Grid<>(MatchInfo.class, false);
        matchsGrid.setHeight("250px");
        matchsGrid.addColumn(m -> String.format("Match %d", m.getId())).setHeader("ID");
        matchsGrid.addColumn(MatchInfo::toString).setHeader("Match");
        matchsGrid.addColumn(m -> m.getTerrainNom() != null ? m.getTerrainNom() : "Non assigné").setHeader("Terrain actuel");

        lierButton = new Button("Assigner le terrain");
        lierButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        lierButton.setWidthFull();
        lierButton.addClickListener(e -> assignerTerrainAuMatch());

        leftPanel.add(
                titreLiaison,
                matchTerrainSelector,
                matchsGrid,
                lierButton
        );
    }

    private void construireTableau(VerticalLayout rightPanel) {
        H2 titreTableau = new H2("Terrains");
        titreTableau.addClassName("page-title");

        terrainsGrid = new Grid<>(Terrain.class, false);
        terrainsGrid.setHeight("300px");
        terrainsGrid.addColumn(Terrain::getId).setHeader("ID").setWidth("10%");
        terrainsGrid.addColumn(Terrain::getNumero).setHeader("Numéro").setWidth("15%");
        terrainsGrid.addColumn(Terrain::getNomTerrain).setHeader("Nom").setWidth("50%");
        terrainsGrid.addColumn(this::compterMatchs).setHeader("Matchs assignés").setWidth("25%");

        // Grid pour afficher les matchs liés au terrain sélectionné
        terrainsMatchsGrid = new Grid<>(MatchInfo.class, false);
        terrainsMatchsGrid.setHeight("200px");
        terrainsMatchsGrid.addColumn(m -> String.format("Match %d", m.getId())).setHeader("ID").setAutoWidth(true);
        terrainsMatchsGrid.addColumn(MatchInfo::toString).setHeader("Match (A vs B)").setAutoWidth(true).setFlexGrow(1);
        terrainsMatchsGrid.addColumn(m -> m.getTerrainNom() != null ? m.getTerrainNom() : "—").setHeader("Terrain assigné").setAutoWidth(true);

        H3 titreMatchsLies = new H3("Matchs assignés au terrain sélectionné");
        titreMatchsLies.addClassName("page-title");

        terrainsGrid.addSelectionListener(selection -> {
            selection.getFirstSelectedItem().ifPresent(terrain -> {
                remplirFormulaire(terrain);
                chargerMatchsParTerrain(terrain);
            });
        });

        rightPanel.add(titreTableau, terrainsGrid, titreMatchsLies, terrainsMatchsGrid);
    }

    private String compterMatchs(Terrain terrain) {
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
        try {
            int count = TerrainClassement.compterMatchsParTerrain(terrain.getId(), tournoiId);
            return String.valueOf(count);
        } catch (SQLException ex) {
            return "0";
        }
    }

    private void setMode(OperationMode mode) {
        currentMode = mode;
        boolean isSelectMode = mode != OperationMode.AJOUTER;

        terrainSelector.setVisible(isSelectMode);
        nomTerrainField.setReadOnly(mode == OperationMode.SUPPRIMER);
        numeroField.setReadOnly(mode == OperationMode.SUPPRIMER);

        String buttonLabel = switch (mode) {
            case AJOUTER -> "Ajouter";
            case MODIFIER -> "Modifier";
            case SUPPRIMER -> "Supprimer";
        };

        String modeLabel = switch (mode) {
            case AJOUTER -> "Ajouter un terrain";
            case MODIFIER -> "Modifier un terrain";
            case SUPPRIMER -> "Supprimer un terrain";
        };

        titreForm.setText(modeLabel);
        submitButton.setText(buttonLabel);

        if (isSelectMode) {
            nomTerrainField.clear();
            numeroField.clear();
        }
    }

    private void remplirFormulaire(Terrain terrain) {
        nomTerrainField.setValue(terrain.getNomTerrain() != null ? terrain.getNomTerrain() : "");
        numeroField.setValue(terrain.getNumero());
    }

    private boolean validerFormulaire() {
        if (nomTerrainField.getValue() == null || nomTerrainField.getValue().trim().isEmpty()) {
            afficherNotification("Le nom du terrain est requis", NotificationVariant.LUMO_ERROR);
            return false;
        }
        if (numeroField.getValue() == null || numeroField.getValue() < 1) {
            afficherNotification("Le numéro du terrain doit être >= 1", NotificationVariant.LUMO_ERROR);
            return false;
        }
        if (currentMode != OperationMode.AJOUTER && terrainSelector.getValue() == null) {
            afficherNotification("Sélectionnez un terrain", NotificationVariant.LUMO_ERROR);
            return false;
        }
        return true;
    }

    private void ajouterTerrain() {
        Terrain terrain = new Terrain(
                nomTerrainField.getValue().trim(),
                numeroField.getValue()
        );

        try (Connection con = ConnectionPool.getConnection()) {
            terrain.saveInDB(con);
            afficherNotification("Terrain ajouté avec succès", NotificationVariant.LUMO_SUCCESS);
            nomTerrainField.clear();
            numeroField.clear();
            chargerTerrains();
        } catch (SQLException ex) {
            afficherNotification("Erreur: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void modifierTerrain() {
        Terrain terrain = terrainSelector.getValue();
        if (terrain == null) return;

        terrain.setNomTerrain(nomTerrainField.getValue().trim());
        terrain.setNumero(numeroField.getValue());

        try {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            Terrain.mettreAJourTerrain(terrain, tournoiId);

            afficherNotification("Terrain modifié avec succès", NotificationVariant.LUMO_SUCCESS);
            chargerTerrains();
            terrainSelector.clear();
            nomTerrainField.clear();
            numeroField.clear();
            setMode(OperationMode.AJOUTER);
        } catch (SQLException ex) {
            afficherNotification("Erreur: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void supprimerTerrain() {
        Terrain terrain = terrainSelector.getValue();
        if (terrain == null) return;

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmer la suppression");
        dialog.setText("Êtes-vous sûr de vouloir supprimer ce terrain? "
                + "Les matchs assignés à ce terrain ne seront pas supprimés mais délié.");
        dialog.setConfirmText("Supprimer");
        dialog.setCancelText("Annuler");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> {
            try {
                int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
                Terrain.supprimerTerrain(terrain.getId(), tournoiId);

                afficherNotification("Terrain supprimé avec succès", NotificationVariant.LUMO_SUCCESS);
                chargerTerrains();
                terrainSelector.clear();
                nomTerrainField.clear();
                numeroField.clear();
                setMode(OperationMode.AJOUTER);
            } catch (SQLException ex) {
                afficherNotification("Erreur: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.open();
    }

    private void assignerTerrainAuMatch() {
        Terrain terrain = matchTerrainSelector.getValue();
        MatchInfo match = matchsGrid.getSelectedItems().stream().findFirst().orElse(null);

        if (terrain == null || match == null) {
            afficherNotification("Sélectionnez un terrain et un match", NotificationVariant.LUMO_WARNING);
            return;
        }

        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

        try {
            TerrainClassement.assignerTerrainAuMatch(terrain.getId(), match.getId(), tournoiId);
            afficherNotification("Terrain assigné au match avec succès", NotificationVariant.LUMO_SUCCESS);
            chargerMatchs();
            chargerTerrains();
            matchTerrainSelector.clear();
            matchsGrid.deselectAll();
        } catch (SQLException ex) {
            afficherNotification("Erreur: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void chargerTerrains() {
        try {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            List<Terrain> terrains = TerrainClassement.chargerTerrains(tournoiId);
            
            terrainsGrid.setItems(terrains);
            terrainSelector.setItems(terrains);
            if (matchTerrainSelector != null) {
                matchTerrainSelector.setItems(terrains);
            }
        } catch (SQLException ex) {
            afficherNotification("Erreur: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void chargerMatchs() {
        try {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            List<MatchInfo> matchs = TerrainClassement.chargerMatchs(tournoiId);
            matchsGrid.setItems(matchs);
        } catch (SQLException ex) {
            afficherNotification("Erreur: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void chargerMatchsParTerrain(Terrain terrain) {
        try {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            List<MatchInfo> matchs = TerrainClassement.chargerMatchsParTerrain(terrain.getId(), tournoiId);
            terrainsMatchsGrid.setItems(matchs);
        } catch (SQLException ex) {
            afficherNotification("Erreur: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void afficherNotification(String message, NotificationVariant variant) {
        Notification notif = new Notification(message);
        notif.addThemeVariants(variant);
        notif.setDuration(3000);
        notif.setPosition(Notification.Position.TOP_CENTER);
        notif.open();
    }
}
