package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import my.insa.yong.model.GestionMatchs;
import my.insa.yong.model.GestionMatchs.ButeurRow;
import my.insa.yong.model.GestionMatchs.GoalRow;
import my.insa.yong.model.GestionMatchs.JoueurRow;
import my.insa.yong.model.GestionMatchs.MatchRow;
import my.insa.yong.model.GestionMatchs.TeamRow;
import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.components.BaseLayout;

@Route("matchs")
@PageTitle("Matchs / Rounds")
public class VueMatch extends BaseLayout {

    private final Grid<MatchRow> grid = new Grid<>(MatchRow.class, false);
    private final Grid<ButeurRow> topGrid = new Grid<>(ButeurRow.class, false);

    // Saisie des buts (semi-auto)
    private final Grid<GoalRow> goalsGrid = new Grid<>(GoalRow.class, false);
    private final ComboBox<JoueurRow> buteurSelect = new ComboBox<>("Buteur (A ou B)");
    private final IntegerField minuteField = new IntegerField("Minute(s)");
    private final Button addGoalBtn = new Button("Ajouter but");
    private final Button clearGoalsBtn = new Button("Effacer tous les buts du match");

    // Tirage & reset
    private final Button drawRoundBtn = new Button("Tirage round suivant (aléatoire)");
    private final Button resetBtn = new Button("Réinitialiser les matchs");

    private final IntegerField scoreAField = new IntegerField("Score A (calculé)");
    private final IntegerField scoreBField = new IntegerField("Score B (calculé)");

    private H2 title;

    // cache pour retrouver TeamRow à partir de l'id (pour libellés)
    private final Map<Integer, TeamRow> teamById = new HashMap<>();

    // match sélectionné
    private MatchRow selectedMatch = null;

    public VueMatch() {
        String tournoiName = UserSession.getCurrentTournoiName();
        title = new H2("Matchs — " + tournoiName);

        // Boutons
        drawRoundBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        resetBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        addGoalBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        clearGoalsBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        // Masquer les boutons d'administration pour les utilisateurs normaux
        boolean isAdmin = UserSession.adminConnected();
        drawRoundBtn.setVisible(isAdmin);
        resetBtn.setVisible(isAdmin);
        addGoalBtn.setVisible(isAdmin);
        clearGoalsBtn.setVisible(isAdmin);

        drawRoundBtn.addClickListener(e -> onNextRound());
        resetBtn.addClickListener(e -> onReset());
        addGoalBtn.addClickListener(e -> onAddGoal());
        clearGoalsBtn.addClickListener(e -> onClearGoals());

        HorizontalLayout actions = new HorizontalLayout(drawRoundBtn, resetBtn);

        // --- Grille des matchs ---
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

        // Sélection d'un match => charge joueurs + buts
        grid.asSingleSelect().addValueChangeListener(ev -> {
            selectedMatch = ev.getValue();
            reloadScoringPanel();
        });

        // --- Classement des buteurs ---
        H3 topTitle = new H3("Classement des buteurs");
        topGrid.addColumn(ButeurRow::joueurNom).setHeader("Joueur").setAutoWidth(true);
        topGrid.addColumn(ButeurRow::equipeNom).setHeader("Équipe").setAutoWidth(true);
        topGrid.addColumn(ButeurRow::buts).setHeader("Buts").setAutoWidth(true);
        topGrid.setHeight("320px");
        grid.setSizeFull();

        // --- Panneau « Saisie des buteurs » --- (seulement pour admin)
        H3 scoringTitle = new H3("Saisie des buteurs (mode semi-auto)");
        scoringTitle.setVisible(isAdmin);

        buteurSelect.setItemLabelGenerator(j -> j.nom() + " — " + j.equipeNom());
        buteurSelect.setWidth("320px");
        buteurSelect.setVisible(isAdmin);

        minuteField.setMin(0);
        minuteField.setMax(130);
        minuteField.setPlaceholder("ex. 42");
        minuteField.setWidth("120px");
        minuteField.setVisible(isAdmin);

        scoreAField.setReadOnly(true);
        scoreBField.setReadOnly(true);
        scoreAField.setWidth("130px");
        scoreBField.setWidth("130px");

        HorizontalLayout addLine = new HorizontalLayout(buteurSelect, minuteField, addGoalBtn, clearGoalsBtn, scoreAField, scoreBField);
        addLine.setSpacing(true);
        addLine.setAlignItems(Alignment.END);
        addLine.setVisible(isAdmin);

        // Grid des buts
        goalsGrid.addColumn(GoalRow::minute).setHeader("Minute(s)").setAutoWidth(true);
        goalsGrid.addColumn(GoalRow::equipeNom).setHeader("Équipe").setAutoWidth(true);
        goalsGrid.addColumn(GoalRow::joueurNom).setHeader("Buteur").setAutoWidth(true).setFlexGrow(1);
        if (isAdmin) {
            goalsGrid.addComponentColumn(row -> {
                Button b = new Button("Supprimer", e -> onDeleteGoal(row.id()));
                b.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
                return b;
            }).setHeader("Action").setAutoWidth(true);
        }
        goalsGrid.setHeight("200px");

        // Empêche le "min-height:auto"
        grid.getElement().getStyle().set("min-height", "0");
        topGrid.getElement().getStyle().set("min-height", "0");
        goalsGrid.getElement().getStyle().set("min-height", "0");

        VerticalLayout scoringLayout = new VerticalLayout(scoringTitle, addLine, goalsGrid);
        scoringLayout.setPadding(false);
        scoringLayout.setSpacing(true);
        scoringLayout.setSizeFull();

        // Zone des grilles principales
        VerticalLayout gridsLayout = new VerticalLayout(grid, topTitle, topGrid);
        gridsLayout.setPadding(false);
        gridsLayout.setSpacing(true);
        gridsLayout.setSizeFull();
        gridsLayout.setFlexGrow(1, grid);
        gridsLayout.setFlexGrow(0, topTitle);
        gridsLayout.setFlexGrow(1, topGrid);

        // Layout principal
        VerticalLayout mainContent = new VerticalLayout(title, actions, scoringLayout, gridsLayout);
        mainContent.setSizeFull();
        mainContent.setFlexGrow(0, title, actions, scoringLayout);
        mainContent.setFlexGrow(1, gridsLayout);
        setContent(mainContent);

        refresh(); // charge grilles + combos
    }

