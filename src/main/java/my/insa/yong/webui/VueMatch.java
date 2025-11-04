package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import my.insa.yong.model.GestionMatchs;
import my.insa.yong.model.GestionMatchs.ButeurRow;
import my.insa.yong.model.GestionMatchs.MatchRow;
import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.components.BaseLayout;

@Route("matchs")
@PageTitle("Matchs / Rounds")
public class VueMatch extends BaseLayout {

    private final Grid<MatchRow> grid = new Grid<>(MatchRow.class, false);
    private final Grid<ButeurRow> topGrid = new Grid<>(ButeurRow.class, false);

    private final Button nextRoundBtn = new Button("Round suivante");
    private final Button resetBtn = new Button("Réinitialiser les matchs");
    private H2 title;

    public VueMatch() {
        String tournoiName = UserSession.getCurrentTournoiName();
        title = new H2("Matchs — " + tournoiName);

        nextRoundBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        resetBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

        nextRoundBtn.addClickListener(e -> onNextRound());
        resetBtn.addClickListener(e -> onReset());

        HorizontalLayout actions = new HorizontalLayout(nextRoundBtn, resetBtn);

        // --- Grille des matchs (ajout des colonnes buteurs) ---
        grid.addColumn(MatchRow::round).setHeader("Round").setAutoWidth(true);
        grid.addColumn(r -> r.poolIndex() == null ? "—" : String.valueOf(r.poolIndex()))
            .setHeader("Pool").setAutoWidth(true);
        grid.addColumn(MatchRow::equipeAName).setHeader("Équipe A").setAutoWidth(true);
        grid.addColumn(r -> r.scoreA() == null ? "—" : String.valueOf(r.scoreA()))
            .setHeader("Score A").setAutoWidth(true);
        grid.addColumn(MatchRow::equipeBName).setHeader("Équipe B").setAutoWidth(true);
        grid.addColumn(r -> r.scoreB() == null ? "—" : String.valueOf(r.scoreB()))
            .setHeader("Score B").setAutoWidth(true);
        grid.addColumn(MatchRow::winnerName).setHeader("Gagnant").setAutoWidth(true);
        grid.addColumn(r -> r.played() ? "Oui" : "Non").setHeader("Joué").setAutoWidth(true);
        grid.addColumn(MatchRow::buteursA).setHeader("Buteurs A").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(MatchRow::buteursB).setHeader("Buteurs B").setAutoWidth(true).setFlexGrow(1);
        grid.setSizeFull();

        // --- Classement des buteurs ---
        H3 topTitle = new H3("Classement des buteurs");
        topGrid.addColumn(ButeurRow::joueurNom).setHeader("Joueur").setAutoWidth(true);
        topGrid.addColumn(ButeurRow::equipeNom).setHeader("Équipe").setAutoWidth(true);
        topGrid.addColumn(ButeurRow::buts).setHeader("Buts").setAutoWidth(true);
        topGrid.setAllRowsVisible(true);
        grid.setSizeFull();
        topGrid.setSizeFull();

        // Empêche le "min-height:auto" qui casse le flex sous Chrome
        grid.getElement().getStyle().set("min-height", "0");
        topGrid.getElement().getStyle().set("min-height", "0");

        // Mets les deux grilles dans un conteneur dédié qui prend tout l’espace
        VerticalLayout gridsArea = new VerticalLayout(grid, topTitle, topGrid);
        gridsArea.setPadding(false);
        gridsArea.setSpacing(true);
        gridsArea.setSizeFull();

        // 50/50 entre les deux grilles (le H3 ne s’étire pas)
        gridsArea.setFlexGrow(1, grid);
        gridsArea.setFlexGrow(0, topTitle);
        gridsArea.setFlexGrow(1, topGrid);

        // Recompose le layout principal
        VerticalLayout content = new VerticalLayout(title, actions, gridsArea);
        content.setSizeFull();
        content.setFlexGrow(1, gridsArea);      // tout l’espace dispo va à la zone des grilles
        content.setFlexGrow(0, title, actions); // titres/boutons gardent leur taille
        setContent(content);
        refresh();
    }

