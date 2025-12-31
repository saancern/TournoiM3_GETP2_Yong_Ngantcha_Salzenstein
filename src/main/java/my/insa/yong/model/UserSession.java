package my.insa.yong.model;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.vaadin.flow.server.VaadinSession;

import my.insa.yong.utils.database.ConnectionPool;

/**
 * Modern Vaadin session management for user authentication
 * @author saancern
 */
public class UserSession implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Utilisateur curUser;
    private Integer currentTournoiId;
    private String currentTournoiName;
    
    /**
     * Get or create the current session info
     */
    public static UserSession getOrCreate() {
        VaadinSession curSession = VaadinSession.getCurrent();
        UserSession curInfo = curSession.getAttribute(UserSession.class);
        if (curInfo == null) {
            curInfo = new UserSession();
            curSession.setAttribute(UserSession.class, curInfo);
        }
        return curInfo;
    }
    
    /**
     * Login a user
     */
    public static void login(Utilisateur u) {
        UserSession curInfo = getOrCreate();
        curInfo.curUser = u;
    }
    
    /**
     * Logout the current user
     */
    public static void logout() {
        UserSession curInfo = getOrCreate();
        curInfo.curUser = null;
    }
    
    /**
     * Get the current user as Optional
     */
    public static Optional<Utilisateur> curUser() {
        Utilisateur u = getOrCreate().curUser;
        if (u == null) {
            return Optional.empty();
        } else {
            return Optional.of(u);
        }
    }
    
    /**
     * Check if a user is connected
     */
    public static boolean userConnected() {
        return curUser().isPresent();
    }
    
    /**
     * Check if an admin is connected
     */
    public static boolean adminConnected() {
        Optional<Utilisateur> curUser = curUser();
        if (curUser.isEmpty()) {
            return false;
        } else {
            return curUser.get().getRole() == 1;
        }
    }
    
    /**
     * Check if a joueur is connected (role = 3)
     */
    public static boolean joueurConnected() {
        Optional<Utilisateur> curUser = curUser();
        if (curUser.isEmpty()) {
            return false;
        } else {
            return curUser.get().getRole() == 3;
        }
    }
    
    /**
     * Get current user's joueur_id (null if not a joueur)
     */
    public static Integer getCurrentJoueurId() {
        Optional<Utilisateur> user = curUser();
        return user.map(Utilisateur::getJoueurId).orElse(null);
    }
    
    /**
     * Get current user ID
     */
    public static Integer getCurrentUserId() {
        Optional<Utilisateur> user = curUser();
        return user.map(Utilisateur::getId).orElse(null);
    }
    
    /**
     * Get current username
     */
    public static String getCurrentUsername() {
        Optional<Utilisateur> user = curUser();
        return user.map(Utilisateur::getSurnom).orElse(null);
    }
    
    /**
     * Get current user role as display string
     */
    public static String getCurrentUserRoleDisplay() {
        if (userConnected()) {
            int role = curUser().get().getRole();
            return switch (role) {
                case 1 -> "Administrateur";
                case 3 -> "Joueur";
                default -> "Utilisateur";
            };
        }
        return "Invité";
    }
    
    /**
     * Get user summary
     */
    public static String getCurrentUserSummary() {
        if (userConnected()) {
            String username = getCurrentUsername();
            String role = getCurrentUserRoleDisplay();
            return String.format("%s (%s)", username, role);
        }
        return "Aucun utilisateur connecté";
    }
    
    /**
     * Set current user (reloads from database to get joueur_id)
     */
    public static void setCurrentUser(int userId, String username, boolean isAdmin) {
        // Reload user from database to get joueur_id
        try (Connection con = ConnectionPool.getConnection()) {
            String sql = "SELECT id, surnom, pass, isAdmin, joueur_id FROM utilisateur WHERE id = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, userId);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    Integer joueurId = rs.getObject("joueur_id", Integer.class);
                    Utilisateur user = new Utilisateur(userId, username, "", isAdmin, joueurId);
                    login(user);
                    return;
                }
            }
        } catch (SQLException e) {
            // Fallback to basic user without joueur_id
        }
        
        // Fallback if database load fails
        Utilisateur user = new Utilisateur(userId, username, "", isAdmin);
        login(user);
    }
    
    // ================== MÉTHODES POUR LE TOURNOI ACTUEL ==================
    
    /**
     * Définir le tournoi actuel
     */
    public static void setCurrentTournoi(Integer tournoiId, String tournoiName) {
        UserSession session = getOrCreate();
        session.currentTournoiId = tournoiId;
        session.currentTournoiName = tournoiName;
    }
    
    /**
     * Obtenir l'ID du tournoi actuel
     */
    public static Optional<Integer> getCurrentTournoiId() {
        UserSession session = getOrCreate();
        return Optional.ofNullable(session.currentTournoiId);
    }
    
    /**
     * Obtenir le nom du tournoi actuel (Optional)
     */
    public static Optional<String> getCurrentTournoiNameOpt() {
        UserSession session = getOrCreate();
        return Optional.ofNullable(session.currentTournoiName);
    }
    
    /**
     * Obtenir le nom du tournoi actuel ou charger le tournoi par défaut
     */
    public static String getCurrentTournoiName() {
        UserSession session = getOrCreate();
        if (session.currentTournoiName == null) {
            // Charger automatiquement le tournoi par défaut (ID=1)
            chargerTournoiParDefaut(session);
        }
        return session.currentTournoiName != null ? session.currentTournoiName : "Tournoi";
    }
    
    /**
     * Vérifier si un tournoi est sélectionné
     */
    public static boolean hasCurrentTournoi() {
        return getCurrentTournoiId().isPresent();
    }
    
    /**
     * Effacer le tournoi actuel
     */
    public static void clearCurrentTournoi() {
        UserSession session = getOrCreate();
        session.currentTournoiId = null;
        session.currentTournoiName = null;
    }
    
    /**
     * Obtenir un résumé utilisateur + tournoi
     */
    public static String getCurrentUserAndTournoiSummary() {
        String userInfo = getCurrentUserSummary();
        String tournoiInfo = getCurrentTournoiName();
        return String.format("%s | Tournoi: %s", userInfo, tournoiInfo);
    }
    
    /**
     * Obtenir le sport du tournoi actuel
     */
    public static String getCurrentTournoiSport() {
        try (Connection con = ConnectionPool.getConnection()) {
            UserSession session = getOrCreate();
            if (session.currentTournoiId != null) {
                Parametre tournoi = Parametre.getParametreById(con, session.currentTournoiId);
                if (tournoi != null) {
                    return tournoi.getSport();
                }
            }
            // Fallback vers le tournoi par défaut
            Parametre tournoiDefaut = Parametre.getParametreById(con, 1);
            return tournoiDefaut != null ? tournoiDefaut.getSport() : "Foot";
        } catch (SQLException ex) {
            return "Foot"; // Valeur par défaut en cas d'erreur
        }
    }
    
    /**
     * Obtenir le nombre de joueurs par équipe du tournoi actuel
     */
    public static int getCurrentTournoiNombreJoueursParEquipe() {
        try (Connection con = ConnectionPool.getConnection()) {
            UserSession session = getOrCreate();
            if (session.currentTournoiId != null) {
                Parametre tournoi = Parametre.getParametreById(con, session.currentTournoiId);
                if (tournoi != null) {
                    return tournoi.getNombreJoueursParEquipe();
                }
            }
            // Fallback vers le tournoi par défaut
            Parametre tournoiDefaut = Parametre.getParametreById(con, 1);
            return tournoiDefaut != null ? tournoiDefaut.getNombreJoueursParEquipe() : 11;
        } catch (SQLException ex) {
            return 11; // Valeur par défaut en cas d'erreur
        }
    }
    
    /**
     * Obtenir le nombre de terrains du tournoi actuel
     */
    public static int getCurrentTournoiNombreTerrains() {
        try (Connection con = ConnectionPool.getConnection()) {
            UserSession session = getOrCreate();
            if (session.currentTournoiId != null) {
                Parametre tournoi = Parametre.getParametreById(con, session.currentTournoiId);
                if (tournoi != null) {
                    return tournoi.getNombreTerrains();
                }
            }
            // Fallback vers le tournoi par défaut
            Parametre tournoiDefaut = Parametre.getParametreById(con, 1);
            return tournoiDefaut != null ? tournoiDefaut.getNombreTerrains() : 10;
        } catch (SQLException ex) {
            return 10; // Valeur par défaut en cas d'erreur
        }
    }
    
    /**
     * Charger le tournoi par défaut (ID=1) depuis la base de données
     */
    private static void chargerTournoiParDefaut(UserSession session) {
        try (Connection con = ConnectionPool.getConnection()) {
            Parametre tournoiDefaut = Parametre.getParametreById(con, 1);
            if (tournoiDefaut != null) {
                session.currentTournoiId = tournoiDefaut.getId();
                session.currentTournoiName = tournoiDefaut.getNomTournoi();
            } else {
                // Fallback si aucun tournoi ID=1 n'existe
                session.currentTournoiId = 1;
                session.currentTournoiName = "Tournoi";
            }
        } catch (SQLException ex) {
            // En cas d'erreur, utiliser des valeurs par défaut
            session.currentTournoiId = 1;
            session.currentTournoiName = "Tournoi";
        }
    }
}