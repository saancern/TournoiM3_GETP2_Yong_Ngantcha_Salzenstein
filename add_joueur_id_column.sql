-- Script pour ajouter la colonne joueur_id à la table utilisateur
-- À exécuter sur la base MySQL de production

USE m3_syong01;

-- Ajouter la colonne joueur_id si elle n'existe pas
ALTER TABLE utilisateur 
ADD COLUMN IF NOT EXISTS joueur_id INT NULL
AFTER isAdmin;

-- Ajouter la contrainte de clé étrangère
ALTER TABLE utilisateur 
ADD CONSTRAINT fk_utilisateur_joueur 
FOREIGN KEY (joueur_id) REFERENCES joueur(id) 
ON DELETE SET NULL;

-- Vérifier que la colonne a été ajoutée
DESCRIBE utilisateur;

SELECT 'Colonne joueur_id ajoutée avec succès !' AS Status;
