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
package my.insa.yong.vaadin.utils;

import java.awt.Color;

/**
 *
 * @author francois
 */
public class CSSUtils {

    public static String toCSSColor(Color couleur) {
        return "rgba("
                + couleur.getRed() + ","
                + couleur.getGreen() + ","
                + couleur.getBlue() + ","
                + couleur.getAlpha()
                + ")";
    }

    /**
     * voir https://developer.mozilla.org/en-US/docs/Web/CSS/border-style
     */
    public static enum BorderStyle {
        DOTTED("dotted"),
        DASHED("dashed"),
        SOLID("solid"),
        DOUBLE("double"),
        GROOVE("groove"),
        RIDGE("ridge"),
        INSET("inset"),
        OUTSET("outset"),
        NONE("none"),
        HIDDEN("hidden"),;

        private String css;

        BorderStyle(String css) {
            this.css = css;
        }

        public String getCss() {
            return this.css;
        }
    }

}
