# api-compta — mémoire projet partagée pour Claude Code

Ce fichier est chargé automatiquement par Claude Code dans toute session ouverte sur ce
repo, quel que soit le développeur ou la machine. Il centralise les décisions d'architecture
déjà tranchées, les pièges déjà résolus, et l'état d'avancement — **pour éviter de
re-débattre une question déjà réglée ou de retomber dans un bug déjà corrigé.**

**Règle de maintenance (s'applique à toute session Claude sur ce repo) :** avant de
terminer une tâche qui a impliqué une décision d'architecture non évidente, la résolution
d'un bug non trivial, ou l'achèvement d'un module/endpoint, **mettre à jour ce fichier**
(section concernée ci-dessous, ou `doc/deja_implemente/phase-0.md` pour le détail des
endpoints) **avant que le développeur ne committe.** Une décision qui n'est pas écrite ici
n'existe pas pour les autres sessions.

Ne pas ajouter ici les préférences de collaboration personnelles d'un développeur avec son
Claude (style de communication, qui lance les serveurs, etc.) — ça reste dans la mémoire
locale de chacun, pas dans ce fichier partagé.

---

## Repo et périmètre

- Backend actif : ce repo (`code_source_back/api-compta`, Spring Boot). Frontend actif :
  `code_source_front/` (Angular), sibling de `code_source_back/`.
- `erp-comptable/` (backend + frontend), s'il existe à côté, est un **essai antérieur
  abandonné** — ne pas y toucher, ne pas s'y référer.
- Documentation complémentaire :
  - `doc/deja_implemente/phase-0.md` — état détaillé de l'existant avant la Phase 1 (auth,
    entreprise, utilisateurs, plan comptable, tiers).
  - `doc/deja_implemente/phase-1.md` — état détaillé du moteur comptable (exercices, journal
    général, écritures, validation, contre-passation, clôture, grand livre, balance) : modèle
    de données, endpoints/rôles, règles métier, critères d'acceptation §1.6 vérifiés un par un.
    À tenir à jour à l'achèvement de chaque module ; créer un `phase-N.md` similaire pour
    chaque phase suivante plutôt que d'agrandir un seul fichier.
  - `doc/a_implementer/cahier-des-charges.md` — cahier des charges métier des Phases 1 à 7
    (moteur comptable, facturation, immobilisations, RH, GED, états financiers, reporting)
    du point de vue chef de projet + expert-comptable OHADA. Phases 2 à 7 pas commencées.
  - `doc-authentification.md` — doc technique canonique du flow login/logout BFF (diagramme
    Mermaid, tableau des composants).
  - `doc/ci.md` — pipeline GitLab (`.gitlab-ci.yml`) : stages, déclencheurs (MR vs push
    `main`), garde-fou de flux de branches, pièges résolus (Keycloak eager JWT discovery en
    test), justification des seuils JaCoCo/Checkstyle. CD pas encore en place (pas d'infra).

## Décisions d'architecture déjà tranchées (ne pas re-proposer sans motif nouveau)

**Authentification (BFF)**
- Login/logout backend-mediated : grant Keycloak **ROPC** via un client confidentiel dédié
  `api-compta-bff` (distinct de `api-compta-admin` service-account et de `erp-comptable-app`
  public). Limite connue et acceptée : incompatible avec toute MFA.
- Tokens Keycloak (access + refresh) chiffrés **AES-256-GCM**, stockés dans `UserSession`
  (Postgres) — pas Redis (working set petit, écritures rares).
- Session applicative par cookie `SESSION_ID` httpOnly + CSRF double-submit (cookie
  `XSRF-TOKEN` en `Path=/`, header `X-XSRF-TOKEN`).
- `EmailVerifieFilter` bloque les endpoints authentifiés tant que le claim JWT
  `email_verified` n'est pas vrai — doit exempter explicitement les mêmes routes publiques
  que `SecurityConfig` (les filtres Spring Security s'exécutent avant toute vérification
  `permitAll()`).
- **Un seul DTO de lecture par entité métier, réutilisé partout** (ex. `UtilisateurReadDto`
  pour login, `/me`, création) — champs non pertinents à `null`/défaut plutôt qu'un DTO
  dédié par endpoint. Préférence forte et explicite : pas de multiplication de records.
- Audit sécurité du 2026-07-17, note 7/10. Corrigés : brute-force protection Keycloak, TLS
  forcé hors localhost, rotation refresh token, CSRF double-submit, rate limiting IP mémoire
  (login/forgot-password/resend-verification/création d'entité). Non corrigés
  volontairement : MFA, énumération de compte à la création d'entreprise (mitigée pas
  éliminée), journal d'audit, gestionnaire de secrets (`.env` en clair), scan de dépendances.

**Création d'entreprise + auto-provisioning admin**
- Plan comptable **partagé par référentiel** (SYSCOHADA_NORMAL/SMT, SYCEBNL), jamais dupliqué
  par entité. `CompteComptable.entite` (nullable) sert uniquement aux comptes/sous-comptes
  personnalisés d'une entreprise.
- Emails envoyés **par le backend lui-même** (API mail Hostinger), pas par Keycloak. Pas de
  `requiredActions` Keycloak — le grant ROPC ne supporte aucune étape interactive.
  "Mot de passe à changer" géré applicativement (`Utilisateur.motDePasseTemporaire`).
- Compensation manuelle en cas d'échec (suppression Keycloak si sauvegarde locale échoue),
  pas de saga complexe. Le reste couvert par `@Transactional`.
- Seed des comptes officiels SYSCOHADA/SYCEBNL explicitement hors scope initial (seules les 3
  lignes `ReferentielComptable` étaient seedées avant le seed complet SYSCOHADA_NORMAL).

**Gestion des utilisateurs (admin)**
- Isolation tenant imposée **côté serveur uniquement** (jamais un paramètre client) : la
  `Specification` de recherche impose l'`entiteId` résolu depuis le JWT appelant.
  `activer`/`desactiver` renvoient **404** (pas 403) si l'utilisateur cible appartient à une
  autre entreprise — ne confirme pas l'existence d'un compte ailleurs.
- Garde-fou : impossible de désactiver le **dernier ADMIN actif** d'une entreprise (409).
- Recherche en `POST /api/utilisateurs/rechercher` (corps JSON de critères), pas en `GET`.
- 3 champs de nom distincts (convention RDC/Afrique centrale) : `nom`, `postNom`
  (facultatif), `prenom` (obligatoire), dans cet ordre.
- Pattern de recherche/pagination à réutiliser pour toute future liste paginée : JPA
  `Specification` en `@Component` + `JpaSpecificationExecutor` + enum whitelist pour le tri +
  DTO de pagination avec factory `from(Page<T>)`.

**Plan comptable (lecture) — `/api/plan-comptable`, `/api/classes-comptables`**
- Endpoints en `GET` + query params (pas le pattern POST/rechercher) — contrat imposé par le
  handoff frontend de cet écran spécifiquement, pas une règle générale du projet.
- `PlanComptable` n'a pas de FK directe vers `Entite` (version datée d'un
  `ReferentielComptable`, partagée). Scope tenant sur `CompteComptable` :
  `entite IS NULL OR entite.id = :entiteId` (jamais une simple égalité).
