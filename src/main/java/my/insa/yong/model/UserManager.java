package my.insa.yong.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import my.insa.yong.utils.database.ConnectionPool;

/**
 * Utility class for user authentication and management
 * Centralizes all JDBC operations for user login/registration
 * @author saancern
 */
public class UserManager {

    /**
     * Data class for user login result
     */
    public static class LoginResult {
        private final boolean success;
        private final int userId;
        private final String username;
        private final boolean isAdmin;
        private final String errorMessage;

        public LoginResult(boolean success, int userId, String username, boolean isAdmin, String errorMessage) {
            this.success = success;
            this.userId = userId;
            this.username = username;
            this.isAdmin = isAdmin;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public boolean isAdmin() {
            return isAdmin;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Authenticate user with username and password
     */
    public static LoginResult authenticateUser(String username, String password) throws SQLException {
        String sql = "SELECT id, surnom, pass, isAdmin FROM utilisateur WHERE surnom = ?";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("pass");
                if (password.equals(storedPassword)) {
                    int userId = rs.getInt("id");
                    boolean isAdmin = rs.getBoolean("isAdmin");
                    return new LoginResult(true, userId, username, isAdmin, null);
                } else {
                    return new LoginResult(false, -1, null, false, "Mot de passe incorrect.");
                }
            } else {
                return new LoginResult(false, -1, null, false, "Utilisateur non trouvé. Veuillez vous inscrire.");
            }
        }
    }

    /**
     * Check if username already exists
     */
    public static boolean userExists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM utilisateur WHERE surnom = ?";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    /**
     * Register a new user
     */
    public static LoginResult registerUser(String username, String password, boolean isAdmin) throws SQLException {
        // Check if user already exists
        if (userExists(username)) {
            return new LoginResult(false, -1, null, false,
                    "Ce nom d'utilisateur existe déjà. Veuillez en choisir un autre ou vous connecter.");
        }

        // Create new user
        String sql = "INSERT INTO utilisateur (surnom, pass, isAdmin) VALUES (?, ?, ?)";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, username);
            pst.setString(2, password);
            pst.setBoolean(3, isAdmin);

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                // Get the generated user ID
                ResultSet generatedKeys = pst.getGeneratedKeys();
                int userId = -1;
                if (generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                }
                return new LoginResult(true, userId, username, isAdmin, null);
            } else {
                return new LoginResult(false, -1, null, false, "Échec de l'inscription. Veuillez réessayer.");
            }
        }
    }
}
