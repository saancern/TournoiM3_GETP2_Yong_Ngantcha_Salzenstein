package my.insa.yong.webui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Layout de base avec navigation pour toutes les pages
 * @author saancern
 */
public class BaseLayout extends VerticalLayout {
    
    private final VerticalLayout contentArea;
    
    public BaseLayout() {
        this.setSpacing(false);
        this.setPadding(false);
        this.setSizeFull();
        this.addClassName("app-container");
        
        // Ajouter le header de navigation
        NavigationHeader header = new NavigationHeader();
        
        // Zone de contenu
        contentArea = new VerticalLayout();
        contentArea.setSizeFull();
        contentArea.setSpacing(true);
        contentArea.setPadding(true);
        
        this.add(header, contentArea);
        this.setFlexGrow(0, header);
        this.setFlexGrow(1, contentArea);
    }
    
    /**
     * Ajoute du contenu à la zone principale
     */
    public void addToContent(Component... components) {
        contentArea.add(components);
    }
    
    /**
     * Configure l'alignement du contenu
     */
    public void setContentAlignment(Alignment alignment) {
        contentArea.setAlignItems(alignment);
    }
    
    /**
     * Configure la justification du contenu
     */
    public void setContentJustifyMode(JustifyContentMode justifyMode) {
        contentArea.setJustifyContentMode(justifyMode);
    }
    
    /**
     * Récupère la zone de contenu pour configuration avancée
     */
    public VerticalLayout getContentArea() {
        return contentArea;
    }
}