    private void refresh() {
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

            // Matchs + buteurs par match
            List<MatchRow> rows = GestionMatchs.listAllMatches(con, tournoiId);
            grid.setItems(rows);

            // Top buteurs (sur tout le tournoi courant)
            List<ButeurRow> top = GestionMatchs.getTopScorers(con, tournoiId, 50);
            topGrid.setItems(top);

            Optional<Integer> championId = GestionMatchs.getChampion(con, tournoiId);
            if (championId.isPresent()) {
                String champion = rows.stream()
                        .filter(r -> r.winnerId() != null && r.winnerId().equals(championId.get()))
                        .map(MatchRow::winnerName)
                        .filter(n -> n != null && !n.isBlank())
                        .findFirst()
                        .orElse("Champion");
                Notification n = Notification.show("🏆 Champion: " + champion, 4000, Notification.Position.TOP_CENTER);
                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
        } catch (SQLException ex) {
            Notification n = Notification.show("Erreur: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void onNextRound() {
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            
            // Vérifier s'il y a des matchs en cours qui nécessitent la saisie des scores
            List<MatchRow> unplayedMatches = getUnplayedMatches(con, tournoiId);
            
            if (!unplayedMatches.isEmpty()) {
                // Afficher le popup pour modifier les scores
                showScoreModificationDialog(unplayedMatches, () -> {
                    // Callback appelé après confirmation des scores
                    proceedWithNextRound();
                });
            } else {
                // Aucun match en attente, procéder directement
                proceedWithNextRound();
            }
            
        } catch (SQLException ex) {
            Notification n = Notification.show("Erreur: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void proceedWithNextRound() {
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            String result = GestionMatchs.nextStep(con, tournoiId);
            Notification.show(result, 3000, Notification.Position.TOP_CENTER);
            refresh();
        } catch (SQLException ex) {
            Notification n = Notification.show("Erreur: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void onReset() {
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            GestionMatchs.resetMatches(con, tournoiId);
            Notification n = Notification.show("Matchs réinitialisés pour ce tournoi.", 3000, Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            refresh();
        } catch (SQLException ex) {
            Notification n = Notification.show("Erreur: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private List<MatchRow> getUnplayedMatches(Connection con, int tournoiId) throws SQLException {
        List<MatchRow> allMatches = GestionMatchs.listAllMatches(con, tournoiId);
        return allMatches.stream()
                .filter(match -> !match.played() && 
                        match.equipeAId() != null && 
                        match.equipeBId() != null)
                .toList();
    }

    private void showScoreModificationDialog(List<MatchRow> unplayedMatches, Runnable onConfirm) {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.addClassName("score-dialog");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.addClassName("score-dialog-content");

        H4 title = new H4("Modification des scores");
        title.addClassName("score-dialog-title");
        content.add(title);

        // Container pour les matchs
        VerticalLayout matchesContainer = new VerticalLayout();
        matchesContainer.setPadding(false);
        matchesContainer.setSpacing(true);

        // Map pour stocker les champs de score
        java.util.Map<Integer, IntegerField> scoreAFields = new java.util.HashMap<>();
        java.util.Map<Integer, IntegerField> scoreBFields = new java.util.HashMap<>();
        
        // Générateur de nombres aléatoires pour les scores pré-remplis
        java.util.Random random = new java.util.Random();

        for (MatchRow match : unplayedMatches) {
            HorizontalLayout matchLayout = new HorizontalLayout();
            matchLayout.setWidthFull();
            matchLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            matchLayout.setSpacing(true);
            matchLayout.addClassName("score-match-row");

            // Générer des scores aléatoires (0-5) pour pré-remplir
            int randomScoreA = random.nextInt(6); // 0 à 5
            int randomScoreB;
            do {
                randomScoreB = random.nextInt(6); // 0 à 5
            } while (randomScoreB == randomScoreA); // Éviter les égalités

            // Nom de l'équipe A
            VerticalLayout teamALayout = new VerticalLayout();
            teamALayout.setPadding(false);
            teamALayout.setSpacing(false);
            teamALayout.setAlignItems(FlexComponent.Alignment.CENTER);
            teamALayout.addClassName("score-team-container");
            H4 teamAName = new H4(match.equipeAName());
            teamAName.addClassName("score-team-name");
            IntegerField scoreAField = new IntegerField();
            scoreAField.setValue(match.scoreA() != null ? match.scoreA() : randomScoreA);
            scoreAField.setMin(0);
            scoreAField.setWidth("80px");
            scoreAField.addClassName("score-input");
            teamALayout.add(teamAName, scoreAField);
            scoreAFields.put(match.id(), scoreAField);

            // VS
            H4 vs = new H4("VS");
            vs.addClassName("score-vs");

            // Nom de l'équipe B
            VerticalLayout teamBLayout = new VerticalLayout();
            teamBLayout.setPadding(false);
            teamBLayout.setSpacing(false);
            teamBLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            teamBLayout.addClassName("score-team-container");
            H4 teamBName = new H4(match.equipeBName());
            teamBName.addClassName("score-team-name");
            IntegerField scoreBField = new IntegerField();
            scoreBField.setValue(match.scoreB() != null ? match.scoreB() : randomScoreB);
            scoreBField.setMin(0);
            scoreBField.setWidth("80px");
            scoreBField.addClassName("score-input");
            teamBLayout.add(teamBName, scoreBField);
            scoreBFields.put(match.id(), scoreBField);

            matchLayout.add(teamALayout, vs, teamBLayout);
            matchLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

            matchesContainer.add(matchLayout);
        }

        content.add(matchesContainer);

        // Boutons
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.addClassName("score-dialog-buttons");
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setSpacing(true);

        Button cancelButton = new Button("Annuler", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button confirmButton = new Button("Confirmer et fermer les matchs", e -> {
            saveMatchScores(unplayedMatches, scoreAFields, scoreBFields);
            dialog.close();
            onConfirm.run();
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        buttonLayout.add(cancelButton, confirmButton);
        content.add(buttonLayout);

        dialog.add(content);
        dialog.open();
    }

    private void saveMatchScores(List<MatchRow> matches, 
                                java.util.Map<Integer, IntegerField> scoreAFields,
                                java.util.Map<Integer, IntegerField> scoreBFields) {
        try (Connection con = ConnectionPool.getConnection()) {
            con.setAutoCommit(false);
            
            String updateSQL = "UPDATE rencontre SET score_a = ?, score_b = ?, winner_id = ?, played = true WHERE id = ?";
            
            try (PreparedStatement pstmt = con.prepareStatement(updateSQL)) {
                for (MatchRow match : matches) {
                    IntegerField scoreAField = scoreAFields.get(match.id());
                    IntegerField scoreBField = scoreBFields.get(match.id());
                    
                    Integer scoreA = scoreAField.getValue();
                    Integer scoreB = scoreBField.getValue();
                    
                    if (scoreA == null) scoreA = 0;
                    if (scoreB == null) scoreB = 0;
                    
                    // Déterminer le gagnant (pas d'égalité autorisée)
                    Integer winnerId;
                    if (scoreA > scoreB) {
                        winnerId = match.equipeAId();
                    } else if (scoreB > scoreA) {
                        winnerId = match.equipeBId();
                    } else {
                        // En cas d'égalité, donner la victoire à l'équipe A par défaut
                        // Ou vous pourriez demander à l'utilisateur de modifier
                        winnerId = match.equipeAId();
                        scoreA = scoreA + 1; // Ajuster le score pour éviter l'égalité
                    }
                    
                    pstmt.setInt(1, scoreA);
                    pstmt.setInt(2, scoreB);
                    pstmt.setInt(3, winnerId);
                    pstmt.setInt(4, match.id());
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                con.commit();
                
                Notification.show("Scores mis à jour avec succès!", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
            
        } catch (SQLException ex) {
            Notification n = Notification.show("Erreur lors de la sauvegarde: " + ex.getMessage(), 
                                             4000, Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
