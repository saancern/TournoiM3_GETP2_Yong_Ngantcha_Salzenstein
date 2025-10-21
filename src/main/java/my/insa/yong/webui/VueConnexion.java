package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;

/**
 * Page de connexion/inscription en français
 *
 * @author saancern
 */
@Route(value = "connexion")
@PageTitle("Connexion/Inscription")
public class VueConnexion extends VerticalLayout {

    private TextField champSurnom;
    private PasswordField champMotDePasse;
    private Button boutonConnexion;
    private Button boutonInscription;

    public VueConnexion() {
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.CENTER);
        this.setSizeFull();
        this.addClassName("app-container");

        construireInterface();
        configurerEvenements();
    }

    private void construireInterface() {
        // Titre principal
        H1 titre = new H1("Connexion / Inscription");
        titre.addClassName("page-title");

        // Sous-titre
        H3 sousTitre = new H3("Veuillez saisir vos informations");
        sousTitre.addClassName("page-subtitle");

        // Champs de saisie
        champSurnom = new TextField("Nom d'utilisateur");
        champSurnom.setPlaceholder("Saisissez votre nom d'utilisateur");
        champSurnom.setRequiredIndicatorVisible(true);
        champSurnom.addClassName("form-field");

        champMotDePasse = new PasswordField("Mot de passe");
        champMotDePasse.setPlaceholder("Saisissez votre mot de passe");
        champMotDePasse.setRequiredIndicatorVisible(true);
        champMotDePasse.addClassName("form-field");

        // Boutons
        boutonConnexion = new Button("Se connecter");
        boutonConnexion.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        boutonConnexion.addClassName("btn-primary");

        boutonInscription = new Button("S'inscrire");
        boutonInscription.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        boutonInscription.addClassName("btn-success");

        // Layout des boutons
        HorizontalLayout layoutBoutons = new HorizontalLayout(boutonConnexion, boutonInscription);
        layoutBoutons.addClassName("button-group");

        // Container principal
        VerticalLayout formulaire = new VerticalLayout();
        formulaire.setAlignItems(Alignment.CENTER);
        formulaire.setSpacing(true);
        formulaire.setPadding(true);
        formulaire.addClassName("form-container");
        formulaire.addClassName("fade-in");

        formulaire.add(titre, sousTitre, champSurnom, champMotDePasse, layoutBoutons);

        this.add(formulaire);
    }

    private void configurerEvenements() {
        boutonConnexion.addClickListener(e -> gererConnexion());
        boutonInscription.addClickListener(e -> gererInscription());
    }

    private void gererConnexion() {
        if (!validerChamps()) {
            return;
        }

        String surnom = champSurnom.getValue().trim();
        String motDePasse = champMotDePasse.getValue();

        try (Connection con = ConnectionPool.getConnection()) {
            String sql = "SELECT id, surnom, pass, isAdmin FROM utilisateur WHERE surnom = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, surnom);
                ResultSet rs = pst.executeQuery();

                if (rs.next()) {
                    String passStocke = rs.getString("pass");
                    if (motDePasse.equals(passStocke)) {
                        int userId = rs.getInt("id");
                        boolean isAdmin = rs.getBoolean("isAdmin");
                        
                        // Set user session
                        UserSession.setCurrentUser(userId, surnom, isAdmin);
                        
                        // Connexion réussie - automatically redirect based on admin status
                        String roleMessage = isAdmin ? " (Administrateur)" : " (Utilisateur)";
                        afficherNotificationSucces("Connexion réussie ! Bienvenue " + surnom + roleMessage);
                        
                        // Redirect to main page
                        getUI().ifPresent(ui -> ui.navigate(""));
                    } else {
                        afficherNotificationErreur("Mot de passe incorrect.");
                    }
                } else {
                    afficherNotificationErreur("Utilisateur non trouvé. Veuillez vous inscrire.");
                }
            }
        } catch (SQLException ex) {
            afficherNotificationErreur("Erreur de connexion à la base de données : " + ex.getMessage());
        }
    }

    private void gererInscription() {
        if (!validerChamps()) {
            return;
        }

        String surnom = champSurnom.getValue().trim();
        String motDePasse = champMotDePasse.getValue();

        // Show admin privileges dialog
        ConfirmDialog adminDialog = new ConfirmDialog();
        adminDialog.setHeader("Privilèges administrateur");
        adminDialog.setText("Voulez-vous avoir des privilèges d'administrateur ?");
        adminDialog.setCancelable(true);
        adminDialog.setConfirmText("Oui (Admin)");
        adminDialog.setCancelText("Non (Utilisateur)");

        adminDialog.addConfirmListener(event -> {
            // User chose admin privileges
            creerUtilisateur(surnom, motDePasse, true);
        });

        adminDialog.addCancelListener(event -> {
            // User chose regular user
            creerUtilisateur(surnom, motDePasse, false);
        });

        adminDialog.open();
    }

    private void creerUtilisateur(String surnom, String motDePasse, boolean isAdmin) {
        try (Connection con = ConnectionPool.getConnection()) {
            // Vérifier si l'utilisateur existe déjà
            String sqlVerif = "SELECT COUNT(*) FROM utilisateur WHERE surnom = ?";
            try (PreparedStatement pstVerif = con.prepareStatement(sqlVerif)) {
                pstVerif.setString(1, surnom);
                ResultSet rs = pstVerif.executeQuery();
                rs.next();

                if (rs.getInt(1) > 0) {
                    afficherNotificationErreur("Ce nom d'utilisateur existe déjà. Veuillez en choisir un autre ou vous connecter.");
                    return;
                }
            }

            // Créer le nouvel utilisateur
            String sqlInsert = "INSERT INTO utilisateur (surnom, pass, isAdmin) VALUES (?, ?, ?)";
            try (PreparedStatement pstInsert = con.prepareStatement(sqlInsert, PreparedStatement.RETURN_GENERATED_KEYS)) {
                pstInsert.setString(1, surnom);
                pstInsert.setString(2, motDePasse);
                pstInsert.setBoolean(3, isAdmin);

                int rowsAffected = pstInsert.executeUpdate();

                if (rowsAffected > 0) {
                    // Get the generated user ID
                    ResultSet generatedKeys = pstInsert.getGeneratedKeys();
                    int userId = -1;
                    if (generatedKeys.next()) {
                        userId = generatedKeys.getInt(1);
                    }
                    
                    // Set user session for automatic login
                    UserSession.setCurrentUser(userId, surnom, isAdmin);
                    
                    String roleMessage = isAdmin ? " (Administrateur)" : " (Utilisateur)";
                    afficherNotificationSucces("Inscription réussie ! Connexion automatique..." + roleMessage);
                    viderChamps();
                    
                    // Automatically login the user and redirect
                    getUI().ifPresent(ui -> ui.navigate(""));
                } else {
                    afficherNotificationErreur("Échec de l'inscription. Veuillez réessayer.");
                }
            }
        } catch (SQLException ex) {
            afficherNotificationErreur("Erreur lors de l'inscription : " + ex.getMessage());
        }
    }

    private boolean validerChamps() {
        if (champSurnom.getValue() == null || champSurnom.getValue().trim().isEmpty()) {
            afficherNotificationErreur("Le nom d'utilisateur est obligatoire.");
            champSurnom.focus();
            return false;
        }

        if (champMotDePasse.getValue() == null || champMotDePasse.getValue().isEmpty()) {
            afficherNotificationErreur("Le mot de passe est obligatoire.");
            champMotDePasse.focus();
            return false;
        }

        return true;
    }

    private void viderChamps() {
        champSurnom.clear();
        champMotDePasse.clear();
    }

    private void afficherNotificationSucces(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void afficherNotificationErreur(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
