package my.insa.yong.vaadin.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.StreamResource;
import java.awt.Color;

/**
 *
 * @author saancern
 */
public class VaadinUtils {

    public static void encadre(Component c,
            CSSUtils.BorderStyle style,
            int largeurEnPixel,
            Color couleur) {
        c.getStyle().set("border-style", style.getCss());
        c.getStyle().set("border-width", largeurEnPixel + "px");
        c.getStyle().set("border-color", CSSUtils.toCSSColor(couleur));
        c.getStyle().set("border-radius", "0px");
    }

    public static void colorPaddingMargin(Component c,
            Color paddingColor, int paddingSizeInPixel,
            Color marginColor, int marginSizeInPixel
    ) {
        c.getStyle().set("padding", paddingSizeInPixel + "px");
        c.getStyle().set("background-color", CSSUtils.toCSSColor(paddingColor));
        c.getStyle().set("margin", marginSizeInPixel + "px");
        c.getStyle().set("box-shadow", "0px 0px 0px " + marginSizeInPixel + "px "
                + CSSUtils.toCSSColor(marginColor));
    }

}
