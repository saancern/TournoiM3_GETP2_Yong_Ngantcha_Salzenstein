/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is part of CoursBeuvron.

CoursBeuvron is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoursBeuvron is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package my.insa.yong.webui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 *
 * @author francois
 */
@Route(value = "")
@PageTitle("Accueil")
public class VuePrincipale extends VerticalLayout {

    public VuePrincipale() {
        H2 titre = new H2("Page d'accueil");

        // Lien vers la page de connexion
        RouterLink lienConnexion = new RouterLink("Se connecter / S'inscrire", VueConnexion.class);
        lienConnexion.getStyle().set("font-size", "18px");
        lienConnexion.getStyle().set("color", "#2c3e50");

        // Lien vers la page d'ajout de joueur
        RouterLink lienJoueur = new RouterLink("Ajouter un joueur", VueJoueur.class);
        lienJoueur.getStyle().set("font-size", "18px");
        lienJoueur.getStyle().set("color", "#16a085");

        this.add(titre, lienConnexion, lienJoueur);
        this.setAlignItems(Alignment.CENTER);
        this.setSpacing(true);
    }

}
