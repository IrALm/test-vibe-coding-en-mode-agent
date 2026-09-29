-- Schémas vides d'une base tenant. Aucune table métier dans ce lot
-- (socle multi-tenant uniquement) : le plan de comptes, les écritures, etc.
-- seront ajoutés dans un lot ultérieur.
CREATE SCHEMA IF NOT EXISTS parametrage;
CREATE SCHEMA IF NOT EXISTS comptabilite;
CREATE SCHEMA IF NOT EXISTS tresorerie;
CREATE SCHEMA IF NOT EXISTS tiers;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS reporting;
