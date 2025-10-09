package my.insa.yong.utils.latex;

/**
 *
 * @author saancern
 */
public interface LatexEnv extends LatexProducer {

    public String latexHeader(TopLevelIncludes collect, LatexMode mode);

    public String latexContent(TopLevelIncludes collect, LatexMode mode);

    public String latexFooter(TopLevelIncludes collect, LatexMode mode);

    public default String toLatex(TopLevelIncludes collect, LatexMode mode) {
        return this.latexHeader(collect, mode)
                + this.latexContent(collect, mode)
                + this.latexFooter(collect, mode);
    }

}
