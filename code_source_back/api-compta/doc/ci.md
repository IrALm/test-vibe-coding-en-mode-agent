# CI — Pipeline GitLab

Documentation du pipeline défini dans [`../.gitlab-ci.yml`](../.gitlab-ci.yml) : ce qui est
fait, dans quel ordre, sur quel déclencheur, et pourquoi. Adapté à ce projet (Spring
Boot/Maven, pas Node) à partir d'un template CI plus général ; les écarts volontaires par
rapport à ce template sont documentés explicitement ci-dessous plutôt que passés sous
silence.

**CD (déploiement) volontairement absente pour le moment** : pas d'infra cible disponible.
Le pipeline s'arrête au stage `security`. À ajouter plus tard, probablement gatée sur le même
déclencheur tag que `package`/`security` — cf. section dédiée en fin de document.

---

## Vue d'ensemble

```
.pre → build → test → package → security
```

| Stage | Rôle |
|---|---|
| `.pre` | Garde-fou de flux de branches (`branch-flow-guard`) |
| `build` | Compile le jar (`mvn package -DskipTests`), une seule fois — artifact réutilisé par `package` |
| `test` | Lint (Checkstyle), tests + couverture (JaCoCo), scan du `Dockerfile` (Trivy), SAST (Semgrep) |
| `package` | Build l'image Docker API à partir du jar produit par `build` |
| `security` | Scan de vulnérabilités de l'image buildée (Trivy), SBOM (Syft), signature keyless (cosign) |

### Principe directeur : ne jamais revalider ce qui a déjà été validé

Sur les 3 transitions du flux de branches (cf. `.pre` plus bas), le comportement diffère
volontairement :

| Transition | `build`/`lint`/`test`/`trivy-fs`/`semgrep`/`package`/`security` | Pourquoi |
|---|---|---|
| `* → develop` (MR) | ✅ tout tourne | Première validation de ce code — rien n'a encore tourné dessus. |
| `develop → test` (MR) | ❌ rien ne tourne (seul `branch-flow-guard`) | Ce contenu a déjà été validé, commit par commit, par chaque pipeline `* → develop` qui l'a fait atterrir sur `develop`. Le revalider ici ne détecterait rien de nouveau. |
| `test → main` (MR) | ❌ rien ne tourne (seul `branch-flow-guard`) | Même raisonnement. |
| Tag `vX.Y.Z` | ✅ tout tourne, **depuis zéro** | Dernier filet avant publication — on ne fait jamais l'impasse ici, même si déjà validé une fois. |

```yaml
.validate_rules: &validate_rules
  - if: '$CI_COMMIT_TAG =~ /^v\d+\.\d+\.\d+$/'
  - if: '$CI_PIPELINE_SOURCE == "merge_request_event" && $CI_MERGE_REQUEST_TARGET_BRANCH_NAME == "develop"'
```

Appliqué à `build`, `lint`, `test`, `trivy-fs`, `semgrep`, `build-api-image`,
`trivy-image-api`, `sbom-api` : ces jobs ne tournent **que** sur un tag, ou sur une MR dont la
cible est précisément `develop`. Une MR `develop → test` ou `test → main` ne déclenche que
`branch-flow-guard` — son rôle se limite alors à faire respecter le flux de branches, pas à
revalider du code déjà vert.

**Ce raisonnement tient parce que les merges sont séquentiels et gatés par MR** (`develop`
protégée, pas de push direct) : le `HEAD` de `develop` est à tout instant un état qui a déjà
été testé (chaque MR est validée contre le `HEAD` réel au moment de sa fusion). Il n'y a donc
pas de "risque d'intégration" caché entre deux fusions successives que `develop → test`
pourrait révéler — contrairement à ce qu'on pourrait craindre en sautant cette revalidation.

**`package`/`security` ont en plus un comportement différent entre MR et tag**, indépendamment
de la cible :

| | MR `* → develop` | Tag `vX.Y.Z` |
|---|---|---|
| `package` construit l'image | ✅ | ✅ |
| `package` pousse l'image vers le registry | ❌ (sauvegardée en tarball uniquement) | ✅ |
| `security` scanne l'image (Trivy) + génère le SBOM | ✅ (depuis le tarball, pas le registry) | ✅ |
| `security` signe l'image (cosign) | ❌ (rien n'est publié, rien à signer) | ✅ |

