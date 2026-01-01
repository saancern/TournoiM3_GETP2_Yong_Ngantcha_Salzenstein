package my.insa.yong.webui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import my.insa.yong.model.Joueur;
import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.components.BaseLayout;

@Route("joueur")
@PageTitle("Joueur")
public class VueJoueur extends BaseLayout {

    private TextField prenomField;
    private TextField nomField;
    private NumberField ageField;
    private ComboBox<String> sexeField;
    private NumberField tailleField;

    // ===== PHOTO UI =====
    private Upload photoUpload;
    private MemoryBuffer photoBuffer;
    private Image photoPreview;
    private Button removePhotoButton;

    // ===== PHOTO DATA (mémoire formulaire) =====
    private byte[] photoBytes;
    private String photoMime;
    private String photoFileName;

    private Button ajouterButton;
    private Button modifierButton;
    private Button supprimerButton;
    private Button submitButton;
    private ComboBox<Joueur> playerSelector;
    private Grid<Joueur> joueursGrid;
    private H2 titreForm;
    private H2 titreTable;
    private Button triButton;
    private boolean triCroissant = true;

    private enum OperationMode {
        AJOUTER, MODIFIER, SUPPRIMER
    }

    private OperationMode currentMode = OperationMode.AJOUTER;

    // Cache miniatures (évite de recharger en DB trop souvent)
    private final Map<Integer, PhotoData> photoCache = new HashMap<>();

    private static class PhotoData {
        final byte[] bytes;
        final String mime;
        final String name;

        PhotoData(byte[] bytes, String mime, String name) {
            this.bytes = bytes;
            this.mime = mime;
            this.name = name;
        }
    }

    public VueJoueur() {
        prenomField = new TextField("Prénom");
        nomField = new TextField("Nom");
        ageField = new NumberField("Âge");
        sexeField = new ComboBox<>("Sexe");
        tailleField = new NumberField("Taille (cm)");
        playerSelector = new ComboBox<>("Sélectionner un joueur");
        titreForm = new H2("Détails du joueur");
        titreTable = new H2("Liste des joueurs");

        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setSizeFull();
        wrapper.addClassName("app-container");
        wrapper.setPadding(true);

        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(true);

        if (UserSession.adminConnected()) {
            VerticalLayout leftPanel = new VerticalLayout();
            leftPanel.addClassName("form-container");
            leftPanel.addClassName("fade-in");
            leftPanel.setWidth("400px");
            leftPanel.setSpacing(true);
            leftPanel.setPadding(true);

            titreForm.addClassName("page-title");

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

            prenomField.setPlaceholder("Entrez le prénom du joueur");
            prenomField.setRequired(true);
            prenomField.addClassName("form-field");
            prenomField.setWidthFull();

            nomField.setPlaceholder("Entrez le nom du joueur");
            nomField.setRequired(true);
            nomField.addClassName("form-field");
            nomField.setWidthFull();

            ageField.setPlaceholder("Entrez l'âge du joueur");
            ageField.setRequired(true);
            ageField.setMin(1);
            ageField.setMax(150);
            ageField.addClassName("form-field");
            ageField.setWidthFull();

            sexeField.setItems("H", "F");
            sexeField.setPlaceholder("Sélectionnez le sexe");
            sexeField.setRequired(true);
            sexeField.addClassName("form-field");
            sexeField.setWidthFull();

            tailleField.setPlaceholder("Entrez la taille du joueur en cm");
            tailleField.setRequired(true);
            tailleField.setMin(50);
            tailleField.setMax(250);
            tailleField.addClassName("form-field");
            tailleField.setWidthFull();

            // ===== PHOTO BLOCK =====
            construireBlocPhoto();

            submitButton = new Button("Valider");
            submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            submitButton.addClassName("btn-primary");
            submitButton.addClickListener(e -> executerOperation());
            submitButton.setWidthFull();

            leftPanel.add(
                    titreForm,
                    operationButtons,
                    playerSelector,
                    prenomField,
                    nomField,
                    ageField,
                    sexeField,
                    tailleField,
                    photoUpload,
                    removePhotoButton,
                    photoPreview,
                    submitButton
            );

            mainLayout.add(leftPanel);
        } else {
            titreForm.addClassName("page-title");
        }

        VerticalLayout rightPanel = new VerticalLayout();
        rightPanel.setSizeFull();
        rightPanel.setSpacing(true);
        rightPanel.setPadding(true);

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(Alignment.CENTER);

        titreTable.addClassName("page-title");

        triButton = new Button("↓ Z-A", e -> {
            triCroissant = !triCroissant;
            triButton.setText(triCroissant ? "↓ Z-A" : "↑ A-Z");
            chargerJoueurs();
        });
        triButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        headerLayout.add(titreTable);
        headerLayout.expand(titreTable);
        headerLayout.add(triButton);

        joueursGrid = new Grid<>(Joueur.class, false);

        // ===== COLONNE PHOTO (miniature) =====
        joueursGrid.addColumn(new ComponentRenderer<>(joueur -> buildPhotoThumb(joueur.getId())))
                .setHeader("Photo")
                .setWidth("90px")
                .setFlexGrow(0);

        joueursGrid.addColumn(Joueur::getId).setHeader("ID").setWidth("80px").setFlexGrow(0);
        joueursGrid.addColumn(Joueur::getPrenom).setHeader("Prénom").setAutoWidth(true);
        joueursGrid.addColumn(Joueur::getNom).setHeader("Nom").setAutoWidth(true);
        joueursGrid.addColumn(Joueur::getAge).setHeader("Âge").setAutoWidth(true);
        joueursGrid.addColumn(Joueur::getSexe).setHeader("Sexe").setAutoWidth(true);
        joueursGrid.addColumn(joueur -> String.format("%.1f cm", joueur.getTaille()))
                .setHeader("Taille").setAutoWidth(true);

        joueursGrid.setSizeFull();
        joueursGrid.addClassName("players-grid");

        rightPanel.add(headerLayout, joueursGrid);

        if (UserSession.adminConnected()) {
            mainLayout.add(rightPanel);
            mainLayout.setFlexGrow(1, rightPanel);
        } else {
            rightPanel.setWidth("100%");
            mainLayout.add(rightPanel);
        }

        wrapper.add(mainLayout);
        this.setContent(wrapper);

        chargerJoueurs();

        if (UserSession.adminConnected()) {
            setMode(OperationMode.AJOUTER);
        }
    }

