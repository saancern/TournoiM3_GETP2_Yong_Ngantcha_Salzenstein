package my.insa.yong.utils.latex;

/**
 *
 * @author saancern
 */
public class LatexEscape {

    public static String escapeLatex(String s, LatexMode mode) {
        StringBuilder res = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c == '\\') {
                if (mode == LatexMode.TextMode) {
                    res.append("\\textbackslash ");
                } else {
                    res.append("\\backslash ");
                }
            } else if (c == '^') {
                res.append("\\textasciicircum ");
            } else if (c == '~') {
                res.append("\\textasciitilde ");
            } else if ("_%#&{}".contains("" + c)) {
                res.append("\\"+c);
            } else {
                res.append(c);
            }
        }
        return res.toString();

    }
}

