package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import my.insa.yong.model.GestionMatchs;
import my.insa.yong.model.GestionMatchs.MatchRow;
import my.insa.yong.model.UserSession;
import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.webui.components.BaseLayout;

@Route("matchs")
@PageTitle("Matchs / Rounds")
public class VueMatch extends BaseLayout {

    private final Grid<MatchRow> grid = new Grid<>(MatchRow.class, false);
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
        grid.setSizeFull();

        VerticalLayout content = new VerticalLayout(title, actions, grid);
        content.setSizeFull();
        setContent(content);

        refresh();
    }

    private void refresh() {
        try (Connection con = ConnectionPool.getConnection()) {
            int tournoiId = UserSession.getCurrentTournoiId().orElse(1);
            List<MatchRow> rows = GestionMatchs.listAllMatches(con, tournoiId);
            grid.setItems(rows);

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
}
