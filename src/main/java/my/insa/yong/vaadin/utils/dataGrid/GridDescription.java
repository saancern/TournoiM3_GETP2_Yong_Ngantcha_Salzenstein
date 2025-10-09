package my.insa.yong.vaadin.utils.dataGrid;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author saancern
 */
public class GridDescription {

    private List<ColumnDescription> columns;
    private boolean columnBorders;

    public GridDescription(List<ColumnDescription> columns) {
        this.columns = columns;
        this.columnBorders = false;
    }

    public List<ColumnDescription> getColumns() {
        return columns;
    }

    /**
     * cas particulier ou l'on utiliser toString pour toutes les colonnes.
     * <pre>
     * <p> il doit y avoir autant de headers que de colonnes dans les données.
     * </p>
     * </pre>
     * @param headers
     * @return 
     */
    public static GridDescription simpleGridDes(List<String> headers) {
        List<ColumnDescription> res = new ArrayList<>(headers.size());
        for (int i = 0; i < headers.size(); i++) {
            res.add(new ColumnDescription()
                    .headerString(headers.get(i))
                    .colData(i));
        }
        return new GridDescription(res);
    }
    
    public GridDescription columnBorders(boolean borders) {
        this.columnBorders = borders;
        return this;
    }

    /**
     * @return the columnBorders
     */
    public boolean isColumnBorders() {
        return columnBorders;
    }

}
