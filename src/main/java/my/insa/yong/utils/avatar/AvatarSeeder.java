package my.insa.yong.utils.avatar;

import java.net.URI;
import java.net.http.*;
import java.sql.*;
import java.util.*;

public class AvatarSeeder {

  public static void remplirPhotosManquantes(Connection con, int tournoiId) throws Exception {
    HttpClient client = HttpClient.newHttpClient();

    // Choisis un style DiceBear (ex: thumbs) + format png
    // API doc: formats + limites :contentReference[oaicite:7]{index=7}
    String base = "https://api.dicebear.com/9.x/personas/png?seed=";

    List<Integer> ids = new ArrayList<>();
    try (PreparedStatement pst = con.prepareStatement(
        "SELECT id FROM joueur WHERE tournoi_id=?")) {
      pst.setInt(1, tournoiId);
      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) ids.add(rs.getInt(1));
      }
    }

    try (PreparedStatement up = con.prepareStatement(
        "UPDATE joueur SET photo=?, photo_mime=?, photo_nom=? WHERE id=? AND tournoi_id=?")) {

      for (int id : ids) {
        String seed = "joueur-" + id;

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(base + java.net.URLEncoder.encode(seed, java.nio.charset.StandardCharsets.UTF_8)))
            .GET()
            .build();

        byte[] png = client.send(req, HttpResponse.BodyHandlers.ofByteArray()).body();

        up.setBytes(1, png);
        up.setString(2, "image/png");
        up.setString(3, "dicebear-" + seed + ".png");
        up.setInt(4, id);
        up.setInt(5, tournoiId);
        up.addBatch();
      }
      up.executeBatch();
    }
  }
}