```yaml
workflow:
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_TAG =~ /^v\d+\.\d+\.\d+$/'
```

**Aucune pipeline ne se déclenche sur un push nu `develop`/`test`/`main`.** `workflow:rules`
ne couvre que deux cas : un événement de MR, ou un tag matchant `vX.Y.Z`. Un merge vers
`develop`/`test`/`main` (mécaniquement un push, une fois la protection de branche configurée
côté GitLab) ne matche ni l'un ni l'autre : aucune pipeline n'est créée.

**Poser un tag est donc le seul geste qui publie réellement quelque chose** (image poussée au
registry, signée). Un merge `test → main`, même approuvé à 2, ne publie — et ne rebuild —
rien tout seul : seul `branch-flow-guard` y tourne. C'est la création du tag, un geste humain
et volontaire, qui relance tout depuis zéro et transforme le résultat en artefact publié.

### CD future : où viendra le déploiement

`develop → test` et `test → main` ne lancent aujourd'hui rien d'autre que le garde-fou — mais
c'est précisément *là* que viendra le déploiement une fois l'infra prête, pas sur `* →
develop`. `test` deviendra l'environnement de recette : le déploiement dessus doit réutiliser
l'image déjà construite et validée par la pipeline `* → develop` (sans rien reconstruire), pas
repartir de zéro. Ce mécanisme de réutilisation inter-pipelines (l'image construite sur une MR
`* → develop` n'est aujourd'hui qu'un tarball éphémère, jamais poussé — cf. tableau ci-dessus)
reste à concevoir quand la CD arrivera : soit en poussant malgré tout une image taguée
`develop`/le SHA dès `* → develop` (à réutiliser telle quelle plus loin), soit via un autre
mécanisme GitLab (pipelines déclenchées, `needs:pipeline:`...). Volontairement pas tranché
maintenant, tant qu'il n'y a rien à déployer.

---

## `.pre` — garde-fou de flux de branches

Le flux imposé par le projet (feature → develop → test → main → prod) :

```
*        → develop   (toute branche source acceptée)
develop  → test       (uniquement depuis develop)
test     → main       (uniquement depuis test)
```

`branch-flow-guard` fait échouer la pipeline de MR si la paire source/cible ne respecte pas
ce flux (`feature/* → test`, `feature/* → main`, `develop → main` bloqués explicitement).
Ne s'applique qu'aux MR ciblant `develop`/`test`/`main` — une MR vers une autre branche (ex.
`feature → feature`) n'est couverte par aucune règle et passe sans vérification, ce flux ne
concernant que les 3 branches protégées du diagramme fourni par le user. Sans objet sur un
pipeline de tag (`rules: - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'` uniquement) :
un tag n'est pas une MR.

**Ce job ne remplace pas la protection de branche GitLab.** Il bloque la pipeline (donc le
statut ✅ nécessaire pour fusionner), mais **rien dans ce fichier n'empêche un push direct**
sur `develop`/`test`/`main` — ça se configure côté GitLab (Settings > Repository > Protected
branches : *Allowed to push* = No one / maintainers uniquement) et doit être fait
séparément, en plus de ce garde-fou. Une MR ciblant `main` doit aussi exiger 2 approbations
(Moïse + Gédéon) — également hors du périmètre de ce fichier, à configurer dans les règles
d'approbation du projet. De même, **rien n'empêche aujourd'hui qui que ce soit de poser un
tag `vX.Y.Z` sur n'importe quel commit** (pas seulement sur `main`) — à restreindre via
Settings > Repository > Protected tags si ça doit rester un geste réservé à certaines
personnes/à des commits `main`.

---

## Mode d'emploi : comment exécuter chaque transition

Ce qui précède explique le *pourquoi*. Concrètement, côté GitLab UI/CLI :

| Transition | Comment | Ce qui se déclenche |
|---|---|---|
| `feature/* → develop` | Créer une MR, n'importe quelle branche source, cible `develop`. | `branch-flow-guard` + toute la suite (`build`/`lint`/`test`/`trivy-fs`/`semgrep`/`package`/`security`), image construite et scannée localement (tarball, jamais poussée). |
| `develop → test` | Créer une MR avec **source = `develop`**, cible `test`. | Seul `branch-flow-guard` (déjà validé en amont, cf. "Principe directeur" plus haut). |
| `test → main` | Créer une MR avec **source = `test`**, cible `main`. | Seul `branch-flow-guard`. |
| Publier une image | Depuis `main` à jour : `git tag vX.Y.Z && git push origin vX.Y.Z`. | Tout depuis zéro, **et** cette fois l'image est réellement poussée sur le registry (3 tags) et signée (cosign). |

**C'est le seul geste qui publie quelque chose** : un merge `test → main`, même approuvé,
ne construit ni ne publie rien tout seul — cf. "Poser un tag est donc le seul geste qui publie
réellement quelque chose" plus haut.

### Piège découvert en testant ce flux (2026-09-08) : pas de pipeline MR sans push après ouverture

Un commit déjà poussé **avant** l'ouverture de la MR ne déclenche pas automatiquement de
pipeline de type "requête de fusion" (`$CI_PIPELINE_SOURCE == merge_request_event`) — GitLab
ne crée pas cette pipeline rétroactivement pour un commit déjà présent sur la branche. Seule
une pipeline de **branche** (source = un simple push, badge "branche" dans la liste des
pipelines) existe alors pour ce commit ; comme `workflow:rules` n'autorise que
`merge_request_event`/tag, cette pipeline de branche a volontairement **0 job** et s'affiche
en échec ("yaml non valide" trompeur — ce n'est pas une erreur de syntaxe, juste GitLab qui
matérialise en pipeline "échec" un refus de `workflow:rules`, cf. "Aucune pipeline ne se
déclenche sur un push nu" plus haut). GitLab associe quand même cette pipeline de branche à la
MR dans l'onglet Pipelines de la MR, ce qui prête à confusion — regarder le badge
déclencheur ("branche" vs "requête de fusion") avant de conclure à un bug de config.

**Pour obtenir la vraie pipeline MR** une fois la MR ouverte sur un commit déjà poussé : soit
pousser un nouveau commit (même vide, `git commit --allow-empty`), soit cliquer **"Exécuter le
pipeline"** dans l'onglet Pipelines de la MR (déclenchement manuel, sans nouveau commit).

---

## Stage `build` — compilation, une seule fois

`mvn package -DskipTests`, artifact `target/*.jar` (1 jour). Rôle double :

1. **Fail-fast.** Sans ce job avant les autres, une erreur de compilation ne serait détectée
   que par le job `test` — après le démarrage du service Postgres et l'exécution des 94 tests
   — alors que `lint` (Checkstyle travaille sur le texte source, pas sur du bytecode) et
   `semgrep`/`trivy-fs` passeraient au vert entre-temps sans rien détecter.
2. **Produire le jar une seule fois**, réutilisé tel quel par `package` (cf. plus bas) au lieu
   de recompiler dans le `Dockerfile` — élimine une double compilation.

**Volontairement `package -DskipTests`, pas `install`/`clean install`.** Le cycle de vie Maven
est cumulatif (`compile → test-compile → test → package → verify → install`) : `install`
exécuterait aussi `test` (les 94 tests, donc il faudrait le service Postgres — on perdrait le
bénéfice "rapide, sans dépendance" qui est le but de ce job) et `verify` (où sont accrochés
`checkstyle-check`/`jacoco-check`, cf. plus bas — un doublon exact des jobs `lint`/`test` qui
tournent déjà dans le stage suivant). `install` copierait en plus le jar dans
`~/.m2/repository`, sans aucun consommateur (pas un projet multi-module, job CI jetable).
`package -DskipTests` est le sous-ensemble minimal qui produit le jar sans rien dupliquer.

### Écart vs le template Node : pas de partage de `.class` entre jobs

Le template dont ce pipeline s'inspire réutilise l'artifact de `build` (`node_modules/`,
`dist/`) dans **tous** les jobs du stage `test` suivant (lint, unit-tests, coverage...). Ici,
seul `package` consomme l'artifact de `build` (le jar final) ; les jobs du stage `test`
(`lint`, `test`, `trivy-fs`, `semgrep`) ne le récupèrent pas et recompilent chacun de leur
côté si besoin. Ce n'est pas un oubli : faire transiter des `.class` compilés entre jobs est
fragile avec Maven (le checkout Git de chaque nouveau job réinitialise les timestamps, ce qui
casse souvent la détection d'incrémentalité et force une recompilation de toute façon) — alors
qu'un jar déjà finalisé (le seul artifact que `package` récupère) n'a aucun problème de ce
genre, c'est un simple fichier binaire copié tel quel dans l'image Docker. Le cache
`.m2/repository` (clé = hash de `pom.xml`, `.maven_cache`) couvre déjà, pour tous les jobs
Maven, le vrai coût qui justifiait le stage `build` côté Node (le téléchargement des
dépendances) — la compilation elle-même reste rapide (quelques secondes) même répétée.

---

## Stage `test`

Tous les jobs ci-dessous tournent en parallèle (même stage), sur `.validate_rules` (MR
`* → develop`, ou tag) — cf. "Principe directeur" plus haut.

### `lint` — Checkstyle

`mvn checkstyle:check`, ruleset [`../checkstyle.xml`](../checkstyle.xml) à la racine.

**Ruleset volontairement restreint**, pas de Sun/Google checks complet : `UnusedImports`,
`RedundantImport`, `EmptyCatchBlock`, `EqualsHashCode`, `EqualsAvoidNull`,
`StringLiteralEquality`, `OneTopLevelClass`, `OuterTypeFilename`, plus une règle regex
interdisant `System.out/err.print` (ce projet logge via SLF4J partout ailleurs). Testé contre
le code existant avant intégration : un ruleset complet (`AvoidStarImport` inclus) aurait fait
échouer 26 fichiers sur une convention établie et assumée du projet (`import
jakarta.persistence.*;` / `import lombok.*;` sur toutes les entités JPA) — pas un bug, un
choix de style. `AvoidStarImport` a donc été explicitement exclu du ruleset plutôt que
d'imposer une réécriture des imports de 13+ fichiers sans rapport avec la mise en place de la
CI. Le seul vrai import mort trouvé (`PlanComptable.java`, import inutilisé de `Entite`) a été
corrigé à cette occasion.

### `test` — JUnit/Mockito + JaCoCo

`mvn test jacoco:report jacoco:check@jacoco-check`, service `postgres:16.4-alpine`.

**`@jacoco-check` (suffixe d'exécution) obligatoire** : `jacoco:check` invoqué en ligne de
commande directe (sans passer par la phase `verify`) utilise sinon une exécution "default-cli"
vide — les `<rules>` (cf. plus bas) ne sont configurées que dans l'exécution nommée
`jacoco-check` du `pom.xml`, jamais reprises par défaut. Erreur sans ce suffixe : `The
parameters 'rules' for goal ... jacoco-maven-plugin:check are missing or invalid`.

#### Piège découvert et corrigé pendant la mise en place de cette CI

`ApiComptaApplicationTests` et `AuthControllerTest` sont des `@SpringBootTest` qui bootent le
contexte Spring complet — contrairement aux 8 autres classes de test (`@ExtendWith
(MockitoExtension.class)`, aucune dépendance externe). Booter ce contexte déclenche
l'autoconfiguration OAuth2 resource server de Spring Boot, qui construit le bean `JwtDecoder`
via `JwtDecoders.fromIssuerLocation(issuer-uri)` — **cet appel fait une découverte OIDC
réseau réelle vers Keycloak au moment de la création du bean**, pas à la première décodification
d'un token. En local, ça passe inaperçu tant que Keycloak tourne déjà (`docker compose up`) ;
sur un runner CI vierge sans Keycloak, ces deux tests auraient fait échouer tout le job.

Corrigé sans toucher au code de prod : nouveau profil Spring **`test`**
([`src/test/resources/application-test.yaml`](../src/test/resources/application-test.yaml)),
activé automatiquement par tous les runs Maven via `maven-surefire-plugin` (`pom.xml`,
`systemPropertyVariables.spring.profiles.active=test`). Ce profil remplace `issuer-uri` par
`jwk-set-uri` (URI factice, jamais résolue) — `NimbusJwtDecoder.withJwkSetUri()` ne fait
**aucun** appel réseau à la construction du bean, seulement à la première décodification
réelle d'un token, ce qu'aucun test de ce projet ne fait aujourd'hui (`AuthControllerTest`
mocke les 5 services dont il dépend). Vérifié empiriquement : les 94 tests passent avec ce
profil actif, contexte Spring inclus, sans que Keycloak soit joignable. Si un futur test a
vraiment besoin de décoder un JWT signé, il faudra remplacer cette URI factice par un vrai
`jwk-set-uri` statique (ou lever ponctuellement l'override pour ce test précis) — pas la peine
avant que ce besoin existe réellement.

Ce même fichier fixe aussi `spring.datasource.url` sur `localhost:5435` — un service dédié
`postgres-test` dans `compose.yaml`, démarré automatiquement avec le `docker compose up`
habituel (pas de profil Compose séparé ni de commande dédiée), distinct de `postgres-metier`
(`5433`, profil `dev`) et volontairement pas sur `5432` : un Postgres natif hors Docker peut
déjà occuper ce port sur la machine du
développeur (piège rencontré et documenté dans `CLAUDE.md`, "Pièges déjà résolus"). Ce service
de test tourne en `tmpfs` (pas de volume nommé) : toujours vide au démarrage, Flyway repart de
zéro à chaque run, comme le service Postgres éphémère de la CI. Le job CI override malgré tout
les 3 valeurs (`SPRING_DATASOURCE_URL/USERNAME/PASSWORD`) pour pointer vers son propre service
`postgres` (réseau isolé par job, alias `postgres`) — les deux mécanismes (local et CI) sont
indépendants, `application-test.yaml` n'est qu'un défaut pratique pour le développement local.

#### JaCoCo : gate scopé à `service/impl`, pas à tout le code

`pom.xml`, exécution `jacoco-check` (phase `verify`) : seuils bloquants (60% lignes / 65%
branches) appliqués uniquement aux classes `**/service/impl/**` — la seule couche soumise à
une vraie discipline de test unitaire dans ce projet (cf. `CLAUDE.md`, section Phase 1).
Étendre le gate au reste du code (DTO, entités, mappers MapStruct, controllers, config)
échouerait sur des classes qui n'ont jamais été visées par une stratégie de test — pas un
signal utile, juste un pipeline rouge en permanence.

**Seuils au niveau BUNDLE (agrégé sur toute la couche), pas per-class.** Plusieurs
`service/impl` (`KeycloakAdminServiceImpl`, `MailServiceImpl`, `KeycloakAuthServiceImpl`...)
sont aujourd'hui à 0% de couverture individuelle — un gate per-class les ferait échouer
immédiatement sans qu'aucun test n'existe encore pour ces classes précises. État réel mesuré
à l'introduction de ce gate (2026-09-08) : ~65% lignes / ~74% branches sur ce bundle ; seuils
fixés en dessous (60/65) pour laisser une marge raisonnable sans être cosmétiques. **À
resserrer progressivement**, et à repasser en gate per-class une fois que les classes
actuellement à 0% auront chacune au moins un test — sinon ce gate reste un plancher, pas une
garantie qu'aucune classe individuelle ne régresse.

Le rapport JaCoCo complet (`target/site/jacoco/`, toutes les classes, pas seulement
`service/impl`) reste généré et exporté en artifact pour visibilité, indépendamment de ce
qui est gate.

### `trivy-fs` / `semgrep` — dépendances et SAST

Même logique à deux passes (rapport large non bloquant + gate étroit bloquant) appliquée à
`trivy-fs` et pas seulement à `trivy-image-*` (cf. stage `security`) : c'est une extension
volontaire de la cohérence du template, pas une exigence du template lui-même. Scanne le
`Dockerfile` (mauvaises pratiques), sans dépendre d'aucun artifact (`dependencies: []`).

#### `pom.xml` volontairement exclu (`--skip-files "pom.xml"`) — piège 429 Maven Central

`trivy-fs` ne scanne **pas** `pom.xml` pour les CVE des dépendances Maven, contrairement à
l'intention initiale. **Découvert et corrigé le 2026-09-08** : dès qu'un `pom.xml` est présent
dans la cible scannée, Trivy tente de résoudre l'arbre effectif des dépendances (parents/BOM en
scope `import`) directement contre `repo.maven.apache.org` — quel que soit le `--scanners`
demandé, **même `misconfig` seul déclenche cette résolution** (vérifié empiriquement, pas
seulement `vuln`). Sur les runners SaaS GitLab (IP partagée entre projets/utilisateurs), ça
déclenche un `429 Too Many Requests` qui fait échouer le job avec 0 rapport produit.

**Piste "pré-remplir le cache Maven local" testée et écartée** : le message d'erreur de Trivy
suggère de peupler `~/.m2/repository` avant le scan (`mvn dependency:resolve`). Testé
empiriquement en montant un `~/.m2/repository` entièrement résolu (`mvn dependency:resolve`
exécuté sur ce projet) dans le conteneur `aquasec/trivy` : **le 429 persiste quand même** —
Trivy redemande des POM (notamment des BOM en scope `import`, ex.
`spring-integration-bom-7.0.5.pom`) que `mvn dependency:resolve` n'a aucune raison de
télécharger lui-même (rien dans l'arbre réel n'en dépend). Partager le cache Maven entre les
jobs `build`/`test` et `trivy-fs` n'aurait donc pas suffi, et aurait en plus nécessité de fixer
`maven.repo.local` (pas configuré actuellement — les jobs Maven utilisent le `~/.m2/repository`
par défaut de l'image, jamais le chemin `.m2/repository` relatif au projet que cache déjà
`.maven_cache` sans effet réel aujourd'hui).

**Corrigé en excluant `pom.xml` du scan** (`--skip-files "pom.xml"` sur les deux passes) —
vérifié empiriquement : plus aucun appel réseau vers Maven Central, `misconfig` sur le
`Dockerfile` reste intact. **Aucune perte de couverture réelle** : `trivy-image-api` (stage
`security`) scanne déjà le jar/l'image construite pour les mêmes CVE Java, sans jamais avoir
besoin de résoudre quoi que ce soit contre Maven Central (les dépendances sont physiquement
présentes dans l'artefact, il suffit de les énumérer) — la détection est simplement décalée de
quelques minutes dans la même pipeline (après `package`, toujours avant la fusion sur une MR
`* → develop`), pas perdue.

`semgrep` scope volontairement à `src/main/java` (via `--exclude "src/test/**"`) : le code
livré, pas les tests. Rulesets `p/java` + `p/security-audit`.

---

## Stage `package`

**Tourne sur `.validate_rules`** (MR `* → develop`, ou tag) — cf. "Principe directeur" plus
haut : construire l'image fait partie de la validation initiale, pas seulement de la
publication finale. Ne tourne pas sur `develop → test`/`test → main` (déjà validé).

**Une seule image** (`$CI_REGISTRY_IMAGE`, pas de suffixe `/api` ni `/front`) : ce repo ne
contient que le backend, contrairement au template Node qui buildait API + Front dans le même
pipeline. Le frontend (`code_source_front/`) est un repo séparé avec, à terme, son propre
pipeline.

`build-api-image` construit toujours l'image (tag local `app:local`) et la sauvegarde en
tarball (`docker save … -o api-image.tar`, artifact 1 jour) — c'est ce tarball, jamais le
registry, que consomme le stage `security` suivant. **Seulement si `$CI_COMMIT_TAG` est
défini** (pipeline de tag), le job va plus loin : `docker login`, tag + push vers
`$CI_REGISTRY_IMAGE` sous trois tags (`$CI_COMMIT_SHORT_SHA`, `$CI_COMMIT_TAG` — ex.
`v1.2.3`, et `buildcache`, mutable, dédié à `--cache-from`), puis vérifie la publication
(`docker manifest inspect`). Jamais `latest`. Sur une pipeline de MR, ce bloc `if` ne s'exécute
juste pas : aucun `docker login`, aucun credential registry utilisé, aucune image poussée
nulle part — l'image locale suffit à la construire et à la scanner.

Le `Dockerfile` (racine du repo) est une image **runtime uniquement** (`eclipse-temurin:21-
jre-alpine`, utilisateur non-root) : `COPY target/*.jar app.jar`, rien d'autre. Il ne compile
plus rien lui-même — `build-api-image` déclare `needs: {job: build, artifacts: true}`, donc
le jar produit par le stage `build` est déjà présent dans le contexte de build au moment du
`docker build .`. Recompiler une seconde fois dans le Dockerfile (comme le ferait un stage
Maven multi-stage) serait redondant : le stage `build` a déjà produit exactement ce jar, avec
les mêmes sources, quelques minutes plus tôt dans la même pipeline.

**Conséquence pour un build local hors CI** : `docker build .` seul ne suffit plus (pas de
jar dans `target/` sur un clone frais) — il faut d'abord `mvn package -DskipTests`. Documenté
en commentaire en tête du `Dockerfile`.

---

## Stage `security`

**`trivy-image-api`/`sbom-api` tournent sur `.validate_rules`** (MR `* → develop`, ou tag), à
partir du tarball produit par `package` (`needs: {job: build-api-image, artifacts: true}`,
`trivy image --input api-image.tar`, `syft docker-archive:api-image.tar`) — **jamais du
registry**, donc aucun credential registry nécessaire pour ces deux jobs, identique en
contexte MR ou tag. Bénéfice concret de ce choix : sur GitLab avec le tier Ultimate, le
rapport `container_scanning` s'affiche dans le widget sécurité de la MR `* → develop` *avant*
la fusion — pas seulement après coup sur un pipeline de tag séparé.

**`sign-image-api` reste réservé au tag** (`.tag_only`) : signer suppose une image réellement
publiée dans un registry (cosign signe une référence OCI), ce qui n'existe que sur un
pipeline de tag. Deux écarts d'implémentation vs le template Node :

- installe le binaire `cosign` via un téléchargement GitHub Releases épinglé (`v2.4.1`)
  plutôt que via `apk add cosign` : le paquet n'est pas dans le dépôt `alpine:3` de référence
  utilisé ici, contrairement à ce que suggérait le template. Signature via `cosign login`
  (pas de `docker login`/`docker-cli`) : cosign n'a pas besoin d'un daemon Docker pour signer,
  seulement d'un accès réseau à l'API OCI du registry.
- **Prérequis Fulcio/OIDC public non vérifié pour ce projet** : `sign-image-api` a
  `allow_failure: true`. Si le GitLab utilisé pour ce projet est self-hosted sans exposition
  publique de son endpoint OIDC, ce job échouera à l'émission du certificat — sans bloquer le
  reste du pipeline. À retirer le jour où c'est confirmé fonctionnel.

### CVE réelles trouvées en testant ce scan — corrigées (2026-09-08)

Un test local (`docker save` + `trivy image --input`) a fait remonter, sur l'image telle
qu'elle était produite à l'introduction de cette CI, des CVE **CRITICAL avec correctif
disponible** : `org.apache.tomcat.embed:tomcat-embed-core` (Tomcat embarqué par
`spring-boot-starter-webmvc`) en 11.0.22 — trois CVE (CVE-2026-65182, CVE-2026-65905,
CVE-2026-68525) — et `org.postgresql:postgresql` 42.7.11, une CVE HIGH (CVE-2026-54291). Avec
le gate tel que configuré (`--severity CRITICAL --ignore-unfixed --exit-code 1`), le tout
premier pipeline aurait échoué au stage `security`.

**Corrigé** en ajoutant deux overrides de propriété dans `pom.xml` (`tomcat.version=11.0.25`,
`postgresql.version=42.7.12` — le parent `spring-boot-dependencies:4.0.7` expose ces deux
propriétés, confirmé via le `.pom` en cache local avant de les fixer) : le jar applicatif est
revenu à 0 vulnérabilité CRITICAL/HIGH après reconstruction (vérifié par un nouveau scan du
tarball). Il reste 3 CVE HIGH (non bloquantes) sur l'OS de base `eclipse-temurin:21-jre-alpine`
(OpenSSL) — hors de portée d'un override Maven, dépendent de la prochaine publication de
l'image de base par Adoptium ; à revérifier périodiquement (`trivy-fs`/`trivy-image-api` le
referont de toute façon à chaque pipeline).

---

## Ce qui manque encore (CD)

Aucun stage `deploy`. Pas d'infra cible connue à ce jour. Deux points d'ancrage déjà identifiés
(cf. "CD future : où viendra le déploiement" plus haut) : `develop → test` pour la recette
(réutilisant l'image déjà construite par `* → develop`, sans rien reconstruire) et le tag
`vX.Y.Z` pour la prod (déploiement irréversible, à gater derrière un geste manuel explicite
`when: manual` même sur un pipeline de tag). Le mécanisme de réutilisation d'artefact
inter-pipelines pour la recette reste à concevoir — pas tranché tant qu'il n'y a rien à
déployer.

---

## Variables CI/CD à configurer manuellement

Aucune pour l'instant. `CI_REGISTRY_USER`/`CI_REGISTRY_PASSWORD`/`CI_REGISTRY_IMAGE` sont
fournies automatiquement par GitLab. `SIGSTORE_ID_TOKEN` est un jeton OIDC émis
automatiquement (`id_tokens:`), rien à créer. Sans stage `deploy`, aucun secret d'infra
(SSH, base de données cible...) n'est nécessaire pour le moment — à ajouter quand la CD sera
mise en place.