- Recherche insensible aux accents : extension Postgres `unaccent` + normalisation Java
  (`Normalizer.NFD`), pattern `RechercheTexteUtils` à réutiliser.
- Tri fixe par `numero` ascendant, pas de `sortBy` exposé (le handoff ne le prévoit pas).
- **Recherche étendue au-delà de la classe** (2026-09-08, retour user : chercher "411" dans le rail
  de classes ne trouvait rien puisque ce filtre ne matchait que `numero`/`titre` de la classe
  elle-même ; chercher un libellé de compte dans le panneau ne portait que sur la classe
  sélectionnée). Deux extensions indépendantes, sans changer le contrat GET+query params existant :
  - `GET /api/classes-comptables?q=` matche désormais aussi si un compte de la classe (visible pour
    le plan actif/l'entité appelante) correspond par numéro ou libellé, via une sous-requête
    `EXISTS` (`ClasseCompteComptableSpecification.parRecherche`, qui prend maintenant
    `planComptableId`/`entiteId` en plus du `referentielComptableId`) — chercher "411" fait
    remonter la classe 4 dans le rail, pas seulement chercher "4".
  - Nouvel endpoint `GET /api/comptes/rechercher?q=&sens=&page=&size=` (dans
    `CompteComptableController`, pas de nouveau contrôleur) : recherche à travers **toutes** les
    classes du plan actif de l'entité appelante, mêmes frontières de sécurité (plan actif + entité)
    que `rechercherComptes` mais sans la contrainte de classe. Réutilise
    `CompteComptableSpecification.build(classeId, ...)` avec `classeId = null` → `parClasse`
    retourne `null` (pas de prédicat) au lieu de lever une NPE ; aucun nouveau DTO, `classeId` reste
    une frontière de sécurité quand fourni (recherche scopée), n'en est simplement plus une pour la
    recherche globale.
  - **Trois bugs corrigés après retours utilisateur en test manuel** (le contexte Spring des tests
    ne les couvrait pas : les tests Mockito mockent les `Specification`, ils ne les exécutent jamais
    réellement contre Postgres) :
    1. `Specification.where(null)` lève `IllegalArgumentException` sur cette version de Spring Data.
       `CompteComptableSpecification.build` ouvrait la chaîne avec `parClasse(classeId)`, devenu
       potentiellement `null` en recherche globale → 500 sur `/api/comptes/rechercher`. Corrigé en
       ouvrant toujours la chaîne avec `parPlanComptable` (jamais null) et en mettant
       `.and(parClasse(classeId))` ensuite.
    2. Ce premier correctif était incomplet : **ni `.where(...)` ni `.and(...)` ne tolèrent qu'on leur
       passe `null` en tant qu'objet `Specification`** (contrairement au `Predicate` qu'une
       `Specification` peut renvoyer une fois *invoquée*, qui lui peut être `null` - c'est ce que font
       correctement `parRecherche`/`parSens` depuis le début). `parClasse` avait été écrit avec un
       `if (classeId == null) return null;` **au niveau de la méthode** (renvoie l'objet Specification
       lui-même comme `null`) au lieu de renvoyer toujours une Specification dont le *Predicate*
       interne vaut `null` quand `classeId` est `null` - d'où `IllegalArgumentException: Other
       specification must not be null` sur `.and(parClasse(classeId))` dès qu'un `q` était saisi côté
       recherche globale (le premier correctif n'avait été exercé par aucun test avant validation
       manuelle). Corrigé en alignant `parClasse` sur le même pattern que `parRecherche`/`parSens` :
       toujours retourner une Specification (lambda) non-null, ne renvoyer `null` qu'*à l'intérieur*.
       Symptôme côté navigateur trompeur : un **401** au lieu d'un 500 (l'exception non interceptée
       remonte jusqu'à Spring Security avant le forward `/error`, qui n'est pas exempté
       d'authentification dans `SecurityConfig` - probable, pas creusé plus avant puisque le vrai bug
       est ce 500 amont, pas ce mapping de statut).
    3. Le matching du numéro de compte dans la sous-requête `EXISTS` de
       `ClasseCompteComptableSpecification` était en "contient" (`LIKE '%4%'`) au lieu de "préfixe" :
       chercher "4" remontait n'importe quelle classe contenant un compte dont le numéro contient le
       chiffre 4 n'importe où (704, 645...), pas seulement la classe 4. Corrigé en `LIKE '4%'`
       (préfixe) - cohérent avec la codification décimale SYSCOHADA où seul le préfixe identifie
       réellement la classe/sous-hiérarchie. Le matching par libellé (texte, `unaccent`) reste en
       "contient", volontairement.
  - Front (`plan-comptable-liste`) : le champ de recherche du panneau comptes bascule automatiquement
    en recherche globale dès qu'il n'est pas vide (`rechercheGlobaleActive`), sinon reste scopé à la
    classe sélectionnée du rail (comportement par défaut inchangé). En mode global, une colonne
    "Classe" apparaît dans le tableau ; la classe d'un compte affiché est déduite côté client du
    premier chiffre de son `numero` (convention de codification décimale SYSCOHADA — la classe est
    toujours ce premier chiffre), comparé à un snapshot **non filtré** des classes
    (`toutesLesClasses`, mis à jour seulement quand la recherche du rail est vide) plutôt qu'à
    `classes()` qui peut être un sous-ensemble filtré par une recherche de rail simultanée. Pas de
    nouveau DTO ajouté à `CompteComptableReadDto` pour porter l'info classe : dérivation client
    volontaire pour rester sur le DTO unique déjà réutilisé partout.

**Création de comptes comptables custom — `/api/comptes`**
- Rôles autorisés : `ADMIN` ou `ADMIN_FINANCIER` (remplace `COMPTABLE`, qui reste autorisé
  ailleurs — voir Tiers ci-dessous). Portée du rôle `ADMIN_FINANCIER` **volontairement
  limitée à cet endpoint**, ne pas l'étendre sans redemander.
- Unicité `(plan_comptable_id, numero)` déjà garantie par contrainte DB existante (V5) —
  scopée au plan (partagée entre entreprises du même référentiel), pas par entreprise.
- **Sous-compte : le sens débit/crédit est forcé côté serveur à celui du compte parent**
  quand un `parentId` est fourni (ignore la valeur envoyée par le client) — vérité serveur,
  pas seulement un verrouillage front. Sans parent, le sens reste libre.

**Module Tiers — `/api/tiers`**
- Rôles autorisés pour créer/modifier/désactiver/associer un compte : `ADMIN` ou
  `COMPTABLE`. Lecture (liste/recherche/détail/recap) : tout utilisateur authentifié.
- Association/dissociation de compte = endpoint dédié `PATCH /{id}/compte-associe`, séparé
  du PATCH général — évite l'ambiguïté Jackson "champ absent" vs "champ null" sur un record
  (pas de dépendance `jackson-databind-nullable` ajoutée pour ça).
- `GET /api/tiers/recap` (KPI agrégés) suit le pattern `PlanComptableRecapReadDto` existant.
- `DELETE /{id}` fait toujours une désactivation logique (`actif=false`) : aucune entité
  Écriture comptable n'existe encore dans le codebase, donc la suppression conditionnelle
  décrite dans un prompt antérieur est sans objet pour l'instant — **à revisiter quand le
  moteur comptable (Phase 1) existera.**

**Phase 1 — Moteur comptable (`/api/exercices`, `/api/ecritures`, `/api/comptabilite`)**
- Scope : journal général uniquement (entité `Journal`, singleton par entreprise, provisionné paresseusement au premier exercice/écriture — pas de CRUD de journaux). Pas de réouverture d'exercice clôturé. Package racine dédié `moteurComptable` (sibling de `integrationClient`), pas nesté dedans — tranche verticale complète.
- Rôles : `ADMIN`/`ADMIN_FINANCIER`/`COMPTABLE` saisissent (brouillon), seuls `ADMIN`/`ADMIN_FINANCIER` valident/renvoient en brouillon/contre-passent, `ADMIN` seul crée/clôture un exercice. Auto-validation autorisée (pas de vérification créateur≠validateur).
- **Numéro séquentiel de l'écriture assigné uniquement à la validation** (`Ecriture.numero` nullable), jamais à la création du brouillon — sinon un brouillon abandonné laisserait un trou dans la numérotation légale du journal. Mécanisme : compteur `Journal.dernierNumero` + verrou de ligne (`@Lock(PESSIMISTIC_WRITE)`, `JournalRepository.findByIdForUpdate`) dans la transaction de `valider()`, contrainte unique `(journal_id, numero)` en filet de sécurité (les écritures non numérotées ont `numero = NULL`, Postgres autorise les NULL multiples dans une contrainte unique).
- **Contre-passation** : crée une écriture miroir en BROUILLON (lignes dupliquées, sens inversé), rattachée à l'exercice OUVERT courant (jamais à celui de l'originale, qui peut être clôturé). Le miroir repasse par la file de validation normale. **L'originale ne bascule en `CONTREPASSEE` que lorsque le miroir est validé** (pas à la demande de contre-passation) — sinon grand livre/balance seraient temporairement incohérents. Sans rapport avec `CompteComptable.lettrable` (lettrage = rapprochement facture/règlement, sujet Phase 2).
  - **Date du miroir laissée à la main de l'utilisateur** (2026-09-08, corrige un bug signalé par le user : `LocalDate.now()` figé ne tombait pas forcément dans les bornes de l'exercice OUVERT courant — ex. exercice 2027 déjà ouvert alors qu'on contre-passe "aujourd'hui" en 2026, la soumission du miroir échouait ensuite sur `validerDateDansExercice`). `POST /{id}/contre-passer` accepte désormais un corps `ContrePassationForm(LocalDate date)` optionnel (pattern `RenvoyerBrouillonForm` : `@RequestBody(required = false)`, défaut `new ContrePassationForm(null)`) ; `EcritureServiceImpl.contrePasser()` retombe sur `original.getDate()` si `date` est `null`, puis appelle `validerDateDansExercice(dateMiroir, exerciceCible)` **avant** de construire le miroir — même garde-fou qu'à la création/modification d'écriture, pas seulement le filet de sécurité existant au `soumettre()`. Front (`ecriture-detail`) : modale de confirmation de contre-passation pré-remplit un `<app-date-picker>` avec la date de l'écriture d'origine (bornes `[min]`/`[max]` = exercice OUVERT courant, chargé via `ExerciceService.obtenirOuvert()` en constructeur, erreur silencieuse si aucun exercice ouvert — n'empêche pas la lecture du détail).
- **Clôture d'exercice revérifiée côté serveur** dans `POST /cloturer` (jamais fait confiance à un appel préalable à `GET /cloture-check`) : bloquée tant que des écritures `BROUILLON`/`EN_ATTENTE` existent sur l'exercice.
- Un seul exercice `OUVERT` par entité imposé par un **index unique partiel** (`WHERE statut = 'OUVERT'`), pas seulement une vérification applicative.
- **Solde d'ouverture du grand livre (à-nouveaux)** : calculé à la volée par cumul de toutes les lignes validées antérieures à la date de début de l'exercice, **uniquement pour les comptes de bilan (classes 1 à 5)** — un compte de gestion (classes 6/7/8) reste à zéro à chaque exercice (sinon le résultat calculé sur l'exercice courant serait faussé par un cumul historique). Aucune écriture d'à-nouveaux n'est physiquement générée pour les comptes de bilan classiques (411, 521...) ; leur report reste dérivé (`calculerSoldeAvant`/`calculerSoldesOuvertureBilan`), cohérent avec le principe déjà appliqué au grand livre/balance du plan comptable.
- Résultat d'exercice : calculé à la volée (Σ crédit-débit des classes 6/7/8, écritures validées non-clôture — cf. `genereParCloture` ci-dessous), jamais stocké sur `ExerciceComptable`.
- **Écriture de clôture générée automatiquement** (2026-09-07, corrige un manque du critère d'acceptation §1.6/5-6 découvert en audit : sans ça, le résultat d'un exercice clos ne "landait" jamais sur le bilan, et la balance de l'exercice suivant ne pouvait jamais s'équilibrer dès qu'un exercice précédent avait un résultat non nul). `ExerciceComptableServiceImpl.cloturerExercice()`, avant de passer l'exercice à `CLOS` :
  - si `calculerSoldesGestion(exerciceId)` (nouvelle requête `LigneEcritureRepository`, solde net par compte 6/7/8 mouvementé sur l'exercice) n'est pas vide ET que le résultat n'est pas exactement nul, génère une **écriture système** : une ligne de sens opposé par compte de gestion mouvementé (le ramène à zéro), plus une ligne sur le compte de résultat SYSCOHADA — **111** "Résultat net : bénéfice" si résultat > 0, **119** "Résultat net : perte" si < 0 (numéros seedés en V15 pour SYSCOHADA_NORMAL uniquement — **à généraliser le jour où SYCEBNL/SMT ont leur propre plan seedé**, cf. section création d'entreprise).
  - Cette écriture est directement `VALIDEE` (numérotée via le même verrou `Journal.dernierNumero`/`findByIdForUpdate` que `EcritureServiceImpl.valider()`), datée `dateFin` de l'exercice, `creePar`/`validePar` = l'ADMIN qui clôture.
  - Marquée `Ecriture.genereParCloture = true` (colonne V22, `@Builder.Default` — **ne pas oublier l'annotation**, cf. piège Lombok déjà documenté plus bas) et **exclue de `calculerResultat`** : sans cette exclusion, l'écriture s'annulerait elle-même dans le résultat de l'exercice qu'elle vient de clôturer (elle solde exactement les comptes que `calculerResultat` somme). Elle reste en revanche incluse dans le grand livre/la balance de l'exercice clos (comportement voulu : on voit la ligne de clôture solder le compte) et, via le compte 111/119 (classe 1), correctement reprise comme solde d'ouverture de l'exercice suivant par `calculerSoldeAvant`/`calculerSoldesOuvertureBilan` — c'est ce qui fait enfin coïncider bilan et résultat cumulé d'un exercice sur l'autre.
  - Aucune écriture générée si le résultat de l'exercice est exactement nul (rien à reporter) — un résidu non soldé individuellement sur un compte 6/7/8 dans ce cas précis n'a aucun impact : ces comptes sont de toute façon exclus par construction du solde d'ouverture des exercices suivants.
  - Risque connu, pas traité : rien n'empêche aujourd'hui de contre-passer une écriture de clôture comme une écriture normale — pas bloquant pour la fiabilité des chiffres, mais à revisiter si ça devient un problème pratique.
- `creePar`/`validePar` sur `Ecriture` sont de vraies relations `@ManyToOne Utilisateur` (pas un id brut ni un champ auditing Spring Data — cette infra n'existe pas dans ce projet), posées manuellement dans le service.
- `LigneEcriture.sens` réutilise l'enum `SensCompte` déjà existant (package `referentiel`) plutôt que d'en dupliquer un nouveau pour le module.
- `RechercheTexteUtils` (`integrationClient.specification`) est passé en visibilité `public` pour être réutilisé par `EcritureSpecification` dans le nouveau package.
- **Compte désactivé bloqué dans une écriture** (2026-09-07) : `EcritureServiceImpl.compteValide()` lève désormais un `ConflitException` (409, pas 404 — le compte existe et est visible, contrairement au cas d'un compte d'une autre entreprise) si `compte.isActif() == false`. S'applique à la création ET à la modification d'une écriture (les deux passent par `remplirLignes`/`compteValide`).
- **Piège résolu : `CompteComptable.actif` sans `@Builder.Default`.** Le champ `private boolean actif = true;` avait un initialiseur Java mais pas l'annotation Lombok — le builder l'ignorait silencieusement et produisait `actif = false` dès qu'un appelant omettait `.actif(...)`. Sans impact en prod (`CompteComptableCreationServiceImpl` et le seed SQL fixent toujours `actif` explicitement), mais aurait fait échouer tout nouveau test construisant un `CompteComptable` sans `.actif(true)` une fois le contrôle ci-dessus ajouté. Corrigé en ajoutant `@Builder.Default` sur le champ plutôt qu'en contournant dans les tests — **réflexe à appliquer à tout champ avec initialiseur inline dans une entité `@Builder`** (vérifier `lettrable` et les futurs champs similaires si un contrôle vient un jour en dépendre).

**Phase 1 — Front (`code_source_front`, écrans C à K du handoff)**
- Nouveau bloc de pages `pages/comptabilite/` (tableau de bord, exercices, saisie/liste/détail écritures, file de validation, grand livre, balance), routes plates top-level (`/comptabilite`, `/comptabilite/ecritures`, etc.), onglet "Comptabilité" ajouté dans `home.html` à côté de Plan comptable/Tiers.
- 3 services (`exercice.service.ts`, `ecriture.service.ts`, `comptabilite.service.ts`) suivant exactement le pattern `tiers.service.ts`. Lignes d'écriture = `signal<LigneRow[]>([])` muté via `.update()`, **pas** `FormArray`/`ReactiveFormsModule` (aucun `FormArray` n'existe ailleurs dans ce front — toutes les pages comptables sont signal-based).
- `validation.guard.ts` (nouveau, miroir de `admin.guard.ts`) : seule route protégée par guard dans tout le module (`/comptabilite/validation`, ADMIN/ADMIN_FINANCIER) — toutes les autres routes comptables suivent le pattern existant "pas de guard, bouton conditionné par rôle en composant".
- `GET /api/ecritures/stats` **ajouté côté back après coup** (absent de l'implémentation initiale de la Phase 1, nécessaire pour les KPI du tableau de bord) — compte brouillon/en attente/validées pour un exercice donné, réutilise les mêmes `countByExercice_IdAndStatut` que `verifierCloture`.
- `EcritureStatutBadge` (nouveau composant partagé) compose `app-status-badge` existant plutôt que de redéfinir des tons — un seul point de mapping libellé/tonalité pour les 4 statuts.
- `.bandeau-equilibre` et `.selecteur-exercice` extraits vers `styles/components.scss` (2e usage atteint pendant cette même implémentation — saisie d'écriture + détail + balance ; grand livre + balance), suivant le pattern déjà noté "déplacer au 2e usage".
- `Entite.devise` : champ déjà présent en saisie libre (`create-entreprise.html`, `maxlength="3"`) et déjà affiché sur l'écran Compte — laissé tel quel (pas de `<select>` fermé CDF/USD/EUR), `decimalesDevise()` (`core/devise.util.ts`) retombe sur 2 décimales pour toute valeur non reconnue.
- Solde d'ouverture du grand livre affiché en 1ʳᵉ ligne de tableau, calculé par le back (cf. section moteur comptable ci-dessus) — le front ne fait qu'afficher `soldeOuverture`/`soldeProgressif`, aucun calcul de cumul côté client.

**Phase 1 — Front, refonte visuelle pour coller aux maquettes (2026-09-07)**
- Livraison initiale des écrans C à K jugée trop éloignée des maquettes `.dc.html`
  (`projet_comptable/maquette/`) — écart de **composition**, pas de palette (les tokens
  `Comptano Design System` étaient déjà proches). Refonte écran par écran pour coller aux
  maquettes, cf. patterns transverses ajoutés à `components.scss` ci-dessous.
- Décision actée : `ecriture-detail` (pas de maquette dédiée — la maquette le montre en modale
  sur l'écran Liste des écritures) **reste une page dédiée** `/comptabilite/ecritures/:id`,
  simplement restylée. Ne pas re-proposer la conversion en modale sans motif nouveau — elle
  perdrait l'URL directe et faudrait réintégrer les actions Supprimer/Renvoyer en brouillon que
  la maquette ne montre pas.
- `formaterMontant()`/`decimalesDevise()` (`core/devise.util.ts`), qui existaient mais n'étaient
  utilisés nulle part, sont maintenant utilisés dans tout le module comptabilité. Devise
  récupérée via `EntiteService.obtenirMonEntite()` **dans chaque composant** (pas de cache
  partagé — aucun pattern de cache n'existe ailleurs dans ce front, un appel par écran reste
  cohérent avec le reste du codebase).
- Nouvelles classes globales dans `components.scss` (2e usage atteint dès cette refonte,
  suivant le pattern "déplacer au 2e usage") : `.fil-ariane` (remplace `.retour` sur les 7
  écrans comptabilité uniquement — les autres modules gardent `.retour`), `.table-dense
  tr.ligne-synthese` (ligne total/solde d'ouverture), `.compte-rail-item` (ligne compte — grand
  livre et panneau de droite de la modale "Choisir un compte" de la saisie), `.toast` (déplacé
  depuis `file-validation.scss`, coin bas-droit, auto-dismiss, sans bouton de fermeture
  manuelle).
- **Grand livre** : le rail est un accordion à un seul niveau ouvert à la fois — cliquer une
  classe (réutilise `.classe-item` tel quel, avec un chevron `.classe-chevron` en plus) déplie
  ses comptes juste en dessous (`GrandLivre.classeOuverteId`, un seul id à la fois) ; en
  cliquer une autre referme la précédente. Pendant une recherche (`query` non vide), ce repli
  manuel est ignoré : toutes les classes ayant un compte correspondant se déplient
  automatiquement, celles sans résultat sont masquées (cf. `GrandLivre.groupes()` computed) —
  décision explicite du user (2026-09-07), à reproduire si un pattern similaire (rail
  classe→comptes) apparaît ailleurs.
- **Saisie d'écriture** : les deux `<select>` classe/compte empilés remplacés par un bouton
  "Choisir un compte…" ouvrant une modale (rail de classes à gauche, réutilise
  `.classes-rail`/`.classe-item` tels quels ; panneau de comptes à droite en
  `.compte-rail-item`). La logique de chargement des comptes par classe reste dans le composant
  (`choisirClasse`/`choisirCompte`, désormais privées, déclenchées par la modale). Bandeau
  d'équilibre + les 2 boutons d'action regroupés dans une barre `position: fixed; bottom:0`
  (`.bandeau-bas-saisie`), au lieu d'être en flux normal — la carte de formulaire a une
  `margin-bottom` pour ne pas passer dessous.
- **Grand livre** : le rail ne liste plus seulement des classes (compte choisi ensuite via
  `<select>`) — il liste directement tous les comptes groupés par classe, chargés en une fois
  (`forkJoin` sur `rechercherComptes` par classe) et filtrés côté client par numéro/libellé.
  Accepte un `?exerciceId=` en query param (lien "Consulter →" depuis un exercice clos dans
  Gestion des exercices).
- **Exercices/Balance** : colonne "Résultat" par exercice (`obtenirResultat(id)`, déjà
  générique, appelé en `forkJoin` pour tous les exercices y compris clos). Balance : une seule
  colonne "Solde" (valeur absolue, couleur conditionnelle débit/crédit) au lieu de deux colonnes
  Solde débiteur/créditeur séparées ; équilibre calculé sur Σ `soldeDebiteur`/`soldeCrediteur`
  des lignes (pas Σ `totalDebit`/`totalCredit`), pour matcher exactement le libellé maquette
  "Σ soldes débiteurs = Σ soldes créditeurs".
- **`<app-date-picker>` maison** (`shared/ui/date-picker/`, 2026-09-07) remplace les 3
  `<input type="date">` du module (saisie d'écriture, création d'exercice ×2) — rendu natif
  trop variable d'un navigateur/OS à l'autre, et pas de moyen rapide de sauter à une année
  lointaine. Aucune dépendance ajoutée (pas de date-fns/Material/etc., cohérent avec ce front
  minimaliste) : logique pure dans `core/date.util.ts` (conversions ISO ⟷ jj/mm/aaaa), panneau
  à 3 niveaux (jours → mois → années, on clique l'en-tête pour remonter d'un niveau) pour
  naviguer vite sur une date éloignée. Saisie manuelle jj/mm/aaaa toujours possible en
  parallèle (commit au blur/Enter, revert si invalide). `[(value)]` se lie directement à un
  `signal<string>` du parent (pas besoin que ce soit un `model()`, Angular le permet depuis la
  17.2) ; `[min]`/`[max]` (ISO, optionnels) désactivent les jours hors bornes dans la grille —
  utilisé pour la date d'écriture (bornée à l'exercice ouvert), pas pour la création d'exercice
  (pas de borne naturelle).
- **`<app-selecteur-exercice>` maison** (`shared/ui/selecteur-exercice/`, 2026-09-07, handoff §I
  grand livre + §J balance) remplace le `<select>` natif du sélecteur d'exercice sur ces deux
  écrans — bouton (période + badge statut + chevron, `aria-haspopup="listbox"`,
  `aria-expanded`) + panneau `role="listbox"` ancré à droite, une option par exercice
  (`role="option"`, `aria-selected`, coche cuivre sur la ligne active), fermeture par calque
  transparent plein écran au clic en dehors (pas `.modale-fond` assombri — ce n'est pas une
  modale bloquante) ou Échap. `[value]`/`(valueChange)` classiques (pas un `model()`) : le
  parent garde son `changerExercice(id)` existant tel quel, juste rebranché sur l'event au lieu
  du `(change)` du `<select>`. Ancien `.selecteur-exercice` (composé autour d'un `<select>`)
  supprimé de `components.scss`, plus aucun usage.
- Le handoff §J mentionne un endpoint `GET /api/rapports/balance?exerciceId=` qui n'existe pas
  tel quel — l'endpoint réellement implémenté et déjà branché est
  `GET /api/comptabilite/balance?exerciceId=` (`GrandLivreBalanceController`). Pas renommé :
  aucun bénéfice fonctionnel à faire coïncider le chemin avec un handoff généraliste, et ça
  casserait le contrat déjà testé. Si un doc futur re-mentionne `/api/rapports/...`, c'est ce
  même écart, pas un nouvel oubli.
- **Confirmation avant Valider/Soumettre** (2026-09-07, retour utilisateur après comparaison
  avec les maquettes) : les 3 actions qui faisaient un appel API direct au clic
  (`file-validation` "Valider", `ecriture-detail` "Valider", `ecriture-saisie` "Soumettre à
  validation") ouvrent maintenant une modale de confirmation (`.modale-fond`/`.modale`
  standard) avant d'agir — récap dans un `.card` (Libellé/Référence/Montant/Soumise par via
  `.entreprise-ligne`/`.entreprise-valeur`), `<app-alert type="warning">` pour la conséquence
  ("verrouillée et intégrée au grand livre" / "ne peut plus être modifiée..."), bouton final
  `.btn-succes` (nouveau, vert, distinct de `.btn-primary` pour ne pas confondre une validation
  avec une action de saisie ordinaire).
- **Contre-passation** (`ecriture-detail`) a rejoint le même pattern de confirmation
  (2026-09-07, "ça passe presque inaperçu" — retour user) : bouton final reste `.btn-primary`
  (pas `.btn-succes`, ce n'est pas une action "positive" comme valider, plutôt une correction/
  annulation) ; le texte d'avertissement explique le mécanisme réel (miroir en brouillon
  rattaché à l'exercice ouvert courant, l'originale ne bascule en CONTREPASSEE qu'à la
  validation du miroir — cf. section moteur comptable) plutôt qu'une formule générique.
- `.entreprise-ligne`/`.entreprise-valeur`/`.entreprise-valeur-id` remontés de `home.scss` vers
  `components.scss` (3e usage : accueil + détail écriture + ces 3 modales) — **l'historique
  d'écriture (`ecriture-detail`) n'avait en réalité jamais été stylé** depuis la refonte
  maquettes : les styles Angular sont encapsulés par composant, donc réutiliser une classe
  définie seulement dans le `.scss` d'un autre composant ne fait rien. Vérifier ce genre de
  faux-positif (classe qui "a l'air" globale mais ne l'est pas) avant de la réutiliser ailleurs.
- `.fil-ariane a` était gris terne par défaut (accent seulement au survol) — la maquette a tous
  ses liens en cuivre par défaut (`a{color:#b5652e}` global). Corrigé (`color: var(--accent)`).
- `.bandeau-equilibre--erreur` (nouveau, rouge `--error-soft`) pour un écart réel (balance) —
  distinct du `.bandeau-equilibre` par défaut (ambre, "pas encore équilibré" en cours de
  saisie) : ne pas fusionner les deux sémantiques, l'ambre reste pour la saisie en cours.

**CI/CD (`.gitlab-ci.yml`) — 2026-09-08**
- Détail complet dans `doc/ci.md`. Résumé des décisions structurantes : `.pre → build → test →
  package → security`, pas de stage `deploy` (pas d'infra cible pour l'instant). Deux
  déclencheurs (`workflow:rules`) : MR (n'importe quelle cible) et tag `vX.Y.Z`.
  **Principe directeur, affiné deux fois après retour du user : ne jamais revalider ce qui a
  déjà été validé.** `build`/`lint`/`test`/`trivy-fs`/`semgrep`/`package`/`security`
  (`.validate_rules`) ne tournent **que** sur une MR `* → develop` (première validation de ce
  code) ou sur un tag (dernier filet, rejoué depuis zéro). Une MR `develop → test` ou
  `test → main` ne déclenche **que** `branch-flow-guard` — le contenu a déjà été validé,
  commit par commit, par les pipelines qui l'ont fait atterrir sur `develop` ; ça tient parce
  que `develop` est protégée (pas de push direct, merges séquentiels gatés par MR), donc son
  `HEAD` est toujours un état déjà testé. `package`/`security` ont en plus une différence MR
  vs tag indépendante de la cible : sur une MR `* → develop`, l'image est construite et
  scannée **localement** (`docker save` en tarball, jamais de push, jamais de credential
  registry utilisé) et jamais signée ; sur un tag, en plus, l'image est réellement poussée (3
  tags : `$CI_COMMIT_SHORT_SHA`, `$CI_COMMIT_TAG`, `buildcache`) et signée (cosign).
  **Aucune pipeline ne se déclenche sur un push nu `develop`/`test`/`main`**. Ni `main` ni les
  tags ne sont protégés par défaut dans ce fichier YAML (à faire côté Settings > Protected
  branches/tags si un jour ça doit être restreint). **CD future** : `develop → test`
  (recette) et le tag (prod) sont les deux points d'ancrage identifiés pour le déploiement
  futur — mécanisme de réutilisation d'artefact inter-pipelines pour la recette pas encore
  conçu (rien à déployer pour l'instant).
- **CVE CRITICAL/HIGH trouvées en testant le scan Trivy — corrigées (2026-09-08)** :
  `org.apache.tomcat.embed:tomcat-embed-core` 11.0.22 (Tomcat embarqué par
  `spring-boot-starter-webmvc`, Spring Boot 4.0.7) — 3 CVE CRITICAL (CVE-2026-65182,
  CVE-2026-65905, CVE-2026-68525) ; `org.postgresql:postgresql` 42.7.11 — 1 CVE HIGH
  (CVE-2026-54291). **Corrigé par deux overrides de propriété dans `pom.xml`**
  (`tomcat.version=11.0.25`, `postgresql.version=42.7.12` — propriétés confirmées dans le
  `.pom` du parent `spring-boot-dependencies:4.0.7` en cache local avant de les fixer, pas
  devinées). Jar revérifié à 0 CVE CRITICAL/HIGH après reconstruction. Reste 3 CVE HIGH non
  bloquantes sur l'OS de base `eclipse-temurin:21-jre-alpine` (OpenSSL), hors de portée d'un
  override Maven — dépend de la prochaine image de base publiée par Adoptium, revérifié de
  toute façon à chaque pipeline par `trivy-fs`/`trivy-image-api`.
- **Garde-fou de flux de branches** (`branch-flow-guard`, stage `.pre`, MR uniquement) :
  applique `* → develop` (source libre), `develop → test`, `test → main` — bloque toute autre
  paire ciblant ces 3 branches (`feature/* → test`, `feature/* → main`, `develop → main`). Ne
  couvre que les MR ciblant develop/test/main ; une MR vers une autre branche n'est pas
  concernée. **Complète, ne remplace pas**, la protection de branche GitLab (push direct,
  approbations sur `main`) — à configurer séparément côté Settings, hors de ce fichier YAML.
- **Stage `build`** (`mvn package -DskipTests`) : fail-fast (une erreur de compilation
  n'aurait sinon été détectée que par le job `test`, après Postgres + 94 tests — ni
  `lint`/`semgrep`/`trivy-fs` ne compilent) et produit le jar réutilisé tel quel par
  `package` (`build-api-image` a `needs: {job: build, artifacts: true}`) — le `Dockerfile` ne
  recompile plus, juste `COPY target/*.jar app.jar`. Conséquence : `docker build .` seul ne
  suffit plus hors CI, il faut d'abord `mvn package -DskipTests`. Volontairement `package
  -DskipTests`, pas `install`/`clean install` : `install` réexécuterait `test`+`verify` (donc
  les 94 tests + un doublon exact de `checkstyle-check`/`jacoco-check` déjà couverts par
  `lint`/`test`) et installerait le jar dans `~/.m2/repository` sans aucun consommateur (pas
  de projet multi-module). Seul le jar final transite entre jobs (pas les `.class`
  intermédiaires vers `lint`/`test`/`trivy-fs`/`semgrep`, qui recompilent chacun de leur côté
  si besoin) : partager des `.class` compilés entre jobs Maven est fragile (le checkout Git de
  chaque nouveau job réinitialise les timestamps, cassant souvent la détection
  d'incrémentalité) — un jar déjà finalisé n'a pas ce problème, c'est un simple binaire copié
  tel quel. La ressource coûteuse à cacher pour Maven reste `.m2/repository` (téléchargement
  des dépendances, pas la compilation elle-même) — un cache `.m2` keyed sur `pom.xml` couvre
  déjà ce besoin pour tous les jobs Maven, `build` inclus.
- **JaCoCo** : gate bloquant (`mvn verify`, phase `verify`) scopé à `**/service/impl/**`
  uniquement (seule couche avec une vraie discipline de test unitaire, cf. Phase 1) — étendre
  au reste (DTO/entités/mappers/controllers) ferait échouer le gate sur du code jamais visé par
  une stratégie de test. Seuils au niveau **BUNDLE** (agrégé), pas per-class : plusieurs
  service/impl (`KeycloakAdminServiceImpl`, `MailServiceImpl`...) sont à 0% individuellement,
  un gate per-class échouerait immédiatement sans rapport avec ce que ce gate cherche à
  garantir. Seuils actuels 60% lignes / 65% branches (mesuré ~65%/~74% réel au 2026-09-08) — à
  resserrer progressivement, et à repasser en per-class une fois les classes à 0% couvertes.
- **Checkstyle** : ruleset volontairement restreint (`checkstyle.xml`, racine) — pas de
  Sun/Google checks complet. `AvoidStarImport` explicitement exclu : `import
  jakarta.persistence.*;`/`import lombok.*;` est une convention établie sur toutes les entités
  JPA de ce projet, pas un bug ; l'imposer aurait fait échouer 26 violations sur 13+ fichiers
  sans rapport avec la CI. Le seul vrai import mort trouvé à cette occasion
  (`PlanComptable.java`) a été corrigé.
- **Piège découvert et corrigé : découverte OIDC eager au boot du contexte Spring en test.**
  `ApiComptaApplicationTests`/`AuthControllerTest` (`@SpringBootTest`, contrairement aux 8
  autres classes de test qui sont de purs `@ExtendWith(MockitoExtension.class)`) déclenchent
  l'autoconfiguration OAuth2 resource server de Spring Boot, qui construit le `JwtDecoder` via
  `JwtDecoders.fromIssuerLocation(issuer-uri)` — **appel réseau réel vers Keycloak au moment de
  la création du bean**, pas à la première décodification. Invisible en local (Keycloak déjà up
  via `docker compose`), aurait fait échouer ces 2 tests sur tout runner CI sans Keycloak.
  Corrigé par un nouveau profil Spring **`test`** (`src/test/resources/application-test.yaml`,
  activé automatiquement via `maven-surefire-plugin` →
  `systemPropertyVariables.spring.profiles.active=test`) qui remplace `issuer-uri` par un
  `jwk-set-uri` factice — `NimbusJwtDecoder.withJwkSetUri()` ne fait aucun appel réseau à la
  construction du bean, seulement à la première décodification réelle (qu'aucun test
  n'exerce aujourd'hui). Zéro impact prod (fichier de test uniquement). Le job CI `test` n'a
  donc besoin que d'un service `postgres:16.4-alpine`, pas de Keycloak.

## Pièges déjà résolus (ne pas reproduire)

- `./mvnw spring-boot:run` lancé hors docker-compose auto-managé **ne charge pas `.env`**
  (`spring-boot-docker-compose` ne fait pas de pass-through générique des variables d'env).
  Résultat : les secrets retombent sur leurs valeurs `:default` (`changeme`), échec silencieux
  jusqu'à l'appel réel (401 côté Keycloak). Exporter explicitement les variables nécessaires
  avant de lancer (pas un `source .env` brut, ça casse sur les valeurs avec espaces non
  quotées).
- Après modification de `keycloak/realm-export.json`, le conteneur Keycloak doit être
  **recréé** (`docker compose up -d --force-recreate keycloak`), pas juste `restart`.
- Le rôle Keycloak `realm-management: manage-users, view-users` seul ne suffit pas pour
  lister les rôles realm — il faut aussi `view-realm`.
- Cookie CSRF posé par erreur en `Path=/api` casse sa lecture via `document.cookie` (les
  pages Angular ne sont jamais sous `/api`) — doit être `Path=/`. Un cookie résiduel
  `Path=/api` coexiste avec le nouveau, vider les cookies du navigateur après ce genre de
  changement.
- **Front : ne jamais utiliser `<form (ngSubmit)="...">` sans importer `FormsModule`** (ou
  `ReactiveFormsModule`) dans le composant — sans lui, `(ngSubmit)` ne se déclenche jamais
  (aucune directive `NgForm` disponible), et le clic sur le bouton `submit` retombe sur la
  soumission HTML native (rechargement de page). Symptôme observé : une modale qui semble se
  fermer sans qu'aucune requête n'apparaisse dans l'onglet Réseau. Les modales signal-based de
  ce projet (`tiers-creation-modal`, `compte-comptable-creation-modal`, et depuis la Phase 1
  toutes les modales de `pages/comptabilite/`) évitent le problème en n'utilisant **jamais**
  de balise `<form>` : un simple `<div class="formulaire">` + bouton `type="button"` +
  `(click)` suffit et reste cohérent avec le style signal-based (`signal()`/`computed()`)
  utilisé partout ailleurs dans ce front.
- L'intercepteur 401 frontend ne doit rediriger vers `/connexion` que si l'utilisateur était
  déjà authentifié en mémoire juste avant l'appel (sinon il masque des pages publiques lors
  de vérifications routinières "suis-je connecté ?").
- **`mvn test`/IDE en local peut se connecter au mauvais Postgres si un service natif écoute
  déjà sur le port 5432** (ex. une install Windows native PostgreSQL, indépendante de ce
  projet) — Spring/Flyway échoue alors en `authentification par mot de passe échouée pour
  erp_user` (pas une erreur réseau : il y a bien un Postgres qui répond, juste pas le bon, avec
  d'autres identifiants). **Corrigé durablement (2026-09-08)**, reproductible sur toute
  machine sans variable d'environnement à poser manuellement : service dédié `postgres-test`
  dans `compose.yaml`, démarré automatiquement par le `docker compose up` habituel (un profil
  Compose séparé avait été envisagé puis écarté — moins pratique, une commande de plus à
  retenir à chaque run de tests), toujours vide au démarrage (`tmpfs`, pas de volume nommé —
  Flyway repart de zéro à chaque run, comme le service Postgres éphémère de la CI), exposé sur
  le port **5435** (ni 5432 ni 5433/`postgres-metier`, pour ne jamais entrer en conflit).
  `application-test.yaml` pointe directement dessus (`localhost:5435`) — donc `mvn test` marche
  tel quel dès que `docker compose up` tourne, sans surcharge `SPRING_DATASOURCE_URL`
  nécessaire.

## État d'avancement

Voir `doc/deja_implemente/phase-0.md` (endpoints, rôles, détail des modules jusqu'à fin
août 2026 — à mettre à jour pour refléter la Phase 1) et `doc/a_implementer/cahier-des-charges.md`
(Phases 1 à 7). Résumé : auth BFF + création entreprise + gestion utilisateurs + plan
comptable (lecture + création de comptes custom) + module Tiers + **Phase 1 complète et
auditée contre les critères d'acceptation §1.6 du cahier des charges** (back + front) :
exercices, journal général, écritures en partie double, validation, contre-passation, grand
livre, balance, clôture avec écriture de solde de gestion + report du résultat sur le bilan.
Back : 94/94 tests verts (92 Mockito purs + 2 `@SpringBootTest` de boot de contexte), couverts
par CI (`.gitlab-ci.yml`, cf. `doc/ci.md`) : lint Checkstyle, JaCoCo (gate scopé à
`service/impl`), Trivy (Dockerfile + image, pas `pom.xml` — cf. `doc/ci.md`, piège 429 Maven
Central), Semgrep, build/push image Docker sur tag
`vX.Y.Z` uniquement (un merge `main` seul ne publie rien).
Front : tous les écrans C à K du handoff `Comptano - Handoff Phase 1.dc.html` implémentés puis
refaits une 2e fois pour coller précisément aux maquettes (`ng build --configuration
development` sans erreur), non testés dans un navigateur par l'assistant (cf. préférence
utilisateur), pas encore couvert par CI (repo séparé). Seul écart connu et volontaire vs les 7
critères §1.6 : le critère "créer/lister les journaux" n'est pas exposé (aucun endpoint
`/api/journaux`, un seul journal général auto-provisionné) — reporté à la Phase 2 par choix
explicite du user. Phases 2 à 7 (facturation, immobilisations, RH, GED, états financiers,
reporting) pas commencées. CD (déploiement) pas encore en place (pas d'infra cible).
