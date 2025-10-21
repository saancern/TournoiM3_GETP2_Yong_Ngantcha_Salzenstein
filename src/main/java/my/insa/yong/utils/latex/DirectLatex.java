package my.insa.yong.utils.latex;

//import java.io.Writer;

/**
 *
 * @author saancern
 */
public class DirectLatex implements LatexProducer {
    
    private String latexCode;
    
    private DirectLatex(String latexCode) {
        this.latexCode = latexCode;
    }
    
    public static DirectLatex verbatim(String latexCode) {
        return new DirectLatex(latexCode);      
    }

    @Override
    public String toLatex(TopLevelIncludes collect,LatexMode m) {
        return this.latexCode;
    }

    
}
