package my.insa.yong.webui;

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
import my.insa.yong.model.Utilisateur;
import my.insa.yong.model.Utilisateur.LoginResult;

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
    private TextField champPrenom; // For joueur matching
    private TextField champNom; // For joueur matching
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

        // Champs pour lier au joueur (visibles seulement à l'inscription)
        champPrenom = new TextField("Prénom");
        champPrenom.setPlaceholder("Votre prénom (optionnel pour joueurs)");
        champPrenom.addClassName("form-field");
        champPrenom.setVisible(false); // Hidden by default

        champNom = new TextField("Nom");
        champNom.setPlaceholder("Votre nom (optionnel pour joueurs)");
        champNom.addClassName("form-field");
        champNom.setVisible(false); // Hidden by default

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

        formulaire.add(titre, sousTitre, champSurnom, champMotDePasse, champPrenom, champNom, layoutBoutons);

        this.add(formulaire);
    }

    private void configurerEvenements() {
        boutonConnexion.addClickListener(e -> gererConnexion());
        boutonInscription.addClickListener(e -> {
            // Show role selection dialog with 3 options
            champPrenom.setVisible(true);
            champNom.setVisible(true);
            afficherDialogueRole();
        });
    }
    
    private void afficherDialogueRole() {
        if (!validerChamps()) {
            return;
        }

        ConfirmDialog roleDialog = new ConfirmDialog();
        roleDialog.setHeader("Type de compte");
        roleDialog.setText("Choisissez le type de compte à créer :");
        roleDialog.setCancelable(true);
        
        // Bouton Admin
        roleDialog.setConfirmText("Administrateur");
        roleDialog.addConfirmListener(event -> {
            creerUtilisateur(true, false);
        });
        
        // Bouton Cancel pour Joueur
        roleDialog.setCancelText("Joueur");
        roleDialog.addCancelListener(event -> {
            creerUtilisateur(false, true);
        });
        
        // Bouton Reject pour Utilisateur
        roleDialog.setRejectText("Utilisateur");
        roleDialog.setRejectable(true);
        roleDialog.addRejectListener(event -> {
            creerUtilisateur(false, false);
        });
        
        roleDialog.open();
    }

    private void gererConnexion() {
        if (!validerChamps()) {
            return;
        }

        String surnom = champSurnom.getValue().trim();
        String motDePasse = champMotDePasse.getValue();

        LoginResult result = Utilisateur.authenticateUser(surnom, motDePasse);
        
        if (result.isSuccess()) {
            // Set user session
            UserSession.setCurrentUser(result.getUserId(), result.getUsername(), result.isAdmin());
            
            // Connexion réussie - automatically redirect based on admin status
            String roleMessage = result.isAdmin() ? " (Administrateur)" : " (Utilisateur)";
            afficherNotificationSucces("Connexion réussie ! Bienvenue " + surnom + roleMessage);
            
            // Redirect to main page
            getUI().ifPresent(ui -> ui.navigate(""));
        } else {
            afficherNotificationErreur(result.getErrorMessage());
        }
    }

    private void creerUtilisateur(boolean isAdmin, boolean requiresJoueur) {
        String surnom = champSurnom.getValue().trim();
        String motDePasse = champMotDePasse.getValue();
        String prenom = champPrenom.getValue() != null ? champPrenom.getValue().trim() : null;
        String nom = champNom.getValue() != null ? champNom.getValue().trim() : null;
        
        // Si c'est un joueur, vérifier que prénom et nom sont remplis
        if (requiresJoueur && (prenom == null || prenom.isEmpty() || nom == null || nom.isEmpty())) {
            afficherNotificationErreur("Pour créer un compte joueur, vous devez renseigner votre prénom et nom (identiques à ceux dans la base de joueurs).");
            return;
        }
        
        LoginResult result = Utilisateur.registerUser(surnom, motDePasse, prenom, nom, isAdmin);
        
        if (result.isSuccess()) {
            // Set user session for automatic login
            UserSession.setCurrentUser(result.getUserId(), result.getUsername(), result.isAdmin());
            
            String roleMessage = result.isAdmin() ? " (Administrateur)" : " (Utilisateur)";
            
            // Check if user was linked to a joueur
            Integer joueurId = UserSession.getCurrentJoueurId();
            if (joueurId != null) {
                roleMessage = " (Joueur - Profil lié ✓)";
            } else if (requiresJoueur) {
                afficherNotificationErreur("Aucun joueur trouvé avec le prénom '" + prenom + "' et le nom '" + nom + "'. Compte créé comme utilisateur simple.");
                roleMessage = " (Utilisateur - Joueur non trouvé)";
            }
            
            afficherNotificationSucces("Inscription réussie ! Connexion automatique..." + roleMessage);
            viderChamps();
            
            // Automatically login the user and redirect
            getUI().ifPresent(ui -> ui.navigate(""));
        } else {
            afficherNotificationErreur(result.getErrorMessage());
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
        champPrenom.clear();
        champNom.clear();
        champPrenom.setVisible(false);
        champNom.setVisible(false);
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
