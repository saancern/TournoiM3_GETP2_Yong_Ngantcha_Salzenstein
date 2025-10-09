package my.insa.yong.utils.matrice;

import my.insa.yong.utils.StringUtil;
import my.insa.yong.utils.latex.LatexEscape;
import my.insa.yong.utils.latex.LatexMode;
import java.util.List;

/**
 *
 * @author saancern
 */
public class MatriceToText {

    public static String[][] fromObjectToString(String[][] mat) {
        String[][] res = new String[mat.length][];
        for (int i = 0; i < mat.length; i++) {
            if (mat[i] != null) {
                res[i] = new String[mat[i].length];
                for (int j = 0; j < res[i].length; j++) {
                    res[i][j] = mat[i][j].toString();
                }
            }
        }
        return res;
    }

    public static String[][] fromListOfListToArrays(List<List<String>> mat) {
        String[][] res = new String[mat.size()][];
        for (int i = 0; i < mat.size(); i++) {
            if (mat.get(i) != null) {
                res[i] = new String[mat.get(i).size()];
                for (int j = 0; j < res[i].length; j++) {
                    res[i][j] = mat.get(i).get(j);
                }
            }
        }
        return res;
    }

    /**
     * nombre de colonnes de la matrice.
     *
     * @return max du nombre d'éléments dans chaque ligne.
     */
    public static int nbrCol(Object[][] mat) {
        int res = 0;
        for (int i = 0; i < mat.length; i++) {
            if (mat[i] != null && mat[i].length > res) {
                res = mat[i].length;
            }
        }
        return res;
    }

    public static int[] largeurCols(String[][] mat) {
        int[] res = new int[nbrCol(mat)];
        for (int j = 0; j < res.length; j++) {
            for (int i = 0; i < mat.length; i++) {
                if (mat[i] != null && mat[i].length > j) {
                    if (mat[i][j] != null && mat[i][j].length() > res[j]) {
                        res[j] = mat[i][j].length();
                    } else if (mat[i][j] == null) {
                        res[j] = Math.max(res[j], "null".length());
                    }
                }
            }
        }
        return res;
    }

    private static void collectHline(int[] largeurs, StringBuilder collect) {
        collect.append("+");
        for (int i = 0; i < largeurs.length; i++) {
            collect.append(StringUtil.mult("-", largeurs[i] + 2));
            collect.append("+");
        }
        collect.append("\n");
    }

    public static String formatMat(List<List<String>> mat, boolean headers) {
        return formatMat(fromListOfListToArrays(mat), headers);
    }

    public static String formatMat(String[][] mat, boolean headers) {
        StringBuilder res = new StringBuilder();
        int[] largeurs = largeurCols(mat);
        collectHline(largeurs, res);
        for (int i = 0; i < mat.length; i++) {
            if (mat[i] == null) {
                res.append("null\n");
            } else {
                res.append("| ");
                for (int j = 0; j < mat[i].length; j++) {
                    String elem;
                    if (mat[i][j] == null) {
                        elem = "null";
                    } else {
                        elem = mat[i][j];
                    }
                    res.append(StringUtil.padRight(elem, largeurs[j]));
                    res.append(" | ");
                }
                res.append("\n");
            }
            if (headers && i == 0) {
                collectHline(largeurs, res);
            }
        }
        collectHline(largeurs, res);
        return res.toString();
    }

    public static String formatMatLatex(List<List<String>> mat, boolean headers) {
        return formatMatLatex(fromListOfListToArrays(mat), headers);
    }

    public static String formatMatLatex(String[][] mat, boolean headers) {
        StringBuilder res = new StringBuilder();
        res.append(
                "\\adjustbox{max width=\\textwidth,max totalheight=\\textheight}{%\n"
                + "\\begin{tabular}{|");
        for (int col = 0; col < mat[0].length; col++) {
            res.append("l|");
        }
        res.append(
                "}\n"
                + "\\hline\n");
        for (int lig = 0; lig < mat.length; lig++) {
            for (int col = 0; col < mat[lig].length; col++) {
                res.append(LatexEscape.escapeLatex(mat[lig][col], LatexMode.TextMode));
                if (col == mat[lig].length - 1) {
                    res.append(" \\\\\n"
                            + "\\hline\n");
                } else {
                    res.append(" & ");
                }
            }
            if (lig == 0 && headers) {
                res.append("\\hline\n");
            }
        }
        res.append(
                "\\end{tabular}\n"
                + "}\n");
        return res.toString();
    }

}
