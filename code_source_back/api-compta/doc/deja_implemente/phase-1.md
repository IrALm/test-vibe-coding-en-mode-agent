# Phase 1 — Moteur comptable (au 2026-09-07)

Périmètre : tout ce qui a été implémenté pour la Phase 1 (exercices, journal général,
écritures en partie double, validation, contre-passation, clôture, grand livre, balance)
dans `api-compta` (backend, package `moteurComptable`) et `code_source_front` (frontend
Angular, `pages/comptabilite/`). Vérifié directement dans le code source. Complète
`phase-0.md` (auth, entreprise, utilisateurs, plan comptable, tiers) sans le dupliquer.

## 1. Modèle de données

```
Entite
  └── ExerciceComptable (id, dateDebut, dateFin, statut OUVERT/CLOS)
        └── Ecriture (id, date, reference, libelle, statut, numero, genereParCloture)
              ├── Journal (singleton par entité, dernierNumero)
              ├── creePar / validePar → Utilisateur (vraies relations @ManyToOne)
              └── LigneEcriture (compte, sens, montant, libelle) × N
```

- `Journal` : **un seul journal ("Journal général") par entité**, contrainte unique sur
  `entite_id`, provisionné paresseusement au premier exercice/écriture
  (`JournalProvisioningService`) — pas de journaux auxiliaires (achats, ventes, banque...) en
  Phase 1, **reporté à la Phase 2 par choix explicite** (pas d'endpoint `/api/journaux`).
- `ExerciceComptable` : un seul `OUVERT` par entité, imposé par un **index unique partiel**
  en base (`WHERE statut = 'OUVERT'`), pas seulement une vérification applicative.
- `Ecriture.numero` : **nullable**, assigné uniquement à la validation (jamais au brouillon)
  — sinon un brouillon abandonné laisserait un trou dans la numérotation légale du journal.
  Compteur `Journal.dernierNumero` + verrou de ligne (`@Lock(PESSIMISTIC_WRITE)`) pour
  l'incrémenter atomiquement ; contrainte unique `(journal_id, numero)` en filet de sécurité.
- `Ecriture.genereParCloture` : marque une écriture système générée par la clôture (cf. §5) —
  n'est jamais créée par un utilisateur.
- `LigneEcriture.sens` réutilise l'enum `SensCompte` déjà existant (module plan comptable).

## 2. Exercices — `ExerciceComptableController` (`/api/exercices`)

| Endpoint | Rôle |
|---|---|
| `GET` | authentifié (liste) |
| `GET /ouvert` | authentifié |
| `GET /{id}/resultat` | authentifié |
| `GET /{id}/cloture-check` | ADMIN |
| `POST` | ADMIN |
| `POST /{id}/cloturer` | ADMIN |

- Création : dates cohérentes (fin > début), pas de chevauchement avec un exercice existant,
  durée max 12 mois (24 mois pour le tout premier exercice de l'entreprise — pas d'hypothèse
  de 365 jours fixes).
- Un utilisateur (COMPTABLE/ADMIN_FINANCIER/ADMIN) qui saisit une date d'écriture hors des
  bornes de l'exercice, ou sur un exercice `CLOS`, est rejeté (`ConflitException`, revérifié à
  la création, la modification **et** la soumission — jamais fait confiance à une validation
  antérieure).
- **Clôture** (`cloturerExercice`) : revérifie côté serveur qu'aucune écriture `BROUILLON`/
  `EN_ATTENTE` ne subsiste (jamais fait confiance à un appel précédent à `cloture-check`), puis
  génère l'écriture de clôture si nécessaire (§5) avant de passer l'exercice à `CLOS`.
- **Résultat provisoire** (`obtenirResultat`) : calculé à la volée, `Σ(crédit − débit)` des
  lignes de comptes classes 6/7/8 sur les écritures **validées** de l'exercice — jamais stocké
  sur `ExerciceComptable`. Exclut l'écriture de clôture elle-même (`genereParCloture`, cf. §5).

## 3. Écritures — `EcritureController` (`/api/ecritures`)

| Endpoint | Rôle |
|---|---|
| `GET` (recherche/pagination) | authentifié |
| `GET /{id}` | authentifié |
| `GET /stats?exerciceId=` | authentifié |
| `POST` | ADMIN, ADMIN_FINANCIER ou COMPTABLE |
| `PATCH /{id}` | ADMIN, ADMIN_FINANCIER ou COMPTABLE |
| `DELETE /{id}` | ADMIN, ADMIN_FINANCIER ou COMPTABLE |
| `POST /{id}/soumettre` | ADMIN, ADMIN_FINANCIER ou COMPTABLE |
| `POST /{id}/valider` | ADMIN ou ADMIN_FINANCIER |
| `POST /{id}/renvoyer-en-brouillon` | ADMIN ou ADMIN_FINANCIER |
| `POST /{id}/contre-passer` | ADMIN ou ADMIN_FINANCIER |

