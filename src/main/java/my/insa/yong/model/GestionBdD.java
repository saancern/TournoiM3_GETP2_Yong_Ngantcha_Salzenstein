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
            
            // Table but (goals) - dépend de rencontre, equipe et joueur
            "create table but ( "
                + ConnectionPool.sqlForGeneratedKeys(con, "id") + ","
                + " rencontre_id int not null,"
                + " equipe_id int not null,"
                + " joueur_id int not null,"
                + " minute int"
                + ") "
        };
        
        // Définir les contraintes de clés étrangères à ajouter après
        String[] foreignKeyQueries = {
            "alter table joueur_equipe add constraint fk_joueur_equipe_joueur "
                + " foreign key (joueur_id) references joueur(id) on delete cascade",
            
            "alter table joueur_equipe add constraint fk_joueur_equipe_equipe "
                + " foreign key (equipe_id) references equipe(id) on delete cascade",
            
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
                + " foreign key (joueur_id) references joueur(id) on delete cascade"
        };
        
        String[] tableNames = {"utilisateur", "joueur", "equipe", "tournoi", "joueur_equipe", "rencontre", "but"};
        String[] constraintNames = {"fk_joueur_equipe_joueur", "fk_joueur_equipe_equipe", 
                                   "fk_rencontre_tournoi", "fk_rencontre_equipe_a", "fk_rencontre_equipe_b", 
                                   "fk_rencontre_winner", "fk_but_rencontre", "fk_but_equipe", "fk_but_joueur"};
        
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
            "alter table rencontre drop constraint if exists fk_rencontre_tournoi",
            "alter table rencontre drop constraint if exists fk_rencontre_equipe_a",
            "alter table rencontre drop constraint if exists fk_rencontre_equipe_b",
            "alter table rencontre drop constraint if exists fk_rencontre_winner",
            "alter table joueur_equipe drop constraint if exists fk_joueur_equipe_joueur",
            "alter table joueur_equipe drop constraint if exists fk_joueur_equipe_equipe"
        };
        
        String[] constraintNames = {"fk_but_rencontre", "fk_but_equipe", "fk_but_joueur",
                                   "fk_rencontre_tournoi", "fk_rencontre_equipe_a", "fk_rencontre_equipe_b", 
                                   "fk_rencontre_winner", "fk_joueur_equipe_joueur", "fk_joueur_equipe_equipe"};
        
        // Définir les tables à supprimer dans l'ordre inverse des dépendances
        String[] tableNames = {"but", "rencontre", "joueur_equipe", "tournoi", "equipe", "joueur", "utilisateur"};
        
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
    
    // =================== TOURNAMENT-SPECIFIC TABLE METHODS ===================
    
    /**
     * Creates tournament-specific tables for a given tournament ID
     * Tables created: equipe_X, joueur_X, joueur_equipe_X, rencontre_X, but_X
     * @param con Database connection
     * @param tournoiId Tournament ID
     * @throws SQLException If creation fails
     */
    public static void createTournamentTables(Connection con, int tournoiId) throws SQLException {
        System.out.println("=== Création des tables pour le tournoi " + tournoiId + " ===");
        
        // Generate tournament-specific table creation queries
        String[] createTableQueries = {
            // Table equipe_X
            "create table equipe_" + tournoiId + " ( "
                + ConnectionPool.sqlForGeneratedKeys(con, "id") + ","
                + " nom_equipe varchar(50) not null,"
                + " date_creation date not null"
                + ") ",
            
            // Table joueur_X  
            "create table joueur_" + tournoiId + " ( "
                + ConnectionPool.sqlForGeneratedKeys(con, "id") + ","
                + " prenom varchar(50) not null,"
                + " nom varchar(50) not null,"
                + " taille double precision not null,"
                + " age int not null,"
                + " sexe char(1) check (sexe in ('F','H'))"
                + ") ",
            
            // Table joueur_equipe_X
            "create table joueur_equipe_" + tournoiId + " ( "
                + " joueur_id int not null,"
                + " equipe_id int not null,"
                + " primary key (joueur_id, equipe_id)"
                + ") ",
            
            // Table rencontre_X
            "create table rencontre_" + tournoiId + " ( "
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
            
            // Table but_X
            "create table but_" + tournoiId + " ( "
                + ConnectionPool.sqlForGeneratedKeys(con, "id") + ","
                + " rencontre_id int not null,"
                + " equipe_id int not null,"
                + " joueur_id int not null,"
                + " minute int"
                + ") "
        };
        
        String[] tableNames = {"equipe_" + tournoiId, "joueur_" + tournoiId, 
                              "joueur_equipe_" + tournoiId, "rencontre_" + tournoiId, "but_" + tournoiId};
        
        boolean oldAutoCommit = con.getAutoCommit();
        SQLException lastException = null;
        int successCount = 0;
        
        try {
            con.setAutoCommit(false);
            
            try (Statement st = con.createStatement()) {
                // Create tables
                for (int i = 0; i < createTableQueries.length; i++) {
                    try {
                        st.executeUpdate(createTableQueries[i]);
                        System.out.println("Table '" + tableNames[i] + "' créée avec succès.");
                        successCount++;
                    } catch (SQLException ex) {
                        String errorMessage = ex.getMessage().toLowerCase();
                        if (errorMessage.contains("already exists") || 
                            errorMessage.contains("déjà exist") || 
                            errorMessage.contains("table") && errorMessage.contains("exist")) {
                            System.out.println("Table '" + tableNames[i] + "' existe déjà.");
                            successCount++;
                        } else {
                            System.err.println("Erreur lors de la création de la table '" + tableNames[i] + "': " + ex.getMessage());
                            lastException = ex;
                        }
                    }
                }
                
                // Add foreign key constraints
                addTournamentConstraints(con, st, tournoiId);
                
                if (successCount > 0) {
                    con.commit();
                    System.out.println("Tables du tournoi " + tournoiId + " créées avec succès (" + successCount + "/" + tableNames.length + ").");
                } else {
                    con.rollback();
                    System.err.println("Aucune table n'a pu être créée pour le tournoi " + tournoiId + ".");
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
        
        if (successCount == 0 && lastException != null) {
            throw lastException;
        }
    }
    
    /**
     * Adds foreign key constraints for tournament-specific tables
     */
    private static void addTournamentConstraints(Connection con, Statement st, int tournoiId) throws SQLException {
        String[] constraintQueries = {
            "alter table joueur_equipe_" + tournoiId + " add constraint fk_joueur_equipe_joueur_" + tournoiId +
                " foreign key (joueur_id) references joueur_" + tournoiId + "(id) on delete cascade",
            
            "alter table joueur_equipe_" + tournoiId + " add constraint fk_joueur_equipe_equipe_" + tournoiId +
                " foreign key (equipe_id) references equipe_" + tournoiId + "(id) on delete cascade",
            
            "alter table rencontre_" + tournoiId + " add constraint fk_rencontre_tournoi_" + tournoiId +
                " foreign key (tournoi_id) references tournoi(id) on delete cascade",
            
            "alter table rencontre_" + tournoiId + " add constraint fk_rencontre_equipe_a_" + tournoiId +
                " foreign key (equipe_a_id) references equipe_" + tournoiId + "(id) on delete cascade",
            
            "alter table rencontre_" + tournoiId + " add constraint fk_rencontre_equipe_b_" + tournoiId +
                " foreign key (equipe_b_id) references equipe_" + tournoiId + "(id) on delete cascade",
            
            "alter table rencontre_" + tournoiId + " add constraint fk_rencontre_winner_" + tournoiId +
                " foreign key (winner_id) references equipe_" + tournoiId + "(id) on delete set null",
            
            "alter table but_" + tournoiId + " add constraint fk_but_rencontre_" + tournoiId +
                " foreign key (rencontre_id) references rencontre_" + tournoiId + "(id) on delete cascade",
            
            "alter table but_" + tournoiId + " add constraint fk_but_equipe_" + tournoiId +
                " foreign key (equipe_id) references equipe_" + tournoiId + "(id) on delete cascade",
            
            "alter table but_" + tournoiId + " add constraint fk_but_joueur_" + tournoiId +
                " foreign key (joueur_id) references joueur_" + tournoiId + "(id) on delete cascade"
        };
        
        String[] constraintNames = {
            "fk_joueur_equipe_joueur_" + tournoiId, "fk_joueur_equipe_equipe_" + tournoiId,
            "fk_rencontre_tournoi_" + tournoiId, "fk_rencontre_equipe_a_" + tournoiId, 
            "fk_rencontre_equipe_b_" + tournoiId, "fk_rencontre_winner_" + tournoiId,
            "fk_but_rencontre_" + tournoiId, "fk_but_equipe_" + tournoiId, "fk_but_joueur_" + tournoiId
        };
        
        System.out.println("=== Ajout des contraintes pour le tournoi " + tournoiId + " ===");
        for (int i = 0; i < constraintQueries.length; i++) {
            try {
                st.executeUpdate(constraintQueries[i]);
                System.out.println("Contrainte '" + constraintNames[i] + "' ajoutée avec succès.");
            } catch (SQLException ex) {
                String errorMessage = ex.getMessage().toLowerCase();
                if (errorMessage.contains("already exists") || 
                    errorMessage.contains("déjà exist") || 
                    errorMessage.contains("constraint") && errorMessage.contains("exist")) {
                    System.out.println("Contrainte '" + constraintNames[i] + "' existe déjà.");
                } else {
                    System.err.println("Erreur lors de l'ajout de la contrainte '" + constraintNames[i] + "': " + ex.getMessage());
                }
            }
        }
    }
    
    /**
     * Deletes all tournament-specific tables for a given tournament ID
     * @param con Database connection
     * @param tournoiId Tournament ID (cannot be 1, the default tournament)
     * @throws SQLException If deletion fails
     */
    public static void deleteTournamentTables(Connection con, int tournoiId) throws SQLException {
        if (tournoiId == 1) {
            throw new SQLException("Le tournoi par défaut (ID=1) ne peut pas être supprimé.");
        }
        
        System.out.println("=== Suppression des tables pour le tournoi " + tournoiId + " ===");
        
        // Drop constraints first, then tables
        String[] constraintQueries = {
            "alter table but_" + tournoiId + " drop constraint if exists fk_but_rencontre_" + tournoiId,
            "alter table but_" + tournoiId + " drop constraint if exists fk_but_equipe_" + tournoiId,
            "alter table but_" + tournoiId + " drop constraint if exists fk_but_joueur_" + tournoiId,
            "alter table rencontre_" + tournoiId + " drop constraint if exists fk_rencontre_tournoi_" + tournoiId,
            "alter table rencontre_" + tournoiId + " drop constraint if exists fk_rencontre_equipe_a_" + tournoiId,
            "alter table rencontre_" + tournoiId + " drop constraint if exists fk_rencontre_equipe_b_" + tournoiId,
            "alter table rencontre_" + tournoiId + " drop constraint if exists fk_rencontre_winner_" + tournoiId,
            "alter table joueur_equipe_" + tournoiId + " drop constraint if exists fk_joueur_equipe_joueur_" + tournoiId,
            "alter table joueur_equipe_" + tournoiId + " drop constraint if exists fk_joueur_equipe_equipe_" + tournoiId
        };
        
        String[] tableNames = {"but_" + tournoiId, "rencontre_" + tournoiId, "joueur_equipe_" + tournoiId, 
                              "joueur_" + tournoiId, "equipe_" + tournoiId};
        
        try (Statement st = con.createStatement()) {
            // Drop constraints
            for (String query : constraintQueries) {
                try {
                    st.executeUpdate(query);
                } catch (SQLException ex) {
                    // Ignore constraint not found errors
                }
            }
            
            // Drop tables
            for (String tableName : tableNames) {
                try {
                    st.executeUpdate("drop table if exists " + tableName);
                    System.out.println("Table '" + tableName + "' supprimée avec succès.");
                } catch (SQLException ex) {
                    System.err.println("Erreur lors de la suppression de la table '" + tableName + "': " + ex.getMessage());
                }
            }
        }
        
        System.out.println("Tables du tournoi " + tournoiId + " supprimées.");
    }
    
    /**
     * Utility method to get tournament-specific table name
     * @param baseTableName Base table name (e.g., "equipe", "joueur", "rencontre", "but")
     * @param tournoiId Tournament ID
     * @return Tournament-specific table name (e.g., "equipe_2", "joueur_3")
     */
    public static String getTournamentTableName(String baseTableName, int tournoiId) {
        return baseTableName + "_" + tournoiId;
    }
    
    /**
     * Checks if tournament-specific tables exist for a given tournament ID
     * @param con Database connection
     * @param tournoiId Tournament ID
     * @return true if all tournament tables exist
     */
    public static boolean tournamentTablesExist(Connection con, int tournoiId) throws SQLException {
        String[] tableNames = {"equipe_" + tournoiId, "joueur_" + tournoiId, 
                              "joueur_equipe_" + tournoiId, "rencontre_" + tournoiId, "but_" + tournoiId};
        
        try (Statement st = con.createStatement()) {
            for (String tableName : tableNames) {
                try {
                    ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tableName);
                    rs.close();
                } catch (SQLException ex) {
                    return false; // Table doesn't exist
                }
            }
        }
        return true; // All tables exist
    }

}
