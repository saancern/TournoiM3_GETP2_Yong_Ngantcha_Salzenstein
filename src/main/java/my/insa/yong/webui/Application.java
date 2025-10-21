package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

import jakarta.annotation.PreDestroy;
import my.insa.yong.model.GestionBdD;
import my.insa.yong.utils.database.ConnectionPool;

@SpringBootApplication
@Theme("default")
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

    public static void main(String[] args) {
        // Initialiser le pool de connexions avant de démarrer l'application
        ConnectionPool.initialize();
        
        SpringApplication.run(Application.class, args);
        
        try (Connection con = ConnectionPool.getConnection()) {
            GestionBdD.creeSchema(con);
        } catch (SQLException ex) {
            throw new Error("Erreur lors de la création du schéma de base de données", ex);
        }
    }
    
    /**
     * Méthode appelée lors de l'arrêt de l'application pour fermer proprement le pool
     */
    @PreDestroy
    public void shutdown() {
        ConnectionPool.shutdown();
    }
}
