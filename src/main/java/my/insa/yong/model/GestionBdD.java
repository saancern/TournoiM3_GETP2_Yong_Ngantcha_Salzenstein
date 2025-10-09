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
        st.executeUpdate("create table loisir ( "
            + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
            + " nom varchar(20) not null unique,"
            + " description text"
            + ") "
        );
        st.executeUpdate("create table pratique ( "
            + " idutilisateur integer not null,"
            + " idloisir integer not null,"
            + " niveau integer not null "
            + ") "
        );
        // Création de la table joueur
        st.executeUpdate("create table joueur ( "
            + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
            + " prenom varchar(50) not null,"
            + " nom varchar(50) not null,"
            + " taille double precision not null "
            + ") "
        );
        con.commit();
        st.executeUpdate("create table apprecie ( "
            + " u1 integer not null,"
            + " u2 integer not null"
            + ") "
        );

        st.executeUpdate("alter table apprecie\n"
            + "  add constraint fk_apprecie_u1\n"
            + "  foreign key (u1) references utilisateur(id)"
        );
        st.executeUpdate("alter table apprecie\n"
            + "  add constraint fk_apprecie_u2\n"
            + "  foreign key (u2) references utilisateur(id)"
        );
        st.executeUpdate("alter table pratique\n"
            + "  add constraint fk_pratique_idutilisateur\n"
            + "  foreign key (idutilisateur) references utilisateur(id)"
        );

        st.executeUpdate("alter table pratique\n"
            + "  add constraint fk_pratique_idloisir\n"
            + "  foreign key (idloisir) references loisir(id)"
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
                st.executeUpdate(
                        "alter table utilisateur "
                        + "drop constraint fk_utilisateur_u1");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate(
                        "alter table utilisateur "
                        + "drop constraint fk_utilisateur_u2");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate(
                        "alter table pratique "
                        + "drop constraint fk_pratique_idutilisateur");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate(
                        "alter table pratique "
                        + "drop constraint fk_pratique_idloisir");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate("drop table apprecie");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate("drop table pratique");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate("drop table loisir");
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
