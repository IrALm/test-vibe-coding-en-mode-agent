-- ============================================================================
-- Script DE RÉFÉRENCE — provisioning PostgreSQL préalable (HORS application)
-- ============================================================================
-- Ce script n'est PAS exécuté automatiquement par l'application "compta".
-- Le provisioning PostgreSQL (CREATE DATABASE / CREATE ROLE / GRANT) est
-- volontairement hors Flyway et hors application : il documente les
-- commandes à exécuter manuellement par un administrateur PostgreSQL.
--
-- Ordre d'exécution :
--   1) Exécuter la section "1) Base d'administration" UNE SEULE FOIS, lors
--      de l'installation initiale de la plateforme.
--   2) Pour CHAQUE nouveau tenant, dupliquer et adapter la section
--      "2) Base tenant" AVANT d'appeler POST /admin/tenants avec les MÊMES
--      identifiants (dbHost, dbPort, dbName, dbUsername, dbPassword) que
--      ceux utilisés ici. Si la base/le rôle n'existent pas encore, le
--      provisioning applicatif échoue (HTTP 422) et le tenant reste en
--      statut FAILED (voir POST /admin/tenants/{id}/retry-migration une
--      fois la base corrigée).
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 1) Base d'administration (annuaire des tenants) — exécuté une seule fois
-- ----------------------------------------------------------------------------
CREATE ROLE compta_admin WITH LOGIN PASSWORD 'CHANGE_ME';
CREATE DATABASE postgres_admin_db OWNER compta_admin;
GRANT ALL PRIVILEGES ON DATABASE postgres_admin_db TO compta_admin;


-- ----------------------------------------------------------------------------
-- 2) Base tenant — à dupliquer et adapter pour CHAQUE nouveau tenant
--    Exemple ci-dessous pour le tenant pilote "École LAPEREAUX"
-- ----------------------------------------------------------------------------
CREATE ROLE ecole_lapereaux_owner WITH LOGIN PASSWORD 'CHANGE_ME';
CREATE DATABASE ecole_lapereaux OWNER ecole_lapereaux_owner;
GRANT ALL PRIVILEGES ON DATABASE ecole_lapereaux TO ecole_lapereaux_owner;

-- Une fois cette section exécutée, appeler l'API admin (authentifiée avec
-- un jeton du realm admin, rôle SUPER_ADMIN) :
--
--   POST /admin/tenants
--   {
--     "name": "École LAPEREAUX",
--     "clientType": "ECOLE",
--     "keycloakRealm": "ecole-lapereaux",
--     "dbName": "ecole_lapereaux",
--     "dbHost": "localhost",
--     "dbPort": 5432,
--     "dbUsername": "ecole_lapereaux_owner",
--     "dbPassword": "CHANGE_ME"
--   }
--
-- L'application se connecte avec ces identifiants pour exécuter les
-- migrations Flyway du schéma tenant (création des schémas vides
-- parametrage, comptabilite, tresorerie, tiers, audit, reporting).
-- En cas de succès, le tenant passe en statut ACTIVE ; en cas d'échec
-- (base/rôle absents, identifiants invalides...), il reste en statut FAILED.
-- ============================================================================


-- ----------------------------------------------------------------------------
-- Prérequis Keycloak (hors scope de ce script et de l'application, à faire
-- manuellement dans la console d'administration Keycloak) :
--   - Un realm "admin" (nom configurable via compta.keycloak.admin-realm),
--     avec un rôle realm SUPER_ADMIN attribué aux comptes habilités à gérer
--     l'annuaire des tenants (endpoints /admin/**).
--   - Un realm tenant "ecole-lapereaux" (pilote), créé manuellement dans
--     Keycloak, distinct du realm admin. Son nom doit correspondre
--     exactement à la valeur "keycloakRealm" fournie à POST /admin/tenants.
-- ----------------------------------------------------------------------------
