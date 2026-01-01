package my.insa.yong.utils.avatar;

import java.sql.Connection;

import my.insa.yong.utils.database.ConnectionPool;
import my.insa.yong.model.UserSession; // optionnel si tu veux récupérer le tournoi courant

public class AvatarSeederRunner {

    public static void main(String[] args) {
        int tournoiId = 1; // <-- mets l'id du tournoi que tu veux remplir
        if (args.length >= 1) {
            tournoiId = Integer.parseInt(args[0]);
        }

        try (Connection con = ConnectionPool.getConnection()) {
            con.setAutoCommit(false);

            AvatarSeeder.remplirPhotosManquantes(con, tournoiId);

            con.commit();
            System.out.println("✅ Photos remplies pour le tournoi " + tournoiId);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Erreur : " + e.getMessage());
        }
    }
}
