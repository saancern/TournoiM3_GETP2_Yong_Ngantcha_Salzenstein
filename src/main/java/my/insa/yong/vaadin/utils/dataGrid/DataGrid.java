package my.insa.yong.vaadin.utils.dataGrid;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import my.insa.yong.utils.database.ResultSetUtils;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author saancern
 */
public class DataGrid extends Grid<List<Object>> {

    //private GridDescription gridDes;

    public DataGrid(List<List<Object>> datas, GridDescription gridDes) {
        //this.gridDes = gridDes;
        for (var colDes : gridDes.getColumns()) {
            Column<List<Object>> col;
            if (colDes.getRenderFromRow().isPresent()) {
                col = this.addColumn(new ComponentRenderer<>(colDes.getRenderFromRow().get()));
            } else {
                col = this.addColumn((source) -> colDes.getToObjectFromRow().get().apply(source));
            }
            if (colDes.getHeaderCompo().isPresent()) {
                col.setHeader(colDes.getHeaderCompo().get());
            } else if (colDes.getHeaderString().isPresent()) {
                col.setHeader(colDes.getHeaderString().get());
            }
            col.setVisible(colDes.isVisible());
            col.setAutoWidth(colDes.isAutoWidth());
        }
        this.setItems(datas);
        if (gridDes.isColumnBorders()) {
            this.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
        }
    }
    
    /**
     * Crée une DataGrid avec une liste vide de lignes.
     * <pre>
     * Les données de la DataGrid pourront être fixées par la suite avec la
     * méthode setItems de Grid.
     * </pre>
     * @param gridDes 
     */
    public DataGrid(GridDescription gridDes) {
        this(new ArrayList<>(),gridDes);
    }
    
    /**
     * un constructeur spécifiquement destinée à la sous-classe ResultSetGrid.
     * @param dataEtHeaders 
     */
    protected DataGrid(ResultSetUtils.ResultSetAsLists dataEtHeaders) {
        this(dataEtHeaders.getValues(),GridDescription.simpleGridDes(dataEtHeaders.getColumnNames()));
    }

    public static DataGrid simpleGrid(List<List<Object>> datas, List<String> headers) {
        return new DataGrid(datas, GridDescription.simpleGridDes(headers));
    }

}
