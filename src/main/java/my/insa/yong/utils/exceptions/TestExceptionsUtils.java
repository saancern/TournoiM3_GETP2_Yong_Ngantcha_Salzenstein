package my.insa.yong.utils.exceptions;

//import java.util.logging.Level;
//import java.util.logging.Logger;

/**
 *
 * @author saancern
 */
public class TestExceptionsUtils {

    public static void test() {
        try {
            throw new RuntimeException("coucou");
        } catch (RuntimeException ex) {
            System.out.println(ExceptionsUtils.messageEtPremiersAppelsDansPackage(ex, "fr.insa.beuvron", 5));
        }
        throw new RuntimeException("coucou2");
    }

}
