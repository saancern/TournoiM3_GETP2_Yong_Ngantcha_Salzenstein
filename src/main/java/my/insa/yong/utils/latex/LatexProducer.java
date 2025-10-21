package my.insa.yong.utils.latex;

//import java.io.IOException;
//import java.io.PrintWriter;
//import java.io.StringWriter;
//import java.io.Writer;

/**
 *
 * @author saancern
 */
public interface LatexProducer {
    
    /**
     * return the latex representation of this.
     * As a side effect, add all required includes in the collect parameter.
     * These includes will be taken care of in a {@link TopLevelLatex} or may
     * be simply ignored in a embeded context
     * @param out
     * @param collect 
     */
    public String toLatex(TopLevelIncludes collect,LatexMode mode);
    
    /**
     * par defaut, on ignore completement les includes
     * @return le code latex sans les includes.
     */
    public default String toLatex() {
        TopLevelIncludes topIncludes = new TopLevelIncludes();
        return this.toLatex(topIncludes,LatexMode.TextMode);
    }
    
    
   
}
