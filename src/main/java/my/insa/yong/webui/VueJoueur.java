package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import my.insa.yong.utils.database.ConnectionSimpleSGBD;

@Route("joueur")
@PageTitle("Ajouter un joueur")
public class VueJoueur extends VerticalLayout {

    private TextField prenomField;
    private TextField nomField;
    private NumberField tailleField;
    private Button ajouterButton;

    public VueJoueur() {
        setAlignItems(Alignment.CENTER);
        setSpacing(true);
        setPadding(true);
        setSizeFull();

        H2 titre = new H2("Ajouter un joueur");
        titre.getStyle().set("color", "#2c3e50");

        prenomField = new TextField("Prénom");
        prenomField.setPlaceholder("Entrez le prénom du joueur");
        prenomField.setRequired(true);
        prenomField.setWidth("300px");

        nomField = new TextField("Nom");
        nomField.setPlaceholder("Entrez le nom du joueur");
        nomField.setRequired(true);
        nomField.setWidth("300px");

        tailleField = new NumberField("Taille (cm)");
        tailleField.setPlaceholder("Entrez la taille du joueur en centimètres");
        tailleField.setRequired(true);
        tailleField.setMin(50);
        tailleField.setMax(250);
        tailleField.setWidth("300px");

        ajouterButton = new Button("Ajouter");
        ajouterButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        ajouterButton.setWidth("140px");
        ajouterButton.addClickListener(e -> ajouterJoueur());

        add(titre, prenomField, nomField, tailleField, ajouterButton);
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
}
