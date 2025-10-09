package my.insa.yong.model;

import com.vaadin.flow.server.VaadinSession;

/**
 * Session management for tracking logged-in users
 * @author saancern
 */
public class UserSession {
    
    private static final String USER_SESSION_KEY = "current_user";
    private static final String USER_ID_KEY = "user_id";
    private static final String USERNAME_KEY = "username";
    private static final String IS_ADMIN_KEY = "is_admin";
    
    /**
     * Set the current logged-in user in the session
     */
    public static void setCurrentUser(int userId, String username, boolean isAdmin) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(USER_ID_KEY, userId);
            session.setAttribute(USERNAME_KEY, username);
            session.setAttribute(IS_ADMIN_KEY, isAdmin);
            session.setAttribute(USER_SESSION_KEY, true);
        }
    }
    
    /**
     * Check if a user is currently logged in
     */
    public static boolean isUserLoggedIn() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            Boolean loggedIn = (Boolean) session.getAttribute(USER_SESSION_KEY);
            return loggedIn != null && loggedIn;
        }
        return false;
    }
    
    /**
     * Get the current user's ID
     */
    public static Integer getCurrentUserId() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null && isUserLoggedIn()) {
            return (Integer) session.getAttribute(USER_ID_KEY);
        }
        return null;
    }
    
    /**
     * Get the current user's username
     */
    public static String getCurrentUsername() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null && isUserLoggedIn()) {
            return (String) session.getAttribute(USERNAME_KEY);
        }
        return null;
    }
    
    /**
     * Check if the current user is an admin
     */
    public static boolean isCurrentUserAdmin() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null && isUserLoggedIn()) {
            Boolean isAdmin = (Boolean) session.getAttribute(IS_ADMIN_KEY);
            return isAdmin != null && isAdmin;
        }
        return false;
    }
    
    /**
     * Get the current user's role as a display string
     */
    public static String getCurrentUserRoleDisplay() {
        if (isUserLoggedIn()) {
            return isCurrentUserAdmin() ? "Administrateur" : "Utilisateur";
        }
        return "Invité";
    }
    
    /**
     * Clear the current user session (logout)
     */
    public static void clearCurrentUser() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(USER_SESSION_KEY, null);
            session.setAttribute(USER_ID_KEY, null);
            session.setAttribute(USERNAME_KEY, null);
            session.setAttribute(IS_ADMIN_KEY, null);
        }
    }
    
    /**
     * Get a complete user info summary
     */
    public static String getCurrentUserSummary() {
        if (isUserLoggedIn()) {
            String username = getCurrentUsername();
            String role = getCurrentUserRoleDisplay();
            return String.format("%s (%s)", username, role);
        }
        return "Aucun utilisateur connecté";
    }
}