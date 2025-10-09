package my.insa.yong.vaadin.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Utility class for consistent styling across the application
 * @author saancern
 */
public class StyleUtils {
    
    // CSS Class Constants
    public static final String APP_CONTAINER = "app-container";
    public static final String CENTERED_LAYOUT = "centered-layout";
    public static final String FORM_CONTAINER = "form-container";
    public static final String FORM_CONTAINER_LARGE = "form-container-large";
    public static final String PAGE_TITLE = "page-title";
    public static final String PAGE_SUBTITLE = "page-subtitle";
    public static final String SECTION_TITLE = "section-title";
    public static final String FORM_FIELD = "form-field";
    public static final String FORM_FIELD_WIDE = "form-field-wide";
    public static final String FORM_FIELD_SMALL = "form-field-small";
    public static final String BTN_PRIMARY = "btn-primary";
    public static final String BTN_SUCCESS = "btn-success";
    public static final String BTN_WARNING = "btn-warning";
    public static final String BTN_DANGER = "btn-danger";
    public static final String BTN_SMALL = "btn-small";
    public static final String BUTTON_GROUP = "button-group";
    public static final String CARD = "card";
    public static final String FADE_IN = "fade-in";
    public static final String SLIDE_IN = "slide-in";
    
    /**
     * Apply standard page layout styling
     */
    public static VerticalLayout createPageLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.addClassName(APP_CONTAINER);
        layout.addClassName(CENTERED_LAYOUT);
        layout.setSizeFull();
        return layout;
    }
    
    /**
     * Create a styled form container
     */
    public static VerticalLayout createFormContainer(boolean large) {
        VerticalLayout container = new VerticalLayout();
        container.addClassName(FORM_CONTAINER);
        if (large) {
            container.addClassName(FORM_CONTAINER_LARGE);
        }
        container.addClassName(FADE_IN);
        container.setAlignItems(VerticalLayout.Alignment.CENTER);
        container.setSpacing(true);
        container.setPadding(true);
        return container;
    }
    
    /**
     * Style a title component
     */
    public static H1 createPageTitle(String text) {
        H1 title = new H1(text);
        title.addClassName(PAGE_TITLE);
        return title;
    }
    
    /**
     * Style a subtitle component
     */
    public static H2 createPageSubtitle(String text) {
        H2 subtitle = new H2(text);
        subtitle.addClassName(PAGE_SUBTITLE);
        return subtitle;
    }
    
    /**
     * Style a section title component
     */
    public static H3 createSectionTitle(String text) {
        H3 title = new H3(text);
        title.addClassName(SECTION_TITLE);
        return title;
    }
    
    /**
     * Apply form field styling
     */
    public static void styleFormField(Component field, FieldSize size) {
        switch (size) {
            case NORMAL:
                field.addClassName(FORM_FIELD);
                break;
            case WIDE:
                field.addClassName(FORM_FIELD_WIDE);
                break;
            case SMALL:
                field.addClassName(FORM_FIELD_SMALL);
                break;
        }
    }
    
    /**
     * Apply button styling
     */
    public static void styleButton(Button button, ButtonType type, ButtonSize size) {
        // Apply type styling
        switch (type) {
            case PRIMARY:
                button.addClassName(BTN_PRIMARY);
                break;
            case SUCCESS:
                button.addClassName(BTN_SUCCESS);
                break;
            case WARNING:
                button.addClassName(BTN_WARNING);
                break;
            case DANGER:
                button.addClassName(BTN_DANGER);
                break;
        }
        
        // Apply size styling
        if (size == ButtonSize.SMALL) {
            button.addClassName(BTN_SMALL);
        }
    }
    
    /**
     * Show a styled success notification
     */
    public static void showSuccessNotification(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    /**
     * Show a styled error notification
     */
    public static void showErrorNotification(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
    
    /**
     * Show a styled warning notification
     */
    public static void showWarningNotification(String message) {
        Notification notification = Notification.show(message, 3500, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
    }
    
    // Enums for type safety
    public enum FieldSize {
        SMALL, NORMAL, WIDE
    }
    
    public enum ButtonType {
        PRIMARY, SUCCESS, WARNING, DANGER
    }
    
    public enum ButtonSize {
        NORMAL, SMALL
    }
}