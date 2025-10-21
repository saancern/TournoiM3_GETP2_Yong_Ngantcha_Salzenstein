package my.insa.yong.model;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import my.insa.yong.utils.database.ConnectionPool;

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
        
        // Définir les requêtes de création de tables (sans foreign keys d'abord)
        String[] createTableQueries = {
            // Table utilisateur (aucune dépendance)
            "create table utilisateur ( "
                + ConnectionPool.sqlForGeneratedKeys(con, "id") + ","
                + " surnom varchar(30) not null unique,"
                + " pass varchar(20) not null,"
                + " isAdmin boolean not null default false "
                + ") ",
            
            // Table joueur (aucune dépendance)
            "create table joueur ( "
                + ConnectionPool.sqlForGeneratedKeys(con, "id") + ","
                + " prenom varchar(50) not null,"
                + " nom varchar(50) not null,"
                + " taille double precision not null,"
                + " age int not null,"
                + " sexe char(1) check (sexe in ('F','H'))"
                + ") ",
            
            // Table equipe (aucune dépendance)
            "create table equipe ( "
                + ConnectionPool.sqlForGeneratedKeys(con, "id") + ","
                + " nom_equipe varchar(50) not null,"
                + " date_creation date not null"
                + ") ",
            
            // Table tournoi (aucune dépendance)
            "create table tournoi ( "
                + ConnectionPool.sqlForGeneratedKeys(con, "id") + ","
                + " nom_tournoi varchar(255) not null,"
                + " sport varchar(100) not null,"
                + " nombre_terrains int not null,"
                + " nombre_joueurs_par_equipe int not null"
                + ") ",
            
            // Table joueur_equipe (sans foreign keys d'abord)
            "create table joueur_equipe ( "
                + " joueur_id int not null,"
                + " equipe_id int not null,"
                + " primary key (joueur_id, equipe_id)"
                + ") "
        };
        
        // Définir les contraintes de clés étrangères à ajouter après
        String[] foreignKeyQueries = {
            "alter table joueur_equipe add constraint fk_joueur_equipe_joueur "
                + " foreign key (joueur_id) references joueur(id) on delete cascade",
            
            "alter table joueur_equipe add constraint fk_joueur_equipe_equipe "
                + " foreign key (equipe_id) references equipe(id) on delete cascade"
        };
        
        String[] tableNames = {"utilisateur", "joueur", "equipe", "tournoi", "joueur_equipe"};
        String[] constraintNames = {"fk_joueur_equipe_joueur", "fk_joueur_equipe_equipe"};
        
        boolean oldAutoCommit = con.getAutoCommit();
        SQLException lastException = null;
        int successCount = 0;
        int constraintCount = 0;
        
        try {
            con.setAutoCommit(false);
            
            try (Statement st = con.createStatement()) {
                // Étape 1: Créer toutes les tables d'abord
                System.out.println("=== Création des tables ===");
                for (int i = 0; i < createTableQueries.length; i++) {
                    try {
                        st.executeUpdate(createTableQueries[i]);
                        System.out.println("Table '" + tableNames[i] + "' créée avec succès.");
                        successCount++;
                    } catch (SQLException ex) {
                        System.err.println("Erreur lors de la création de la table '" + tableNames[i] + "': " + ex.getMessage());
                        lastException = ex;
                        // Continuer avec les autres tables
                    }
                }
                
                // Étape 2: Ajouter les contraintes de clés étrangères
                System.out.println("=== Ajout des contraintes de clés étrangères ===");
                for (int i = 0; i < foreignKeyQueries.length; i++) {
                    try {
                        st.executeUpdate(foreignKeyQueries[i]);
                        System.out.println("Contrainte '" + constraintNames[i] + "' ajoutée avec succès.");
                        constraintCount++;
                    } catch (SQLException ex) {
                        System.err.println("Erreur lors de l'ajout de la contrainte '" + constraintNames[i] + "': " + ex.getMessage());
                        lastException = ex;
                        // Continuer avec les autres contraintes
                    }
                }
                
                // Si au moins une table a été créée, commiter
                if (successCount > 0) {
                    con.commit();
                    System.out.println("=== Résumé ===");
                    System.out.println("Schema créé avec " + successCount + "/" + tableNames.length + " tables.");
                    System.out.println("Contraintes ajoutées: " + constraintCount + "/" + constraintNames.length + ".");
                    
                    // Créer le tournoi par défaut avec ID=1
                    creerTournoiParDefaut(con);
                    
                } else {
                    con.rollback();
                    System.err.println("Aucune table n'a pu être créée.");
                }
            }
            
        } catch (SQLException ex) {
            try {
                con.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Erreur lors du rollback: " + rollbackEx.getMessage());
            }
            throw ex;
        } finally {
            try {
                con.setAutoCommit(oldAutoCommit);
            } catch (SQLException ex) {
                System.err.println("Erreur lors de la restauration d'autoCommit: " + ex.getMessage());
            }
        }
        
        // Si aucune table n'a été créée et qu'il y a eu une exception, la relancer
        if (successCount == 0 && lastException != null) {
            throw lastException;
        }
    }

    /**
     *
     * @param con
     * @throws SQLException
     */
    public static void deleteSchema(Connection con) throws SQLException {
        // Définir les contraintes à supprimer d'abord
        String[] constraintQueries = {
            "alter table joueur_equipe drop constraint if exists fk_joueur_equipe_joueur",
            "alter table joueur_equipe drop constraint if exists fk_joueur_equipe_equipe"
        };
        
        String[] constraintNames = {"fk_joueur_equipe_joueur", "fk_joueur_equipe_equipe"};
        
        // Définir les tables à supprimer dans l'ordre inverse des dépendances
        String[] tableNames = {"joueur_equipe", "equipe", "joueur", "utilisateur"};
        
        int constraintCount = 0;
        int successCount = 0;
        StringBuilder errors = new StringBuilder();
        
        try (Statement st = con.createStatement()) {
            // Étape 1: Supprimer les contraintes de clés étrangères d'abord
            System.out.println("=== Suppression des contraintes de clés étrangères ===");
            for (int i = 0; i < constraintQueries.length; i++) {
                try {
                    st.executeUpdate(constraintQueries[i]);
                    System.out.println("Contrainte '" + constraintNames[i] + "' supprimée avec succès.");
                    constraintCount++;
                } catch (SQLException ex) {
                    System.err.println("Erreur lors de la suppression de la contrainte '" + constraintNames[i] + "': " + ex.getMessage());
                    if (errors.length() > 0) {
                        errors.append("; ");
                    }
                    errors.append(constraintNames[i]).append(": ").append(ex.getMessage());
                    // Continuer avec les autres contraintes
                }
            }
            
            // Étape 2: Supprimer les tables
            System.out.println("=== Suppression des tables ===");
            for (String tableName : tableNames) {
                try {
                    st.executeUpdate("drop table if exists " + tableName);
                    System.out.println("Table '" + tableName + "' supprimée avec succès.");
                    successCount++;
                } catch (SQLException ex) {
                    System.err.println("Erreur lors de la suppression de la table '" + tableName + "': " + ex.getMessage());
                    if (errors.length() > 0) {
                        errors.append("; ");
                    }
                    errors.append(tableName).append(": ").append(ex.getMessage());
                    // Continuer avec les autres tables
                }
            }
        }
        
        System.out.println("=== Résumé de suppression ===");
        System.out.println("Contraintes supprimées: " + constraintCount + "/" + constraintNames.length);
        System.out.println("Tables supprimées: " + successCount + "/" + tableNames.length);
        
        // Si aucune table n'a pu être supprimée et qu'il y a eu des erreurs, afficher un message
        if (successCount == 0 && errors.length() > 0) {
            System.err.println("Aucune table n'a pu être supprimée. Erreurs: " + errors.toString());
            // On ne lance pas d'exception car la suppression peut échouer si les tables n'existent pas
        }
    }

    /**
     * Affiche le statut des tables dans la base de données
     * @param con
     */
    public static void checkSchemaStatus(Connection con) {
        String[] tableNames = {"utilisateur", "joueur", "equipe", "joueur_equipe"};
        
        System.out.println("=== Statut des tables ===");
        try (Statement st = con.createStatement()) {
            for (String tableName : tableNames) {
                try {
                    ResultSet rs = st.executeQuery("SELECT COUNT(*) as count FROM " + tableName);
                    if (rs.next()) {
                        int count = rs.getInt("count");
                        System.out.println("Table '" + tableName + "': existe (" + count + " ligne(s))");
                    }
                    rs.close();
                } catch (SQLException ex) {
                    System.out.println("Table '" + tableName + "': n'existe pas ou inaccessible");
                }
            }
        } catch (SQLException ex) {
            System.err.println("Erreur lors de la vérification du statut: " + ex.getMessage());
        }
        System.out.println("========================");
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
        try (Connection con = ConnectionPool.getConnection()) {
            System.out.println("=== AVANT RECONSTRUCTION ===");
            checkSchemaStatus(con);
            
            System.out.println("\n=== RECONSTRUCTION DE LA BASE ===");
            razBdd(con);
            
            System.out.println("\n=== APRÈS RECONSTRUCTION ===");
            checkSchemaStatus(con);
            
        } catch (SQLException ex) {
            throw new Error(ex);
        }
    }
    
    /**
     * Crée le tournoi par défaut avec ID=1 s'il n'existe pas déjà
     * @param con La connexion à la base de données
     * @throws SQLException En cas d'erreur SQL
     */
    private static void creerTournoiParDefaut(Connection con) throws SQLException {
        // Vérifier si le tournoi ID=1 existe déjà
        String checkSql = "SELECT COUNT(*) FROM tournoi WHERE id = 1";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(checkSql)) {
            
            if (rs.next() && rs.getInt(1) == 0) {
                // Le tournoi ID=1 n'existe pas, le créer
                String insertSql = "INSERT INTO tournoi (id, nom_tournoi, sport, nombre_terrains, nombre_joueurs_par_equipe) " +
                                 "VALUES (1, 'Tournoi', 'Foot', 10, 11)";
                
                try (Statement insertSt = con.createStatement()) {
                    insertSt.executeUpdate(insertSql);
                    System.out.println("Tournoi par défaut créé avec succès (ID=1, 'Tournoi', 'Foot', 10, 11)");
                }
            } else {
                System.out.println("Tournoi par défaut (ID=1) existe déjà");
            }
        }
    }

}
