# Phase 0 — État de l'existant (au 2026-08-30)

Périmètre : tout ce qui a été implémenté à ce jour dans `api-compta` (backend) et `code_source_front` (frontend Angular). Vérifié directement dans le code source, pas seulement depuis les notes de session.

## 1. Infrastructure

- `compose.yaml` : Keycloak (port 8081), pgAdmin (5050), PostgreSQL "métier" (5433), PostgreSQL Keycloak (réseau interne uniquement).
- Realm Keycloak custom `erp-comptable` (`keycloak/realm-export.json`), avec 3 clients :
  - `api-compta-admin` — client confidentiel service-account, utilisé côté backend pour administrer Keycloak (créer utilisateurs, assigner rôles).
  - `api-compta-bff` — client confidentiel dédié au grant ROPC du login BFF.
  - `erp-comptable-app` — client public applicatif.
- 17 migrations Flyway appliquées (V1 à V17) : référentiels comptables, entités, classes/comptes/plan comptable, tiers, utilisateurs (+ keycloak_id, email_verifie, mot_de_passe_temporaire, split nom/postNom/prenom), sessions utilisateur, tokens (vérification email / reset password), extension Postgres `unaccent`, seed des 3 référentiels (SYSCOHADA_NORMAL/SMT, SYCEBNL) et du plan comptable SYSCOHADA_NORMAL complet.

## 2. Authentification (BFF) — `AuthController` (`/api/auth`)

| Endpoint | Rôle |
|---|---|
| `POST /login` | public |
| `POST /logout` | authentifié |
| `POST /forgot-password` | public |
| `POST /reset-password` | public (token) |
| `GET /verify-email` | public (token) |
| `POST /resend-verification` | public |
| `POST /definir-mot-de-passe` | authentifié (1er changement de mot de passe temporaire) |

- Grant Keycloak **ROPC** via `api-compta-bff`. Access/refresh token Keycloak chiffrés **AES-256-GCM** et stockés en base (`UserSession`), pas Redis.
- Session applicative par cookie `SESSION_ID` (httpOnly) + protection CSRF double-submit (cookie `XSRF-TOKEN`, header `X-XSRF-TOKEN`).
- `EmailVerifieFilter` bloque les endpoints authentifiés tant que `email_verified` (claim JWT) n'est pas vrai.
- Rate limiting IP en mémoire sur login / forgot-password / resend-verification / création d'entité.
- Protection brute-force Keycloak activée, TLS forcé hors localhost, rotation du refresh token à chaque usage.
- **Audit sécurité réalisé (2026-07-17) : note 7/10.** Non traités volontairement : MFA (incompatible avec ROPC), énumération de compte à la création d'entreprise (mitigée par le rate limiting, pas éliminée), journal d'audit, gestionnaire de secrets (actuellement `.env` en clair), scan de dépendances.

## 3. Création d'entreprise + auto-provisioning admin — `EntiteController` (`/api/entites`)

| Endpoint | Rôle |
|---|---|
| `POST /api/entites` | public |
| `GET /api/entites/moi` | authentifié |

- À la création d'une `Entite`, le demandeur est auto-créé **ADMIN** : compte Keycloak (profil pré-rempli), mot de passe temporaire envoyé par email (via l'API mail Hostinger, appelée directement par le backend), email de vérification, flow de reset password.
- Compensation manuelle en cas d'échec (suppression de l'utilisateur Keycloak si la sauvegarde locale échoue) ; le reste couvert par `@Transactional`.
- Un `ReferentielComptable` (SYSCOHADA_NORMAL, SYSCOHADA_SMT ou SYCEBNL) est choisi à la création ; le plan comptable correspondant est **partagé** entre toutes les entreprises qui utilisent ce référentiel (pas de duplication par entreprise).

## 4. Gestion des utilisateurs (admin) — `UtilisateurController` (`/api/utilisateurs`)

| Endpoint | Rôle |
|---|---|
| `GET /me` | authentifié |
| `POST /api/utilisateurs` | ADMIN |
| `POST /rechercher` | ADMIN |
| `PATCH /{id}/activer` | ADMIN |
| `PATCH /{id}/desactiver` | ADMIN |

