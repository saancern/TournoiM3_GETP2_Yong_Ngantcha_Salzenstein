package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
    private NumberField tailleField;
    private Button ajouterButton;
    private Grid<Joueur> joueursGrid;

    public VueJoueur() {
        this.addClassName("app-container");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Créer le layout principal horizontal (gauche/droite)
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(true);

        // Partie gauche - Formulaire d'ajout
        VerticalLayout leftPanel = new VerticalLayout();
        leftPanel.addClassName("form-container");
        leftPanel.addClassName("fade-in");
        leftPanel.setWidth("400px");
        leftPanel.setSpacing(true);
        leftPanel.setPadding(true);

        H2 titreForm = new H2("Ajouter un joueur");
        titreForm.addClassName("page-title");

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

        tailleField = new NumberField("Taille (cm)");
        tailleField.setPlaceholder("Entrez la taille du joueur en cm");
        tailleField.setRequired(true);
        tailleField.setMin(50);
        tailleField.setMax(250);
        tailleField.addClassName("form-field");
        tailleField.setWidthFull();

        ajouterButton = new Button("Ajouter");
        ajouterButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        ajouterButton.addClassName("btn-primary");
        ajouterButton.addClickListener(e -> ajouterJoueur());
        ajouterButton.setWidthFull();

        leftPanel.add(titreForm, prenomField, nomField, tailleField, ajouterButton);

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
    }

    private void ajouterJoueur() {
        String prenom = prenomField.getValue().trim();
        String nom = nomField.getValue().trim();
        Double taille = tailleField.getValue();

        if (prenom.isEmpty() || nom.isEmpty() || taille == null || taille <= 0) {
            Notification notification = Notification.show("Veuillez remplir tous les champs correctement.", 3000, Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            String sql = "INSERT INTO joueur (prenom, nom, taille) VALUES (?, ?, ?)";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, prenom);
                pst.setString(2, nom);
                pst.setDouble(3, taille);
                
                int rows = pst.executeUpdate();
                if (rows > 0) {
                    Notification notification = Notification.show("Joueur ajouté avec succès !", 3000, Notification.Position.TOP_CENTER);
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    prenomField.clear();
                    nomField.clear();
                    tailleField.clear();
                    // Recharger la liste des joueurs
                    chargerJoueurs();
                } else {
                    Notification notification = Notification.show("Échec de l'ajout du joueur.", 3000, Notification.Position.TOP_CENTER);
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        } catch (SQLException ex) {
            Notification notification = Notification.show("Erreur lors de l'ajout : " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Charge tous les joueurs depuis la base de données et les affiche dans le tableau
     */
    private void chargerJoueurs() {
        List<Joueur> joueurs = new ArrayList<>();
        
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            String sql = "SELECT id, prenom, nom, taille FROM joueur ORDER BY nom, prenom";
            try (PreparedStatement pst = con.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                
                while (rs.next()) {
                    Joueur joueur = new Joueur(
                        rs.getInt("id"),
                        rs.getString("prenom"),
                        rs.getString("nom"),
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
