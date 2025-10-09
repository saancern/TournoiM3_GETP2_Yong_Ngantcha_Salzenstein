package my.insa.yong.utils.exceptions;

/**
 *
 * @author saancern
 */
public class ExceptionsUtils {
    
    public static String messageEtPremiersAppelsDansPackage(Throwable ex,String packageName,int nbrAppels) {
        String res = "--> (display only first " + nbrAppels + " calls in package : " + packageName + ")\n" + 
                "Exception in thread \"" + Thread.currentThread().getName() +
                "\" " + ex.getClass().getName() +
                ": " + ex.getLocalizedMessage();
        int i = 0;
        int nbr = 0;
        StackTraceElement[] elems = ex.getStackTrace();
        while (nbr < nbrAppels && i < elems.length) {
            if (elems[i].getClassName().startsWith(packageName)) {
//                res = res + "\n" + i + ": at " + elems[i].getClassName() +
                res = res + "\n    at " + elems[i].getClassName() +
                        "." + elems[i].getMethodName() + "(" + 
                        elems[i].getFileName() + ":" +elems[i].getLineNumber() + ")";
                nbr ++;
            }
            i ++;
        }
        return res;
    }
    
}
