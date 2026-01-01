package my.insa.yong.webui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map; 
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

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

    // === LOGO UI ===
    private Upload logoUpload;
    private MemoryBuffer logoBuffer;
    private Image logoPreview;
    private Button removeLogoButton;

    // === LOGO DATA (en mémoire pendant le formulaire) ===
    private byte[] logoBytes;
    private String logoMime;
    private String logoFileName;

    private Button ajouterButton;
    private Button modifierButton;
    private Button supprimerButton;
    private Button submitButton;
    private ComboBox<Equipe> equipeSelector;
    private Grid<Equipe> equipesGrid;
    private H2 titreForm;

    // Petit cache pour éviter de recharger le logo 50 fois
    private final Map<Integer, LogoData> logoCache = new HashMap<>();

    private static class LogoData {
        final byte[] bytes;
        final String mime;
        final String name;
        LogoData(byte[] bytes, String mime, String name) {
            this.bytes = bytes;
            this.mime = mime;
            this.name = name;
        }
    }

    private enum OperationMode {
        AJOUTER, MODIFIER, SUPPRIMER
    }

    private OperationMode currentMode = OperationMode.AJOUTER;

    public VueEquipe() {
        nomEquipeField = new TextField("Nom de l'équipe");
        dateCreationField = new DatePicker("Date de création");
        joueursComboBox = new MultiSelectComboBox<>();
        equipeSelector = new ComboBox<>("Sélectionner une équipe");
        titreForm = new H2("Ajouter une équipe");

        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setSizeFull();
        wrapper.addClassName("app-container");
        wrapper.setPadding(true);

        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(true);

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

        chargerEquipes();
        chargerJoueurs();
    }

    private void construireFormulaire(VerticalLayout leftPanel) {
        titreForm = new H2("Ajouter une équipe");
        titreForm.addClassName("page-title");

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
        dateCreationField.setValue(LocalDate.now());

        joueursComboBox = new MultiSelectComboBox<>();
        joueursComboBox.setLabel("Sélectionner les joueurs");
        joueursComboBox.setItemLabelGenerator(joueur
                -> String.format("%s %s (%d ans)", joueur.getPrenom(), joueur.getNom(), joueur.getAge()));
        joueursComboBox.addClassName("form-field");
        joueursComboBox.setWidthFull();

        // === LOGO COMPONENTS ===
        construireBlocLogo();

        submitButton = new Button("Valider");
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.addClassName("btn-primary");
        submitButton.addClickListener(e -> executerOperation());
        submitButton.setWidthFull();

        leftPanel.add(
                titreForm,
                operationButtons,
                equipeSelector,
                nomEquipeField,
                dateCreationField,
                joueursComboBox,
                logoUpload,
                removeLogoButton,
                logoPreview,
                submitButton
        );
    }

    private void construireBlocLogo() {
        logoBuffer = new MemoryBuffer();
        logoUpload = new Upload(logoBuffer);
        logoUpload.setDropAllowed(true);
        logoUpload.setMaxFiles(1);
        logoUpload.setAcceptedFileTypes("image/png", "image/jpeg", "image/webp");
        logoUpload.setMaxFileSize(2 * 1024 * 1024); // 2MB (ajuste si besoin)
        logoUpload.setUploadButton(new Button("Uploader le logo"));

        logoPreview = new Image();
        logoPreview.setVisible(false);
        logoPreview.getStyle().set("max-width", "180px");
        logoPreview.getStyle().set("max-height", "180px");

        removeLogoButton = new Button("Supprimer le logo");
        removeLogoButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        removeLogoButton.setVisible(true);
        removeLogoButton.addClickListener(e -> {
            logoBytes = null;
            logoMime = null;
            logoFileName = null;
            logoPreview.setVisible(false);
            Notification.show("Logo retiré (sera supprimé si vous validez en modification).", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        });

        logoUpload.addSucceededListener(event -> {
            try {
                logoMime = event.getMIMEType();
                logoFileName = event.getFileName();
                logoBytes = readAllBytes(logoBuffer.getInputStream());

                updateLogoPreview(logoBytes, logoMime);

                Notification.show("Logo chargé : " + logoFileName, 2500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Erreur lecture logo : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        logoUpload.addFileRejectedListener(event -> Notification
                .show("Fichier refusé : " + event.getErrorMessage(), 4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR));
    }

    private static byte[] readAllBytes(InputStream in) throws Exception {
        try (InputStream is = in; ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = is.read(buf)) != -1) {
                bos.write(buf, 0, r);
            }
            return bos.toByteArray();
        }
    }

    private void updateLogoPreview(byte[] bytes, String mime) {
        if (bytes == null || bytes.length == 0) {
            logoPreview.setVisible(false);
            return;
        }
        StreamResource res = new StreamResource("logo-" + System.nanoTime(), () -> new ByteArrayInputStream(bytes));
        if (mime != null && !mime.isBlank()) {
            res.setContentType(mime);
        }
        logoPreview.setSrc(res);
        logoPreview.setAlt("Logo équipe");
        logoPreview.setVisible(true);
    }

    private void construireTableau(VerticalLayout rightPanel) {
        H2 titreTable = new H2("Liste des équipes");
        titreTable.addClassName("page-title");

        equipesGrid = new Grid<>(Equipe.class, false);

        // Logo en miniature
        equipesGrid.addColumn(new ComponentRenderer<>(equipe -> buildLogoThumb(equipe.getId())))
                .setHeader("Logo")
                .setWidth("90px")
                .setFlexGrow(0);

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

    private Component buildLogoThumb(int equipeId) {
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
        try {
            LogoData data = logoCache.computeIfAbsent(equipeId, id -> {
                try {
                    return loadLogoFromDB(id, tournoiId);
                } catch (SQLException e) {
                    return new LogoData(null, null, null);
                }
            });

            if (data.bytes == null || data.bytes.length == 0) {
                Icon icon = VaadinIcon.PICTURE.create();
                icon.setSize("20px");
                return icon;
            }

            StreamResource res = new StreamResource("logo-thumb-" + equipeId + "-" + System.nanoTime(),
                    () -> new ByteArrayInputStream(data.bytes));
            if (data.mime != null && !data.mime.isBlank()) {
                res.setContentType(data.mime);
            }

            Image img = new Image(res, "Logo");
            img.getStyle().set("width", "32px");
            img.getStyle().set("height", "32px");
            img.getStyle().set("object-fit", "contain");
            return img;

        } catch (Exception ex) {
            Icon icon = VaadinIcon.PICTURE.create();
            icon.setSize("20px");
            return icon;
        }
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

        List<Joueur> joueursEquipe = chargerJoueursEquipe(equipe.getId());
        Set<Joueur> joueursSelectionnes = new HashSet<>(joueursEquipe);

        chargerJoueursDisponiblesPourModification(equipe.getId());
        joueursComboBox.setValue(joueursSelectionnes);

        // Charger le logo existant en base pour prévisualisation
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            LogoData data = loadLogoFromDB(equipe.getId(), tournoiId);
            logoBytes = data.bytes;
            logoMime = data.mime;
            logoFileName = data.name;
            updateLogoPreview(logoBytes, logoMime);
        } catch (SQLException ex) {
            // silencieux
        }
    }

    private void viderFormulaire() {
        nomEquipeField.clear();
        dateCreationField.setValue(LocalDate.now());
        joueursComboBox.clear();

        logoBytes = null;
        logoMime = null;
        logoFileName = null;
        logoPreview.setVisible(false);
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

        int minJoueurs = UserSession.getCurrentTournoiNombreJoueursParEquipe();
        if (joueursSelectionnes == null || joueursSelectionnes.isEmpty() || joueursSelectionnes.size() < minJoueurs) {
            Notification.show("⚠️ ERREUR : Une équipe doit contenir au minimum " + minJoueurs + " joueurs. Vous en avez sélectionné "
                            + (joueursSelectionnes == null ? 0 : joueursSelectionnes.size()) + ".",
                    4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {

            int maxEquipes = getMaxEquipesAutorisees(con);
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            int equipesActuelles = Equipe.compterEquipesPourTournoi(con, tournoiId);

            if (equipesActuelles >= maxEquipes) {
                Notification.show("IMPOSSIBLE D'AJOUTER : La limite de " + maxEquipes + " équipes pour ce tournoi ("
                                + (maxEquipes / 2) + " terrains) est atteinte.",
                        5000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            int maxJoueurs = UserSession.getCurrentTournoiNombreJoueursParEquipe();
            if (joueursSelectionnes.size() > maxJoueurs) {
                Notification.show("Trop de joueurs sélectionnés. Maximum autorisé : " + maxJoueurs + " joueurs pour ce tournoi.",
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            for (Joueur joueur : joueursSelectionnes) {
                if (Equipe.joueurDansAutreEquipe(con, joueur.getId(), tournoiId, -1)) {
                    Notification.show("❌ ERREUR : Un ou plusieurs joueurs sélectionnés sont déjà inscrits dans une autre équipe.",
                            5000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
            }

            con.setAutoCommit(false);

            Equipe nouvelleEquipe = new Equipe(nomEquipe, dateCreation);
            int equipeId = nouvelleEquipe.saveInDB(con);

            if (!joueursSelectionnes.isEmpty()) {
                Equipe.ajouterJoueursAEquipe(con, equipeId, new ArrayList<>(joueursSelectionnes), tournoiId);
            }

            // === Sauvegarde logo (si fourni) ===
            if (logoBytes != null && logoBytes.length > 0) {
                updateEquipeLogo(con, equipeId, tournoiId, logoBytes, logoMime, logoFileName);
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

        int minJoueurs = UserSession.getCurrentTournoiNombreJoueursParEquipe();
        if (joueursSelectionnes == null || joueursSelectionnes.isEmpty() || joueursSelectionnes.size() < minJoueurs) {
            Notification.show("⚠️ ERREUR : Une équipe doit contenir au minimum " + minJoueurs + " joueurs.",
                    4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        int maxJoueurs = UserSession.getCurrentTournoiNombreJoueursParEquipe();
        if (joueursSelectionnes.size() > maxJoueurs) {
            Notification.show("Trop de joueurs sélectionnés. Maximum autorisé : " + maxJoueurs + " joueurs pour ce tournoi.",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

            for (Joueur joueur : joueursSelectionnes) {
                if (Equipe.joueurDansAutreEquipe(con, joueur.getId(), tournoiId, equipeSelectionnee.getId())) {
                    Notification.show("❌ ERREUR : Un ou plusieurs joueurs sélectionnés sont déjà inscrits dans une autre équipe.",
                            5000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
            }

            con.setAutoCommit(false);

            equipeSelectionnee.setNomEquipe(nomEquipe);
            equipeSelectionnee.setDateCreation(dateCreation);
            Equipe.modifierEquipe(con, equipeSelectionnee, tournoiId);

            Equipe.supprimerJoueursEquipe(con, equipeSelectionnee.getId(), tournoiId);

            if (!joueursSelectionnes.isEmpty()) {
                Equipe.ajouterJoueursAEquipe(con, equipeSelectionnee.getId(), new ArrayList<>(joueursSelectionnes), tournoiId);
            }

            // === Mise à jour logo (même si null : ça permet de supprimer) ===
            updateEquipeLogo(con, equipeSelectionnee.getId(), tournoiId, logoBytes, logoMime, logoFileName);

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

            Equipe.supprimerJoueursEquipe(con, equipeSelectionnee.getId(), tournoiId);
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

            // Cache logos reset (sinon tu peux garder si tu veux)
            logoCache.clear();

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
            // silencieux
        }
        return new ArrayList<>();
    }

    private int getMaxEquipesAutorisees(Connection con) throws SQLException {
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
        Parametre params = Parametre.getParametreById(con, tournoiId);
        if (params != null) {
            return params.getNombreTerrains() * 2;
        }
        return Integer.MAX_VALUE;
    }

    // ======================= LOGO DB HELPERS =======================

    private static void updateEquipeLogo(Connection con, int equipeId, int tournoiId,
                                         byte[] bytes, String mime, String filename) throws SQLException {
        String sql = "UPDATE equipe SET logo = ?, logo_mime = ?, logo_nom = ? WHERE id = ? AND tournoi_id = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {

            if (bytes != null && bytes.length > 0) {
                pst.setBytes(1, bytes);
            } else {
                pst.setNull(1, Types.BLOB); // supprime le logo si null
            }

            if (mime != null && !mime.isBlank()) {
                pst.setString(2, mime);
            } else {
                pst.setNull(2, Types.VARCHAR);
            }

            if (filename != null && !filename.isBlank()) {
                pst.setString(3, filename);
            } else {
                pst.setNull(3, Types.VARCHAR);
            }

            pst.setInt(4, equipeId);
            pst.setInt(5, tournoiId);
            pst.executeUpdate();
        }
    }

    private static LogoData loadLogoFromDB(int equipeId, int tournoiId) throws SQLException {
        String sql = "SELECT logo, logo_mime, logo_nom FROM equipe WHERE id = ? AND tournoi_id = ?";
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, equipeId);
            pst.setInt(2, tournoiId);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    byte[] bytes = rs.getBytes("logo");
                    String mime = rs.getString("logo_mime");
                    String name = rs.getString("logo_nom");
                    return new LogoData(bytes, mime, name);
                }
            }
        }
        return new LogoData(null, null, null);
    }
}