    // ===================== PHOTO UI =====================

    private void construireBlocPhoto() {
        photoBuffer = new MemoryBuffer();
        photoUpload = new Upload(photoBuffer);
        photoUpload.setDropAllowed(true);
        photoUpload.setMaxFiles(1);
        photoUpload.setAcceptedFileTypes("image/png", "image/jpeg", "image/webp");
        photoUpload.setMaxFileSize(2 * 1024 * 1024); // 2MB
        photoUpload.setUploadButton(new Button("Uploader une photo"));

        photoPreview = new Image();
        photoPreview.setVisible(false);
        photoPreview.getStyle().set("max-width", "180px");
        photoPreview.getStyle().set("max-height", "180px");

        removePhotoButton = new Button("Supprimer la photo");
        removePhotoButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        removePhotoButton.addClickListener(e -> {
            photoBytes = null;
            photoMime = null;
            photoFileName = null;
            photoPreview.setVisible(false);
            Notification.show("Photo retirée (sera supprimée si vous validez en modification).", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        });

        photoUpload.addSucceededListener(event -> {
            try {
                photoMime = event.getMIMEType();
                photoFileName = event.getFileName();
                photoBytes = readAllBytes(photoBuffer.getInputStream());
                updatePhotoPreview(photoBytes, photoMime);

                Notification.show("Photo chargée : " + photoFileName, 2500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Erreur lecture photo : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        photoUpload.addFileRejectedListener(event -> Notification
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

    private void updatePhotoPreview(byte[] bytes, String mime) {
        if (bytes == null || bytes.length == 0) {
            photoPreview.setVisible(false);
            return;
        }
        StreamResource res = new StreamResource("photo-" + System.nanoTime(), () -> new ByteArrayInputStream(bytes));
        if (mime != null && !mime.isBlank()) {
            res.setContentType(mime);
        }
        photoPreview.setSrc(res);
        photoPreview.setAlt("Photo joueur");
        photoPreview.setVisible(true);
    }

    private Component buildPhotoThumb(int joueurId) {
        int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

        try {
            PhotoData data = photoCache.computeIfAbsent(joueurId, id -> {
                try {
                    return loadPhotoFromDB(id, tournoiId);
                } catch (SQLException e) {
                    return new PhotoData(null, null, null);
                }
            });

            if (data.bytes == null || data.bytes.length == 0) {
                Icon icon = VaadinIcon.USER.create();
                icon.setSize("20px");
                return icon;
            }

            StreamResource res = new StreamResource("photo-thumb-" + joueurId + "-" + System.nanoTime(),
                    () -> new ByteArrayInputStream(data.bytes));
            if (data.mime != null && !data.mime.isBlank()) {
                res.setContentType(data.mime);
            }

            Image img = new Image(res, "Photo");
            img.getStyle().set("width", "32px");
            img.getStyle().set("height", "32px");
            img.getStyle().set("object-fit", "cover");
            img.getStyle().set("border-radius", "50%");
            return img;

        } catch (Exception ex) {
            Icon icon = VaadinIcon.USER.create();
            icon.setSize("20px");
            return icon;
        }
    }

    // ===================== MODES =====================

    private void setMode(OperationMode mode) {
        this.currentMode = mode;

        // Visibilité du bloc photo selon le mode
        boolean showPhotoControls = (mode != OperationMode.SUPPRIMER);
        if (photoUpload != null) photoUpload.setVisible(showPhotoControls);
        if (removePhotoButton != null) removePhotoButton.setVisible(showPhotoControls);

        switch (mode) {
            case AJOUTER:
                titreForm.setText("Détails du joueur");
                ajouterButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                modifierButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                modifierButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                supprimerButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                supprimerButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

                playerSelector.setVisible(false);
                setReadOnlyForm(false);
                resetSubmitButtonStyle(false);
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
                setReadOnlyForm(false);
                resetSubmitButtonStyle(false);
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
                setReadOnlyForm(true);
                resetSubmitButtonStyle(true);
                viderFormulaire();
                chargerJoueursSelector();
                break;
        }
    }

    private void setReadOnlyForm(boolean ro) {
        prenomField.setReadOnly(ro);
        nomField.setReadOnly(ro);
        tailleField.setReadOnly(ro);
        ageField.setReadOnly(ro);
        sexeField.setReadOnly(ro);
    }

    private void resetSubmitButtonStyle(boolean danger) {
        if (submitButton == null) return;
        submitButton.removeThemeVariants(ButtonVariant.LUMO_ERROR);
        submitButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        if (danger) {
            submitButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        } else {
            submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
    }

    private void remplirFormulaire(Joueur joueur) {
        prenomField.setValue(joueur.getPrenom());
        nomField.setValue(joueur.getNom());
        tailleField.setValue(joueur.getTaille());
        ageField.setValue((double) joueur.getAge());
        sexeField.setValue(joueur.getSexe());

        // Charger la photo existante
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            PhotoData data = loadPhotoFromDB(joueur.getId(), tournoiId);
            photoBytes = data.bytes;
            photoMime = data.mime;
            photoFileName = data.name;
            updatePhotoPreview(photoBytes, photoMime);
        } catch (SQLException ex) {
            // silencieux
        }
    }

    private void viderFormulaire() {
        prenomField.clear();
        nomField.clear();
        tailleField.clear();
        playerSelector.clear();
        ageField.clear();
        sexeField.clear();

        // reset photo
        photoBytes = null;
        photoMime = null;
        photoFileName = null;
        if (photoPreview != null) photoPreview.setVisible(false);
    }

    private void chargerJoueursSelector() {
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            List<Joueur> joueurs = Joueur.chargerJoueursPourTournoi(con, tournoiId);
            playerSelector.setItems(joueurs);
        } catch (SQLException ex) {
            Notification.show("Erreur lors du chargement des joueurs : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

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

    private void ajouterJoueur() {
        String prenom = prenomField.getValue().trim();
        String nom = nomField.getValue().trim();
        Double taille = tailleField.getValue();
        Double ageDouble = ageField.getValue();
        String sexe = sexeField.getValue();

        if (prenom.isEmpty() || nom.isEmpty() || taille == null || taille <= 0
                || ageDouble == null || ageDouble <= 0 || sexe == null || sexe.isEmpty()) {
            Notification.show("Veuillez remplir tous les champs correctement.", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        int age = ageDouble.intValue();

        try (Connection con = ConnectionPool.getConnection()) {
            con.setAutoCommit(false);

            Joueur nouveauJoueur = new Joueur(prenom, nom, age, sexe, taille);
            int newId = nouveauJoueur.saveInDB(con);

            // Sauver photo si présente
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            if (photoBytes != null && photoBytes.length > 0) {
                updateJoueurPhoto(con, newId, tournoiId, photoBytes, photoMime, photoFileName);
            }

            con.commit();

            Notification.show("Joueur ajouté avec succès dans " + UserSession.getCurrentTournoiName() + " ! (ID: " + newId + ")",
                            3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            viderFormulaire();
            chargerJoueurs();

        } catch (SQLException ex) {
            Notification.show("Erreur lors de l'ajout : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

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

        if (prenom.isEmpty() || nom.isEmpty() || taille == null || taille <= 0
                || ageDouble == null || ageDouble <= 0 || sexe == null || sexe.isEmpty()) {
            Notification.show("Veuillez remplir tous les champs correctement.", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        int age = ageDouble.intValue();

        try (Connection con = ConnectionPool.getConnection()) {
            con.setAutoCommit(false);

            Joueur.modifierJoueur(con, joueurSelectionne.getId(), prenom, nom, age, sexe, taille);

            // Update photo (même si null => permet suppression)
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            updateJoueurPhoto(con, joueurSelectionne.getId(), tournoiId, photoBytes, photoMime, photoFileName);

            con.commit();

            Notification.show("Joueur modifié avec succès !", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            viderFormulaire();
            chargerJoueurs();
            chargerJoueursSelector();

        } catch (SQLException ex) {
            Notification.show("Erreur lors de la modification : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void supprimerJoueur() {
        Joueur joueurSelectionne = playerSelector.getValue();
        if (joueurSelectionne == null) {
            Notification.show("Veuillez sélectionner un joueur à supprimer.", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            Joueur.supprimerJoueur(con, joueurSelectionne.getId(), tournoiId);

            Notification.show("Joueur supprimé avec succès !", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            viderFormulaire();
            chargerJoueurs();
            chargerJoueursSelector();
            setMode(OperationMode.AJOUTER);

        } catch (SQLException ex) {
            Notification.show("Erreur lors de la suppression : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void chargerJoueurs() {
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            String ordre = triCroissant ? "ASC" : "DESC";
            String sortCriteria = "nom " + ordre + ", prenom " + ordre;
            List<Joueur> joueurs = Joueur.chargerJoueursPourTournoi(con, tournoiId, sortCriteria);

            photoCache.clear(); // refresh miniatures
            joueursGrid.setItems(joueurs);

        } catch (SQLException ex) {
            Notification notification = Notification.show("Erreur lors du chargement des joueurs : " + ex.getMessage(),
                    4000, Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // ===================== PHOTO DB HELPERS =====================

    private static void updateJoueurPhoto(Connection con, int joueurId, int tournoiId,
                                          byte[] bytes, String mime, String filename) throws SQLException {
        String sql = "UPDATE joueur SET photo = ?, photo_mime = ?, photo_nom = ? WHERE id = ? AND tournoi_id = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {

            if (bytes != null && bytes.length > 0) {
                pst.setBytes(1, bytes);
            } else {
                pst.setNull(1, Types.BLOB);
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

            pst.setInt(4, joueurId);
            pst.setInt(5, tournoiId);
            pst.executeUpdate();
        }
    }

    private static PhotoData loadPhotoFromDB(int joueurId, int tournoiId) throws SQLException {
        String sql = "SELECT photo, photo_mime, photo_nom FROM joueur WHERE id = ? AND tournoi_id = ?";
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, joueurId);
            pst.setInt(2, tournoiId);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    byte[] bytes = rs.getBytes("photo");
                    String mime = rs.getString("photo_mime");
                    String name = rs.getString("photo_nom");
                    return new PhotoData(bytes, mime, name);
                }
            }
        }
        return new PhotoData(null, null, null);
    }
}
