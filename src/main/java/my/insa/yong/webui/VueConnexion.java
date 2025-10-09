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

import my.insa.yong.utils.database.ConnectionSimpleSGBD;

/**
 * Page de connexion/inscription en français
 *
 * @author saancern
 */
@Route(value = "connexion")
@PageTitle("Connexion - Inscription")
public class VueConnexion extends VerticalLayout {

    private TextField champSurnom;
    private PasswordField champMotDePasse;
    private Button boutonConnexion;
    private Button boutonInscription;

    public VueConnexion() {
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.CENTER);
        this.setSizeFull();

        construireInterface();
        configurerEvenements();
    }

    private void construireInterface() {
        // Titre principal
        H1 titre = new H1("Connexion / Inscription");
        titre.getStyle().set("color", "#2c3e50");

        // Sous-titre
        H3 sousTitre = new H3("Veuillez saisir vos informations");
        sousTitre.getStyle().set("color", "#7f8c8d");

        // Champs de saisie
        champSurnom = new TextField("Nom d'utilisateur");
        champSurnom.setPlaceholder("Saisissez votre nom d'utilisateur");
        champSurnom.setRequiredIndicatorVisible(true);
        champSurnom.setWidth("300px");

        champMotDePasse = new PasswordField("Mot de passe");
        champMotDePasse.setPlaceholder("Saisissez votre mot de passe");
        champMotDePasse.setRequiredIndicatorVisible(true);
        champMotDePasse.setWidth("300px");

        // Boutons
        boutonConnexion = new Button("Se connecter");
        boutonConnexion.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        boutonConnexion.setWidth("140px");

        boutonInscription = new Button("S'inscrire");
        boutonInscription.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        boutonInscription.setWidth("140px");

        // Layout des boutons
        HorizontalLayout layoutBoutons = new HorizontalLayout(boutonConnexion, boutonInscription);
        layoutBoutons.setSpacing(true);

        // Container principal
        VerticalLayout formulaire = new VerticalLayout();
        formulaire.setAlignItems(Alignment.CENTER);
        formulaire.setSpacing(true);
        formulaire.setPadding(true);
        formulaire.getStyle().set("border", "1px solid #ddd");
        formulaire.getStyle().set("border-radius", "8px");
        formulaire.getStyle().set("background-color", "#fafafa");
        formulaire.setWidth("400px");

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

        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            String sql = "SELECT id, surnom, pass, isAdmin FROM utilisateur WHERE surnom = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, surnom);
                ResultSet rs = pst.executeQuery();

                if (rs.next()) {
                    String passStocke = rs.getString("pass");
                    if (motDePasse.equals(passStocke)) {
                        boolean isAdmin = rs.getBoolean("isAdmin");
                        // Connexion réussie - automatically redirect based on admin status
                        String roleMessage = isAdmin ? " (Administrateur)" : " (Utilisateur)";
                        afficherNotificationSucces("Connexion réussie ! Bienvenue " + surnom + roleMessage);
                        
                        // TODO: Store user session info including admin status
                        // For now, redirect to main page
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
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
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
            try (PreparedStatement pstInsert = con.prepareStatement(sqlInsert)) {
                pstInsert.setString(1, surnom);
                pstInsert.setString(2, motDePasse);
                pstInsert.setBoolean(3, isAdmin);

                int rowsAffected = pstInsert.executeUpdate();

                if (rowsAffected > 0) {
                    String roleMessage = isAdmin ? " (Administrateur)" : " (Utilisateur)";
                    afficherNotificationSucces("Inscription réussie ! Connexion automatique..." + roleMessage);
                    viderChamps();
                    
                    // Automatically login the user
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
