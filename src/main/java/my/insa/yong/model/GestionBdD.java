package my.insa.yong.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import my.insa.yong.utils.database.ConnectionSimpleSGBD;

/**
 *
 * @author saancern
 */
public class GestionBdD {

    /**
     *
     * @param con
     * @throws SQLException
     */
    public static void creeSchema(Connection con)
            throws SQLException {
        try {
            con.setAutoCommit(false);
        try (Statement st = con.createStatement()) {
        // creation des tables
        st.executeUpdate("create table utilisateur ( "
            + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
            + " surnom varchar(30) not null unique,"
            + " pass varchar(20) not null,"
            + " isAdmin boolean not null default false "
            + ") "
        );
        st.executeUpdate("create table joueur ( "
            + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
            + " prenom varchar(50) not null,"
            + " nom varchar(50) not null,"
            + " taille double precision not null,"
            + " age int not null,"
            + " sexe char(1) check (sexe in ('F','H'))"
            + ") "
        );
        con.commit();
        }
        } catch (SQLException ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    /**
     *
     * @param con
     * @throws SQLException
     */
    public static void deleteSchema(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            try {
                st.executeUpdate("drop table joueur");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate("drop table utilisateur");
            } catch (SQLException ex) {
            }
        }
    }

    /**
     *
     * @param con
     * @throws SQLException
     */
    public static void razBdd(Connection con) throws SQLException {
        deleteSchema(con);
        creeSchema(con);
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            razBdd(con);
        } catch (SQLException ex) {
            throw new Error(ex);
        }
    }

}
