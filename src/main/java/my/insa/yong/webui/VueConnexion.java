/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is part of CoursBeuvron.

CoursBeuvron is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoursBeuvron is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import my.insa.yong.utils.database.ConnectionSimpleSGBD;

/**
 * Page de connexion/inscription en français
 * @author francois
 */
@Route(value = "connexion")
@PageTitle("Connexion - Inscription")
public class VueConnexion extends VerticalLayout {
    
    private TextField champSurnom;
    private PasswordField champMotDePasse;
    private IntegerField champNiveauAcces;
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
        
        champNiveauAcces = new IntegerField("Niveau d'accès administratif");
        champNiveauAcces.setPlaceholder("Niveau d'accès (nombre non nul)");
        champNiveauAcces.setRequiredIndicatorVisible(true);
        champNiveauAcces.setWidth("300px");
        champNiveauAcces.setMin(1);
        champNiveauAcces.setValue(1);
        champNiveauAcces.setHelperText("Niveau 1 = Utilisateur standard, Niveau 2+ = Administrateur");
        
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
        
        formulaire.add(titre, sousTitre, champSurnom, champMotDePasse, champNiveauAcces, layoutBoutons);
        
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
            String sql = "SELECT id, surnom, pass, role FROM utilisateur WHERE surnom = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, surnom);
                ResultSet rs = pst.executeQuery();
                
                if (rs.next()) {
                    String passStocke = rs.getString("pass");
                    if (motDePasse.equals(passStocke)) {
                        int role = rs.getInt("role");
                        int niveauAccesSaisi = champNiveauAcces.getValue();
                        
                        if (role == niveauAccesSaisi) {
                            // Connexion réussie
                            afficherNotificationSucces("Connexion réussie ! Bienvenue " + surnom);
                            // TODO: Rediriger vers la page principale ou tableau de bord
                            getUI().ifPresent(ui -> ui.navigate(""));
                        } else {
                            afficherNotificationErreur("Niveau d'accès incorrect.");
                        }
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
        int niveauAcces = champNiveauAcces.getValue();
        
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
            String sqlInsert = "INSERT INTO utilisateur (surnom, pass, role) VALUES (?, ?, ?)";
            try (PreparedStatement pstInsert = con.prepareStatement(sqlInsert)) {
                pstInsert.setString(1, surnom);
                pstInsert.setString(2, motDePasse);
                pstInsert.setInt(3, niveauAcces);
                
                int rowsAffected = pstInsert.executeUpdate();
                
                if (rowsAffected > 0) {
                    afficherNotificationSucces("Inscription réussie ! Vous pouvez maintenant vous connecter.");
                    viderChamps();
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
        
        if (champNiveauAcces.getValue() == null || champNiveauAcces.getValue() <= 0) {
            afficherNotificationErreur("Le niveau d'accès doit être un nombre supérieur à 0.");
            champNiveauAcces.focus();
            return false;
        }
        
        return true;
    }
    
    private void viderChamps() {
        champSurnom.clear();
        champMotDePasse.clear();
        champNiveauAcces.setValue(1);
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