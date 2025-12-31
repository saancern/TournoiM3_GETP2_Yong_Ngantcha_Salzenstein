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
    private Integer joueurId; // Lien vers joueur (null si non joueur)
    
    public Utilisateur() {
    }
    
    public Utilisateur(String surnom, String pass, boolean isAdmin) {
        this.surnom = surnom;
        this.pass = pass;
        this.isAdmin = isAdmin;
        this.joueurId = null;
    }
    
    public Utilisateur(int id, String surnom, String pass, boolean isAdmin) {
        this.id = id;
        this.surnom = surnom;
        this.pass = pass;
        this.isAdmin = isAdmin;
        this.joueurId = null;
    }
    
    public Utilisateur(int id, String surnom, String pass, boolean isAdmin, Integer joueurId) {
        this.id = id;
        this.surnom = surnom;
        this.pass = pass;
        this.isAdmin = isAdmin;
        this.joueurId = joueurId;
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
    
    public Integer getJoueurId() {
        return joueurId;
    }
    
    public void setJoueurId(Integer joueurId) {
        this.joueurId = joueurId;
    }
    
    /**
     * Get user role as integer (0 = user, 1 = admin, 3 = joueur)
     * Compatible with SessionInfo pattern
     */
    public int getRole() {
        if (isAdmin) {
            return 1; // Admin
        } else if (joueurId != null) {
            return 3; // Joueur
        } else {
            return 2; // Utilisateur simple
        }
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

        // Try with joueur_id first, fallback to old schema if column doesn't exist
        String sql = "SELECT id, surnom, pass, isAdmin FROM utilisateur WHERE surnom = ?";
        Integer joueurId = null;
        
        try (Connection con = ConnectionPool.getConnection()) {
            // Check if joueur_id column exists
            boolean hasJoueurIdColumn = false;
            try (PreparedStatement checkPst = con.prepareStatement("SELECT joueur_id FROM utilisateur WHERE 1=0")) {
                checkPst.executeQuery();
                hasJoueurIdColumn = true;
            } catch (SQLException e) {
                // Column doesn't exist - use old schema
            }
            
            if (hasJoueurIdColumn) {
                sql = "SELECT id, surnom, pass, isAdmin, joueur_id FROM utilisateur WHERE surnom = ?";
            }
            
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, username);
                ResultSet rs = pst.executeQuery();

                if (rs.next()) {
                    String storedPassword = rs.getString("pass");
                    
                    if (storedPassword.equals(password)) {
                        int userId = rs.getInt("id");
                        boolean isAdmin = rs.getBoolean("isAdmin");
                        
                        // Read joueur_id if column exists
                        if (hasJoueurIdColumn) {
                            joueurId = rs.getObject("joueur_id", Integer.class);
                        }
                        
                        // Create full user object
                        Utilisateur user = new Utilisateur(userId, username, password, isAdmin, joueurId);
                        
                        return new LoginResult(true, userId, username, isAdmin, null);
                    } else {
                        return new LoginResult(false, -1, "", false, "Invalid password");
                    }
                } else {
                    return new LoginResult(false, -1, "", false, "User not found");
                }
            }
        } catch (SQLException e) {
            return new LoginResult(false, -1, "", false, "Database error: " + e.getMessage());
        }
    }

    /**
     * Register new user in database with auto-detection of joueur
     * @param username the new username
     * @param password the password
     * @param prenom first name to search for joueur match
     * @param nom last name to search for joueur match
     * @param isAdmin whether user has admin privileges
     * @return LoginResult containing registration status
     */
    public static LoginResult registerUser(String username, String password, String prenom, String nom, boolean isAdmin) {
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

        try (Connection con = ConnectionPool.getConnection()) {
            // Search for matching joueur by prenom + nom
            Integer joueurId = null;
            if (prenom != null && !prenom.trim().isEmpty() && nom != null && !nom.trim().isEmpty()) {
                String searchJoueurSql = "SELECT id FROM joueur WHERE LOWER(prenom) = LOWER(?) AND LOWER(nom) = LOWER(?) LIMIT 1";
                try (PreparedStatement pst = con.prepareStatement(searchJoueurSql)) {
                    pst.setString(1, prenom.trim());
                    pst.setString(2, nom.trim());
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        joueurId = rs.getInt("id");
                    }
                }
            }

            // Insert user with joueur_id if found
            String insertSql = "INSERT INTO utilisateur (surnom, pass, isAdmin, joueur_id) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pst = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                pst.setString(1, username);
                pst.setString(2, password);
                pst.setBoolean(3, isAdmin);
                if (joueurId != null) {
                    pst.setInt(4, joueurId);
                } else {
                    pst.setNull(4, java.sql.Types.INTEGER);
                }
                pst.executeUpdate();

                try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int newId = generatedKeys.getInt(1);
                        return new LoginResult(true, newId, username, isAdmin, null);
                    }
                }
            }
        } catch (SQLException e) {
            return new LoginResult(false, -1, "", false, "Registration failed: " + e.getMessage());
        }

        return new LoginResult(false, -1, "", false, "Registration failed");
    }

    /**
     * Legacy registration method (for compatibility)
     */
    public static LoginResult registerUser(String username, String password, boolean isAdmin) {
        return registerUser(username, password, null, null, isAdmin);
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