- Recherche/tri/pagination via `Specification` JPA, isolation tenant imposée côté serveur (jamais un paramètre client).
- Garde-fou : impossible de désactiver le **dernier ADMIN actif** d'une entreprise (409).
- 3 champs de nom : `nom`, `postNom` (facultatif), `prenom`.
- 7 rôles disponibles (`Role`) : `ADMIN`, `ADMIN_FINANCIER`, `COMPTABLE`, `RH`, `ACHATS`, `CAISSIER`, `LECTURE_SEULE`.

## 5. Plan comptable

**Référentiels** — `ReferentielComptableController` (`/api/referentiels-comptables`)
- `GET` — liste des 3 référentiels disponibles.

**Lecture / recherche** — `PlanComptableController` (`/api/plan-comptable`) et `ClasseCompteComptableController` (`/api/classes-comptables`)

| Endpoint | Description |
|---|---|
| `GET /api/plan-comptable/recap` | référentiel actif + nb classes + nb comptes |
| `GET /api/classes-comptables?q=` | rail des classes, triées par numéro |
| `GET /api/classes-comptables/{id}/comptes?q=&sens=&page=&size=` | comptes paginés d'une classe |

- Recherche insensible aux accents (extension `unaccent`).
- Scope tenant : `entite IS NULL OR entite.id = :entiteId` (comptes standards = `entite` null, sous-comptes spécifiques = `entite` renseignée).

**Création de comptes custom** — `CompteComptableController`

| Endpoint | Rôle |
|---|---|
| `GET /api/classes/{classeId}/comptes` | authentifié (liste légère pour le select "compte parent") |
| `POST /api/comptes` | ADMIN ou ADMIN_FINANCIER |
| `GET /api/comptes/exists?numero=` | authentifié |

- Formulaire stepper 3 étapes côté front.
- Unicité `(plan_comptable_id, numero)` déjà garantie par contrainte DB (V5).
- **Sous-compte** : si un `parentId` est fourni, le **sens débit/crédit est forcé côté serveur** à celui du parent (ignore la valeur envoyée par le client). Sans parent, le sens reste libre.
- Rôle `ADMIN_FINANCIER` : portée **volontairement limitée** à cet endpoint de création de comptes (n'étend pas le module Tiers).

## 6. Module Tiers — `TiersController` (`/api/tiers`)

| Endpoint | Rôle |
|---|---|
| `GET /api/tiers` (recherche/pagination) | authentifié |
| `GET /recap` | authentifié |
| `GET /{id}` | authentifié |
| `POST /api/tiers` | ADMIN ou COMPTABLE |
| `PATCH /{id}` | ADMIN ou COMPTABLE |
| `PATCH /{id}/compte-associe` | ADMIN ou COMPTABLE |
| `DELETE /{id}` (désactivation logique) | ADMIN ou COMPTABLE |

- Types de tiers : CLIENT, FOURNISSEUR, SALARIE, ORGANISME_SOCIAL, AUTRE.
- Association/dissociation de compte comptable via un endpoint dédié (pas fusionné avec le PATCH général) pour éviter l'ambiguïté Jackson "champ absent vs champ null".
- `DELETE` fait toujours une désactivation logique (`actif=false`) : aucune entité Écriture comptable n'existe encore dans le codebase, donc la suppression physique conditionnelle n'a pas d'objet pour l'instant.
- Tests unitaires (`TiersServiceImplTest`, 12 tests) couvrant isolation tenant, validation du compte associé, recap.

## 7. Frontend (`code_source_front`, Angular)

- Routes : `home` (avec onglets internes), `/tiers`, `/plan-comptable`, `/admin/utilisateurs` (guard `admin.guard.ts` qui re-résout systématiquement le profil via `/me`).
- Écrans : connexion, mot de passe oublié/reset, vérification email, liste + recherche + activation/désactivation utilisateurs, plan comptable (rail classes + comptes paginés) avec modale de création de compte (stepper 3 étapes), module Tiers (liste, création stepper 3 étapes, association de compte via rail+panneau).
- Design system partagé : tokens de rôle (`--role-*-bg/fg/border`) réutilisés pour les badges de type de tiers, système de modale-stepper et rail filtrable extraits dans `styles/components.scss` au 2e usage.
- Build `ng build --configuration development` validé sans erreur sur les derniers changements.

## Ce qui n'existe PAS encore (rappel, détaillé dans `doc/a_implementer/`)

- Aucune entité "Écriture comptable" / journal comptable.
- Pas de MFA, pas de journal d'audit des actions sensibles, pas de gestionnaire de secrets, pas de scan de dépendances automatisé.
