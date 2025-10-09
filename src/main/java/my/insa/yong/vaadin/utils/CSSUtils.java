package my.insa.yong.vaadin.utils;

import java.awt.Color;

/**
 *
 * @author saancern
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