**Workflow** : `BROUILLON` → (soumettre) → `EN_ATTENTE` → (valider) → `VALIDEE`, ou
(renvoyer en brouillon, avec motif) → retour `BROUILLON`. Auto-validation autorisée (pas de
vérification créateur ≠ validateur). **Modifier/supprimer un brouillon n'est pas restreint à
son créateur** — n'importe quel rôle de saisie de l'entreprise peut reprendre le brouillon
d'un collègue (décision explicite du user, 2026-09-07 — le front ne doit pas non plus
restreindre ça, piège déjà corrigé une fois).

- **Partie double** : `≥ 2` lignes obligatoires (form `@NotEmpty` + vérification serveur),
  montant `> 0` (`@DecimalMin("0.01")`). Équilibre (`Σdébit = Σcrédit`) vérifié à la
  **soumission** et **revérifié à la validation** (jamais fait confiance au statut calculé lors
  de la soumission).
- **Compte désactivé rejeté** dans une ligne d'écriture (`ConflitException` 409, création et
  modification) — corrigé le 2026-09-07, cf. piège Lombok associé plus bas.
- **Immutabilité post-validation** : aucune route ne permet d'éditer/supprimer une écriture
  `VALIDEE` (`modifierEcriture`/`supprimerEcriture` exigent `statut == BROUILLON`, sinon 409).
  Seule la **contre-passation** permet de l'annuler : crée une écriture miroir en `BROUILLON`
  (lignes dupliquées, sens inversé), rattachée à l'exercice **OUVERT courant** (jamais à celui
  de l'originale, qui peut être clôturé). Le miroir repasse par la file de validation normale ;
  l'originale ne bascule en `CONTREPASSEE` que lorsque le miroir est **validé** (pas à la
  demande de contre-passation), pour ne jamais laisser grand livre/balance temporairement
  incohérents.
- Sens débit/crédit d'une ligne **n'est jamais conditionné** par le compte choisi (ni back ni
  front) — un compte peut légitimement être débité ou crédité selon l'opération (ex. 411
  Clients : débité à la facturation, crédité au règlement). Volontaire, pas un oubli.

## 4. Grand livre / balance — `GrandLivreBalanceController` (`/api/comptabilite`)

| Endpoint | Rôle |
|---|---|
| `GET /grand-livre?exerciceId=&compteId=` | authentifié |
| `GET /balance?exerciceId=` | authentifié |

Les deux sont des **projections dérivées**, jamais stockées : calculées à chaque appel à
partir des `LigneEcriture` des écritures **validées** uniquement.

- **Solde d'ouverture (à-nouveaux)** : cumul de toutes les lignes validées antérieures à la
  date de début de l'exercice, **uniquement pour les comptes de bilan (classes 1 à 5)** — un
  compte de gestion (6/7/8) reste à zéro à chaque exercice. Même formule utilisée dans le
  grand livre (`calculerSoldeAvant`, par compte) et la balance (`calculerSoldesOuvertureBilan`,
  pour tous les comptes de bilan d'un coup) — **garantit que le solde d'un compte donné est
  identique sur les deux écrans**, y compris pour un compte de bilan sans aucun mouvement sur
  l'exercice courant (il apparaît quand même dans la balance avec son solde reporté). Corrigé
  le 2026-09-07 — avant, la balance ignorait complètement le solde d'ouverture.

## 5. Clôture : écriture de solde de gestion + report du résultat (2026-09-07)

Sans mécanisme dédié, le résultat d'un exercice clôturé ne "landait" jamais sur le bilan, et la
balance de l'exercice suivant ne pouvait jamais s'équilibrer dès qu'un exercice précédent avait
un résultat non nul. `cloturerExercice()` génère donc, **si le résultat n'est pas exactement
nul**, une écriture système avant de passer l'exercice à `CLOS` :

- Une ligne de sens opposé par compte de gestion (6/7/8) mouvementé sur l'exercice, pour le
  ramener à zéro (`LigneEcritureRepository.calculerSoldesGestion`).
- Une ligne sur le compte de résultat SYSCOHADA (seedé en V15, référentiel SYSCOHADA_NORMAL
  uniquement — **à généraliser le jour où SYCEBNL/SMT ont leur propre plan seedé**) : **111**
  "Résultat net : bénéfice" si résultat > 0, **119** "Résultat net : perte" si < 0. 409 si ce
  compte est introuvable dans le plan.

Cette écriture est directement `VALIDEE` (numérotée via le même verrou de journal que
`valider()`), datée de la fin de l'exercice, et marquée `genereParCloture = true` — ce qui
l'**exclut de `calculerResultat`** (sinon elle s'annulerait elle-même dans le résultat de
l'exercice qu'elle vient de clôturer, puisqu'elle solde exactement les comptes qu'il somme).
Elle reste en revanche visible dans le grand livre/la balance de l'exercice clos, et surtout,
via le compte 111/119 (classe 1, un compte de bilan), correctement reprise comme solde
d'ouverture de l'exercice suivant — c'est ce qui fait enfin coïncider le résultat cumulé d'un
exercice à l'autre. Aucune écriture générée si le résultat est exactement nul (rien à
reporter ; les comptes de gestion non soldés dans ce cas précis n'ont aucun impact, puisqu'ils
sont de toute façon exclus par construction du solde d'ouverture des exercices suivants).

