package my.insa.yong.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import my.insa.yong.utils.database.ConnectionPool;

/**
 * Classe représentant un utilisateur du système
 * Includes static methods for authentication and registration
 * @author saancern
 */
public class Utilisateur {
    
    private int id;
    private String surnom;
    private String pass;
    private boolean isAdmin; // Privilèges administrateur
    
    public Utilisateur() {
    }
    
    public Utilisateur(String surnom, String pass, boolean isAdmin) {
        this.surnom = surnom;
        this.pass = pass;
        this.isAdmin = isAdmin;
    }
    
    public Utilisateur(int id, String surnom, String pass, boolean isAdmin) {
        this.id = id;
        this.surnom = surnom;
        this.pass = pass;
        this.isAdmin = isAdmin;
    }
    
    // ============================================================================
    // INSTANCE GETTERS AND SETTERS
    // ============================================================================
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getSurnom() {
        return surnom;
    }
    
    public void setSurnom(String surnom) {
        this.surnom = surnom;
    }
    
    public String getPass() {
        return pass;
    }
    
    public void setPass(String pass) {
        this.pass = pass;
    }
    
    public boolean isAdmin() {
        return isAdmin;
    }
    
    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    
    /**
     * Get user role as integer (0 = user, 1 = admin)
     * Compatible with SessionInfo pattern
     */
    public int getRole() {
        return isAdmin ? 1 : 0;
    }
    
    @Override
    public String toString() {
        return "Utilisateur{" +
                "id=" + id +
                ", surnom='" + surnom + '\'' +
                ", isAdmin=" + isAdmin +
                '}';
    }
    
    // ============================================================================
    // STATIC AUTHENTICATION AND USER MANAGEMENT METHODS
    // ============================================================================
    
    /**
     * Result wrapper for login/registration operations
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
     * @param username the username
     * @param password the password
     * @return LoginResult containing authentication status and user information
     */
    public static LoginResult authenticateUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            return new LoginResult(false, -1, "", false, "Username and password are required");
        }

        String sql = "SELECT id, surnom, pass, isAdmin FROM utilisateur WHERE surnom = ?";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("pass");
                if (storedPassword != null && storedPassword.equals(password)) {
                    return new LoginResult(
                        true,
                        rs.getInt("id"),
                        rs.getString("surnom"),
                        rs.getBoolean("isAdmin"),
                        null
                    );
                } else {
                    return new LoginResult(false, -1, "", false, "Invalid password");
                }
            } else {
                return new LoginResult(false, -1, "", false, "User not found");
            }
        } catch (SQLException e) {
            return new LoginResult(false, -1, "", false, "Database error: " + e.getMessage());
        }
    }

    /**
     * Register new user in database
     * @param username the new username
     * @param password the password
     * @param isAdmin whether user has admin privileges
     * @return LoginResult containing registration status
     */
    public static LoginResult registerUser(String username, String password, boolean isAdmin) {
        if (username == null || username.trim().isEmpty()) {
            return new LoginResult(false, -1, "", false, "Username cannot be empty");
        }
        if (password == null || password.trim().isEmpty()) {
            return new LoginResult(false, -1, "", false, "Password cannot be empty");
        }

        // Check if user already exists
        if (userExists(username)) {
            return new LoginResult(false, -1, "", false, "User already exists");
        }

        String insertSql = "INSERT INTO utilisateur (surnom, pass, isAdmin) VALUES (?, ?, ?)";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, username);
            pst.setString(2, password);
            pst.setBoolean(3, isAdmin);
            pst.executeUpdate();

            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int newId = generatedKeys.getInt(1);
                    return new LoginResult(true, newId, username, isAdmin, null);
                }
            }
        } catch (SQLException e) {
            return new LoginResult(false, -1, "", false, "Registration failed: " + e.getMessage());
        }

        return new LoginResult(false, -1, "", false, "Registration failed");
    }

    /**
     * Check if user exists in database
     * @param username the username to check
     * @return true if user exists, false otherwise
     */
    public static boolean userExists(String username) {
        String sql = "SELECT COUNT(*) FROM utilisateur WHERE surnom = ?";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
        return false;
    }
}
