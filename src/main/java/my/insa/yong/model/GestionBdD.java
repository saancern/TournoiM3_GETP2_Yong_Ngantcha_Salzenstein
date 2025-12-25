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

            // Table joueur (ajout tournoi_id)
            "create table joueur ( "
                + ConnectionPool.sqlForGeneratedKeys(con, "id") + ","
                + " prenom varchar(50) not null,"
                + " nom varchar(50) not null,"
                + " taille double precision not null,"
                + " age int not null,"
                + " sexe char(1) check (sexe in ('F','H')),"
                + " tournoi_id int not null"
                + ") ",

            // Table equipe (ajout tournoi_id)
            "create table equipe ( "
                + ConnectionPool.sqlForGeneratedKeys(con, "id") + ","
                + " nom_equipe varchar(50) not null,"
                + " date_creation date not null,"
                + " tournoi_id int not null"
                + ") ",

            // Table tournoi (aucune dépendance)
            "create table tournoi ( "
                + ConnectionPool.sqlForGeneratedKeys(con, "id") + ","
                + " nom_tournoi varchar(255) not null,"
                + " sport varchar(100) not null,"
                + " nombre_terrains int not null,"
                + " nombre_joueurs_par_equipe int not null"
                + ") ",

            // Table joueur_equipe (ajout tournoi_id)
            "create table joueur_equipe ( "
                + " joueur_id int not null,"
                + " equipe_id int not null,"
                + " tournoi_id int not null,"
                + " primary key (joueur_id, equipe_id, tournoi_id)"
                + ") ",

            // Table rencontre (matches) - dépend de tournoi et equipe
            "create table rencontre ( "
                + ConnectionPool.sqlForGeneratedKeys(con, "id") + ","
                + " tournoi_id int not null,"
                + " round_number int not null,"
                + " pool_index int,"
                + " equipe_a_id int not null,"
                + " equipe_b_id int,"
                + " score_a int,"
                + " score_b int,"
                + " winner_id int,"
                + " played boolean not null default false"
                + ") ",

            // Table but (ajout tournoi_id)
            "create table but ( "
                + ConnectionPool.sqlForGeneratedKeys(con, "id") + ","
                + " rencontre_id int not null,"
                + " equipe_id int not null,"
                + " joueur_id int not null,"
                + " minute int,"
                + " tournoi_id int not null"
                + ") ",

            // Table terrain (gestion des terrains)
            "create table terrain ( "
                + ConnectionPool.sqlForGeneratedKeys(con, "id") + ","
                + " nom_terrain varchar(100) not null,"
                + " numero int not null,"
                + " tournoi_id int not null"
                + ") ",

            // Table terrain_rencontre (liaison N:N terrain-match)
            "create table terrain_rencontre ( "
                + " terrain_id int not null,"
                + " rencontre_id int not null,"
                + " tournoi_id int not null,"
                + " primary key (terrain_id, rencontre_id, tournoi_id)"
                + ") "
        };
        
        // Définir les contraintes de clés étrangères à ajouter après
        String[] foreignKeyQueries = {
            "alter table joueur add constraint fk_joueur_tournoi "
                + " foreign key (tournoi_id) references tournoi(id) on delete cascade",

            "alter table equipe add constraint fk_equipe_tournoi "
                + " foreign key (tournoi_id) references tournoi(id) on delete cascade",

            "alter table joueur_equipe add constraint fk_joueur_equipe_joueur "
                + " foreign key (joueur_id) references joueur(id) on delete cascade",

            "alter table joueur_equipe add constraint fk_joueur_equipe_equipe "
                + " foreign key (equipe_id) references equipe(id) on delete cascade",

            "alter table joueur_equipe add constraint fk_joueur_equipe_tournoi "
                + " foreign key (tournoi_id) references tournoi(id) on delete cascade",

            "alter table rencontre add constraint fk_rencontre_tournoi "
                + " foreign key (tournoi_id) references tournoi(id) on delete cascade",

            "alter table rencontre add constraint fk_rencontre_equipe_a "
                + " foreign key (equipe_a_id) references equipe(id) on delete cascade",

            "alter table rencontre add constraint fk_rencontre_equipe_b "
                + " foreign key (equipe_b_id) references equipe(id) on delete cascade",

            "alter table rencontre add constraint fk_rencontre_winner "
                + " foreign key (winner_id) references equipe(id) on delete set null",

            "alter table but add constraint fk_but_rencontre "
                + " foreign key (rencontre_id) references rencontre(id) on delete cascade",

            "alter table but add constraint fk_but_equipe "
                + " foreign key (equipe_id) references equipe(id) on delete cascade",

            "alter table but add constraint fk_but_joueur "
                + " foreign key (joueur_id) references joueur(id) on delete cascade",

            "alter table but add constraint fk_but_tournoi "
                + " foreign key (tournoi_id) references tournoi(id) on delete cascade",

            "alter table terrain add constraint fk_terrain_tournoi "
                + " foreign key (tournoi_id) references tournoi(id) on delete cascade",

            "alter table terrain_rencontre add constraint fk_terrain_rencontre_terrain "
                + " foreign key (terrain_id) references terrain(id) on delete cascade",

            "alter table terrain_rencontre add constraint fk_terrain_rencontre_rencontre "
                + " foreign key (rencontre_id) references rencontre(id) on delete cascade",

            "alter table terrain_rencontre add constraint fk_terrain_rencontre_tournoi "
                + " foreign key (tournoi_id) references tournoi(id) on delete cascade"
        };
        
        String[] tableNames = {"utilisateur", "joueur", "equipe", "tournoi", "joueur_equipe", "rencontre", "but", "terrain", "terrain_rencontre"};
        String[] constraintNames = {"fk_joueur_tournoi", "fk_equipe_tournoi",
                                   "fk_joueur_equipe_joueur", "fk_joueur_equipe_equipe", "fk_joueur_equipe_tournoi",
                                   "fk_rencontre_tournoi", "fk_rencontre_equipe_a", "fk_rencontre_equipe_b", 
                                   "fk_rencontre_winner", "fk_but_rencontre", "fk_but_equipe", "fk_but_joueur", "fk_but_tournoi",
                                   "fk_terrain_tournoi", "fk_terrain_rencontre_terrain", "fk_terrain_rencontre_rencontre", "fk_terrain_rencontre_tournoi"};
        
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
                        // Check if table already exists
                        String errorMessage = ex.getMessage().toLowerCase();
                        if (errorMessage.contains("already exists") || 
                            errorMessage.contains("déjà exist") || 
                            errorMessage.contains("table/view does not exist") ||
                            errorMessage.contains("table") && errorMessage.contains("exist")) {
                            System.out.println("Table '" + tableNames[i] + "' existe déjà.");
                            successCount++; // Count as success since table exists
                        } else {
                            System.err.println("Erreur lors de la création de la table '" + tableNames[i] + "': " + ex.getMessage());
                            lastException = ex;
                        }
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
                        // Check if constraint already exists
                        String errorMessage = ex.getMessage().toLowerCase();
                        if (errorMessage.contains("already exists") || 
                            errorMessage.contains("déjà exist") || 
                            errorMessage.contains("constraint") && errorMessage.contains("exist")) {
                            System.out.println("Contrainte '" + constraintNames[i] + "' existe déjà.");
                            constraintCount++; // Count as success since constraint exists
                        } else {
                            System.err.println("Erreur lors de l'ajout de la contrainte '" + constraintNames[i] + "': " + ex.getMessage());
                            lastException = ex;
                        }
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
            "alter table but drop constraint if exists fk_but_rencontre",
            "alter table but drop constraint if exists fk_but_equipe",
            "alter table but drop constraint if exists fk_but_joueur",
            "alter table terrain_rencontre drop constraint if exists fk_terrain_rencontre_terrain",
            "alter table terrain_rencontre drop constraint if exists fk_terrain_rencontre_rencontre",
            "alter table terrain_rencontre drop constraint if exists fk_terrain_rencontre_tournoi",
            "alter table terrain drop constraint if exists fk_terrain_tournoi",
            "alter table rencontre drop constraint if exists fk_rencontre_tournoi",
            "alter table rencontre drop constraint if exists fk_rencontre_equipe_a",
            "alter table rencontre drop constraint if exists fk_rencontre_equipe_b",
            "alter table rencontre drop constraint if exists fk_rencontre_winner",
            "alter table joueur_equipe drop constraint if exists fk_joueur_equipe_joueur",
            "alter table joueur_equipe drop constraint if exists fk_joueur_equipe_equipe"
        };
        
        String[] constraintNames = {"fk_but_rencontre", "fk_but_equipe", "fk_but_joueur",
                                   "fk_terrain_rencontre_terrain", "fk_terrain_rencontre_rencontre", "fk_terrain_rencontre_tournoi",
                                   "fk_terrain_tournoi", "fk_rencontre_tournoi", "fk_rencontre_equipe_a", "fk_rencontre_equipe_b", 
                                   "fk_rencontre_winner", "fk_joueur_equipe_joueur", "fk_joueur_equipe_equipe"};
        
        // Définir les tables à supprimer dans l'ordre inverse des dépendances
        String[] tableNames = {"terrain_rencontre", "but", "terrain", "rencontre", "joueur_equipe", "tournoi", "equipe", "joueur", "utilisateur"};
        
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
                    // Check if constraint doesn't exist (which is OK for deletion)
                    String errorMessage = ex.getMessage().toLowerCase();
                    if (errorMessage.contains("does not exist") || 
                        errorMessage.contains("n'existe pas") || 
                        errorMessage.contains("not found") ||
                        errorMessage.contains("constraint") && errorMessage.contains("not")) {
                        System.out.println("Contrainte '" + constraintNames[i] + "' n'existait pas.");
                        constraintCount++; // Count as success since constraint is gone
                    } else {
                        System.err.println("Erreur lors de la suppression de la contrainte '" + constraintNames[i] + "': " + ex.getMessage());
                        if (errors.length() > 0) {
                            errors.append("; ");
                        }
                        errors.append(constraintNames[i]).append(": ").append(ex.getMessage());
                    }
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
                    // Check if table doesn't exist (which is OK for deletion)
                    String errorMessage = ex.getMessage().toLowerCase();
                    if (errorMessage.contains("does not exist") || 
                        errorMessage.contains("n'existe pas") || 
                        errorMessage.contains("not found") ||
                        errorMessage.contains("table") && errorMessage.contains("not")) {
                        System.out.println("Table '" + tableName + "' n'existait pas.");
                        successCount++; // Count as success since table is gone
                    } else {
                        System.err.println("Erreur lors de la suppression de la table '" + tableName + "': " + ex.getMessage());
                        if (errors.length() > 0) {
                            errors.append("; ");
                        }
                        errors.append(tableName).append(": ").append(ex.getMessage());
                    }
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
        String[] tableNames = {"utilisateur", "joueur", "equipe", "tournoi", "joueur_equipe", "rencontre", "but"};
        
        System.out.println("=== Statut des tables ===");
        try (Statement st = con.createStatement()) {
            for (String tableName : tableNames) {
                try {
                    try (ResultSet rs = st.executeQuery("SELECT COUNT(*) as count FROM " + tableName)) {
                        if (rs.next()) {
                            int count = rs.getInt("count");
                            System.out.println("Table '" + tableName + "': existe (" + count + " ligne(s))");
                        }
                    }
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
    
    // =================== UNIFIED SCHEMA NOTES ===================
    // The database now uses a unified schema with tournoi_id column for tournament isolation.
    // All tournament data (equipe, joueur, joueur_equipe, rencontre, but) is stored in single tables
    // with tournoi_id NOT NULL column to isolate data per tournament.
    // Legacy tournament-specific table creation/deletion methods have been removed.
    // See createSchema() above for the current unified table structure.

}
