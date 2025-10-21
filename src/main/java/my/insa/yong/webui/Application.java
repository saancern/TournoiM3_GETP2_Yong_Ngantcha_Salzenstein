package my.insa.yong.webui;

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

import my.insa.yong.model.GestionBdD;
import my.insa.yong.utils.database.ConnectionSimpleSGBD;

@SpringBootApplication
@Theme("default")
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            GestionBdD.creeSchema(con);
        } catch (SQLException ex) {
            throw new Error(ex);
        }
    }

}
