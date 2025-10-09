package my.insa.yong.vaadin.utils.dataGrid;

import my.insa.yong.utils.database.ResultSetUtils;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Permet facilement de visualier n'importe quel ResultSet dans une Grid vaadin.
 * <pre>
 * On fourni le PreparedStatement plutôt que directement le ResultSet : cela
 * permet si besoin de rafraichir la Grid en ré-évaluant le PreparedStatement.
 * </pre>
 * @author saancern
 */
public class ResultSetGrid extends DataGrid {

    private PreparedStatement pst;
    
    /**
     * crée une Grid avec toutes les colonnes du ResultSet, et les noms de colonnes
     * comme entêtes.
     * <pre>
     * Si vous voulez de jolies Grid, utilisez plutôt le constructeur avec
     * une GridDescription, qui vous permet de préciser les colonnes et leur
     * forme.
     * </pre>
     * @param pst
     * @throws SQLException 
     */
    public ResultSetGrid(PreparedStatement pst) throws SQLException {
        super(executeStatement(pst));
        this.pst = pst;
    }

    /**
     * crée une Grid à partir d'un ResultSet, en précisant dans la GridDescription
     * les colonnes à afficher, et leur mise en forme.
     * @param pst
     * @param gridDes
     * @throws SQLException 
     */
    public ResultSetGrid(PreparedStatement pst, GridDescription gridDes) throws SQLException {
        super(gridDes);
        this.pst = pst;
        this.update();
    }

    private static ResultSetUtils.ResultSetAsLists executeStatement(PreparedStatement pst) throws SQLException {
        try (ResultSet res = pst.executeQuery()) {
            return ResultSetUtils.toLists(res);
        }
    }

    public void update() throws SQLException {
        var asLists = executeStatement(this.pst);
        this.setItems(asLists.getValues());
    }

}