    private void notifySql(SQLException ex) {
        Notification.show("Erreur SQL: " + ex.getMessage(), 3000, Notification.Position.TOP_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
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

            // Equipes (cache)
            teamById.clear();
            List<TeamRow> teams = GestionMatchs.listEquipes(con, tournoiId);
            for (TeamRow t : teams) teamById.put(t.id(), t);

            // Si un match est sélectionné, recharger son panneau
            reloadScoringPanel();

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
            notifySql(ex);
        }
    }

    private void reloadScoringPanel() {
        if (selectedMatch == null) {
            buteurSelect.clear();
            buteurSelect.setItems(List.of());
            goalsGrid.setItems(List.of());
            scoreAField.clear();
            scoreBField.clear();
            return;
        }
        try (Connection con = ConnectionPool.getConnection()) {
            // Lister joueurs des 2 équipes pour ce match
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            List<JoueurRow> joueurs = GestionMatchs.listPlayersForMatch(con, tournoiId, selectedMatch.id());
            buteurSelect.setItems(joueurs);

            // Lister les buts du match
            List<GoalRow> buts = GestionMatchs.listGoalsForMatch(con, tournoiId, selectedMatch.id());
            goalsGrid.setItems(buts);

            // Mettre à jour affichage des scores (déjà recalculés côté modèle)
            List<MatchRow> one = GestionMatchs.listAllMatches(con, UserSession.getCurrentTournoiId().orElse(1));
            // retrouver cette ligne
            one.stream().filter(m -> m.id() == selectedMatch.id()).findFirst().ifPresent(m -> {
                Integer scoreA = m.scoreA();
                Integer scoreB = m.scoreB();
                scoreAField.setValue(scoreA != null ? scoreA : 0);
                scoreBField.setValue(scoreB != null ? scoreB : 0);
                selectedMatch = m; // tenir en phase (played/winner)
            });
        } catch (SQLException ex) {
            notifySql(ex);
        }
    }

    private void onNextRound() {
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);

            // Si des matchs du round courant ne sont pas joués, prévenir plutôt que de les "jouer"
            int cur = GestionMatchs.getCurrentRound(con, tournoiId);
            if (cur > 0 && GestionMatchs.hasUnplayedMatchesPublic(con, tournoiId, cur)) {
                Notification n = Notification.show("Il reste des matchs non saisis au round " + cur + ".", 4000, Notification.Position.TOP_CENTER);
                n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                return;
            }

            GestionMatchs.generateNextRound(con, tournoiId);
            Notification.show("Round " + (cur + 1) + " généré.", 2500, Notification.Position.TOP_CENTER);
            refresh();
        } catch (SQLException ex) {
            notifySql(ex);
        }
    }

    private void onReset() {
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            // Replace resetTournoi with resetMatches
            GestionMatchs.resetMatches(con, tournoiId);
            refresh();
        } catch (SQLException ex) {
            notifySql(ex);
        }
    }

    private void onAddGoal() {
        if (selectedMatch == null || buteurSelect.getValue() == null || minuteField.getValue() == null) {
            Notification.show("Veuillez sélectionner un match, un buteur et une minute", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            // Get the team ID from the selected player
            JoueurRow joueur = buteurSelect.getValue();
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            // Use the tournament-aware addGoal method
            GestionMatchs.addGoal(con, tournoiId, selectedMatch.id(), joueur.equipeId(), joueur.id(), minuteField.getValue());
            reloadScoringPanel();
            refresh();  // Refresh to update top scorers
        } catch (SQLException ex) {
            notifySql(ex);
        }
    }

    private void onClearGoals() {
        if (selectedMatch == null) {
            Notification.show("Aucun match sélectionné", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            GestionMatchs.clearGoalsForMatch(con, tournoiId, selectedMatch.id());
            reloadScoringPanel();
            refresh();  // Refresh to update top scorers
        } catch (SQLException ex) {
            notifySql(ex);
        }
    }

    private void onDeleteGoal(int goalId) {
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            GestionMatchs.deleteGoal(con, tournoiId, goalId);
            reloadScoringPanel();
            refresh();  // Refresh to update top scorers
        } catch (SQLException ex) {
            notifySql(ex);
        }
    }
}