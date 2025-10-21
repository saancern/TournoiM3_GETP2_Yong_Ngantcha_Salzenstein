package my.insa.yong.utils.latex;

//import java.io.StringWriter;
//import java.util.List;

/**
 * ajoute les includes après collect dans la forme finale toLatex
 * @author saancern
 */
public interface TopLevelLatex extends LatexProducer {
    
    public default String toLatex() {
       TopLevelIncludes includes = new TopLevelIncludes();
       String withoutIncludes = this.toLatex(includes,LatexMode.TextMode);
       return includes.toLatex() + withoutIncludes;
    }
    

    
}