Risque connu, non traité : rien n'empêche aujourd'hui de contre-passer une écriture de
clôture comme une écriture normale.

## 6. Pièges résolus pendant la Phase 1

- **Lombok `@Builder` + initialiseur inline sans `@Builder.Default`** : `CompteComptable.actif`
  (`private boolean actif = true;`) était ignoré par le builder, qui produisait silencieusement
  `actif = false` dès qu'un appelant omettait `.actif(...)` — sans impact en prod (le seed SQL
  et `CompteComptableCreationServiceImpl` fixent toujours `actif` explicitement) mais aurait
  fait échouer tout nouveau test construisant un `CompteComptable` sans `.actif(true)` une fois
  le contrôle de compte désactivé ajouté. **Réflexe à appliquer à tout champ avec initialiseur
  inline dans une entité `@Builder`** — d'autres entités du projet ont le même défaut
  (`Utilisateur`, `Entite`, `Tiers`, `PlanComptable`, warnings visibles à la compilation), pas
  corrigées tant qu'aucun contrôle n'en dépend.
- `Object[]` unique passé à `List.of(...)` dans un test (`List.of(new Object[]{...})`) est
  ambigu pour l'inférence de type Java (le varargs "spread" l'array au lieu d'en faire un
  élément unique) — nécessite `List.<Object[]>of(...)` avec le witness de type explicite dès
  qu'il n'y a qu'un seul élément.

## 7. Front (`code_source_front/src/app/pages/comptabilite/`)

8 écrans : tableau de bord, gestion des exercices, liste des écritures (journal général),
saisie d'écriture, détail d'écriture, file de validation, grand livre, balance générale.
Routes plates top-level (`/comptabilite`, `/comptabilite/ecritures`, etc.), guard dédié
`validation.guard.ts` (ADMIN/ADMIN_FINANCIER) sur la file de validation uniquement.

- Implémentation initiale suivie d'une **refonte visuelle complète** (2026-09-07) pour coller
  précisément aux maquettes `Comptano - *.dc.html` — détail des patterns transverses
  (`.fil-ariane`, `.compte-rail-item`, `.ligne-synthese`, `.toast`, modale "Choisir un compte",
  accordion classes→comptes du grand livre) documenté plus haut dans ce fichier
  (section "Phase 1 — Front, refonte visuelle").
- Détail d'écriture reste une **page dédiée** (pas de maquette propre, la maquette le montre en
  modale sur la liste) — décision actée, ne pas re-proposer la conversion sans motif nouveau.
- Dates affichées en `dd/MM/yyyy` (Angular `DatePipe`) partout dans le module — les DTOs
  transmettent des dates ISO brutes, jamais affichées telles quelles.

## 8. Tests

91/91 tests Mockito verts (`EcritureServiceImplTest` 19, `ExerciceComptableServiceImplTest` 14,
`GrandLivreBalanceServiceImplTest` 8, plus les tests des modules Phase 0). Couvrent notamment :
équilibre partie double, immutabilité post-validation, contre-passation, compte désactivé
rejeté, réconciliation balance/grand livre (compte avec solde d'ouverture, avec et sans
mouvement sur l'exercice), génération de l'écriture de clôture (bénéfice, perte, résultat nul,
compte de résultat introuvable).

## 9. Critères d'acceptation §1.6 du cahier des charges — vérifiés le 2026-09-07

| # | Critère | Statut |
|---|---|---|
| 1 | Créer un exercice (rôle autorisé, dates cohérentes, OUVERT par défaut) | ✅ |
| 2 | Créer/lister les journaux (au moins le journal général) | ❌ reporté Phase 2 (choix explicite) |
| 3 | Brouillon ≥2 lignes, validation bloquée si Σdébit≠Σcrédit | ✅ |
| 4 | Écriture validée immuable, seule la contre-passation l'annule | ✅ |
| 5 | Grand livre / balance calculés auto, cohérents entre eux | ✅ (corrigé le 2026-09-07) |
| 6 | Clôture bloque + génère les à-nouveaux | ✅ (écriture de clôture, §5) |
| 7 | Écriture hors bornes / exercice clos rejetée | ✅ |

## Ce qui n'existe PAS encore (reporté)

- Aucun endpoint `/api/journaux` (créer/lister) — un seul journal général auto-provisionné,
  pas de CRUD. Reporté à la Phase 2 par choix explicite du user.
- Journaux auxiliaires (achats, ventes, banque, caisse, OD) — Phase 2.
- Pièce justificative (`Ecriture.reference`) non obligatoire à la soumission (le texte du
  cahier des charges le suggère) — laissé optionnel pour l'instant, pas de blocage prévu.
- Compte de résultat (111/119) codé en dur pour le référentiel SYSCOHADA_NORMAL — à généraliser
  si SYCEBNL/SMT reçoivent un plan comptable seedé un jour.
