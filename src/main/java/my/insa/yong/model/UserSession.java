package my.insa.yong.model;

import com.vaadin.flow.server.VaadinSession;
import java.io.Serializable;
import java.util.Optional;

/**
 * Modern Vaadin session management for user authentication
 * @author saancern
 */
public class UserSession implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Utilisateur curUser;
    
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
            return adminConnected() ? "Administrateur" : "Utilisateur";
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
     * Set current user (for backward compatibility)
     */
    public static void setCurrentUser(int userId, String username, boolean isAdmin) {
        Utilisateur user = new Utilisateur(userId, username, "", isAdmin);
        login(user);
    }
}