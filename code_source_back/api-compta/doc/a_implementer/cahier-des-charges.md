# Cahier des charges — Phases 1 à 7

Rédigé du point de vue double : chef de projet (découpage, dépendances, critères d'acceptation) et expert-comptable OHADA (conformité au référentiel réglementaire). Périmètre : **SYSCOHADA révisé pur** (AUDCIF, applicable de manière commune aux 17 États membres OHADA) — sans spécificités fiscales/sociales d'un pays donné, qui feront l'objet d'un paramétrage ultérieur.

Contenu **purement métier** (règles, concepts, contrôles, livrables attendus) — pas de proposition d'entités techniques/migrations à ce stade.

---

## Préambule — Cadre réglementaire de référence

Le référentiel comptable applicable dans l'espace OHADA (17 États : Bénin, Burkina Faso, Cameroun, Comores, Congo, Côte d'Ivoire, Gabon, Guinée, Guinée-Bissau, Guinée équatoriale, Mali, Niger, RCA, RDC, Sénégal, Tchad, Togo) est fixé par l'**Acte Uniforme relatif au Droit Comptable et à l'Information Financière (AUDCIF)**, adopté le 26 janvier 2017 à Brazzaville, publié au Journal Officiel le 15 février 2017, en vigueur depuis le **1er janvier 2018** pour les comptes personnels des entités (1er janvier 2019 pour les comptes consolidés/combinés et les états financiers en normes IFRS).

Ce texte définit le **SYSCOHADA révisé**, qui comprend :
- le **Système Normal (SN)** — applicable par défaut aux entités dont le chiffre d'affaires dépasse les seuils du SMT ;
- le **Système Minimal de Trésorerie (SMT)** — pour les très petites entités (seuils indicatifs de chiffre d'affaires annuel hors taxes, pouvant varier légèrement selon les textes nationaux : environ 60 millions FCFA pour le négoce, 40 millions pour l'artisanat, 30 millions pour les services), fondé sur une comptabilité de trésorerie (recettes-dépenses) plutôt que d'engagement ;
- le **SYCEBNL** (Système Comptable des Entités à But Non Lucratif), acte uniforme distinct plus récent, pour les associations/ONG/entités sans but lucratif, qui ne relèvent pas du champ d'application du SYSCOHADA "classique".

**Point de conformité rassurant** : le projet a déjà anticipé cette structure — les 3 référentiels seedés en base (`SYSCOHADA_NORMAL`, `SYSCOHADA_SMT`, `SYCEBNL`) correspondent exactement à ce découpage réglementaire réel. Ne pas les remettre en cause, seulement les compléter.

Sont **hors champ** du SYSCOHADA (donc hors périmètre de cette application, sauf demande explicite future) : établissements de crédit, institutions de microfinance, acteurs des marchés financiers, sociétés d'assurance/réassurance, organismes de sécurité sociale — qui relèvent de plans comptables sectoriels distincts.

---

## Phase 1 — Moteur comptable (détaillé)

### Pourquoi cette phase conditionne tout le reste
Aucune facture, aucune immobilisation, aucune paie ne peut produire une écriture comptable valide sans un moteur qui sait : ouvrir un exercice, tenir un journal, enregistrer une écriture équilibrée, en dériver un grand livre et une balance, et clôturer proprement. C'est le socle non négociable — les phases 2 à 4 ne feront **qu'alimenter** ce moteur, jamais le contourner.

### 1.1 — Exercice comptable
- Durée normale : **12 mois**, correspondant en général à l'année civile (1er janvier–31 décembre), sauf statuts de l'entreprise prévoyant une autre date de clôture.
- **Premier exercice** : peut être inférieur ou supérieur à 12 mois (jusqu'à 24 mois dans certains cas, selon la date de constitution de l'entreprise en cours d'année) — règle métier à prévoir dès la création d'un exercice, pas seulement en régime de croisière.
- Un exercice a un statut : **ouvert** (des écritures peuvent y être passées) ou **clôturé** (plus aucune écriture nouvelle, sauf réouverture explicite par un rôle autorisé — cas rare, à documenter comme exception et non comme flux normal).
- La clôture d'un exercice :
  - solde les comptes de gestion (charges classe 6, produits classe 7, HAO classe 8) pour déterminer le résultat de l'exercice ;
  - génère les **à-nouveaux** : report des soldes des comptes de bilan (classes 1 à 5) en ouverture de l'exercice suivant.

### 1.2 — Journaux
- **Journal général** obligatoire — enregistre chronologiquement toutes les opérations.
- **Journaux auxiliaires** (faculté, mais pratique quasi-systématique en cabinet) : achats, ventes, banque, caisse, opérations diverses (OD). Chacun doit être **centralisé dans le journal général au moins une fois par mois** (règle légale explicite) — pas de centralisation en temps réel obligatoire, mais une périodicité maximale.
- Chaque écriture appartient à **un seul journal**, un seul exercice, et porte une **numérotation séquentielle continue** (pas de trou, pas de réutilisation de numéro) au sein de son journal.

### 1.3 — Écriture comptable et partie double
- Une écriture = un ensemble de **lignes** (2 au minimum), chacune portant : un compte du plan comptable actif de l'entreprise, un sens (débit **ou** crédit, jamais les deux sur une même ligne), un montant, un libellé, et une référence à une pièce justificative.
- **Principe de la partie double** : pour toute écriture, `Σ débits = Σ crédits`. C'est un contrôle **bloquant à la validation**, pas un avertissement — une écriture déséquilibrée ne doit jamais pouvoir devenir définitive.
- Corollaire structurel (garanti automatiquement si chaque écriture est équilibrée) : à tout instant, `Σ soldes débiteurs de la balance = Σ soldes créditeurs`, et l'égalité fondamentale `Actif = Passif + Résultat` reste vérifiable.
- **Immutabilité après validation** : les livres légaux doivent être tenus "sans blanc ni altération d'aucune sorte" — une écriture validée ne se modifie ni ne se supprime. Toute correction se fait par **contre-passation / écriture d'extourne** (une nouvelle écriture qui annule la précédente en sens inverse), jamais par édition destructive. Un statut intermédiaire **brouillon** (librement modifiable) avant validation est la pratique professionnelle réelle et doit être prévu — sans quoi la saisie quotidienne d'un comptable devient impraticable.
- Chaque écriture référence une **pièce justificative** (numéro, nature) — le rattachement physique du document sera traité en Phase 5, mais le champ de référence doit exister dès cette phase pour rester conforme à l'obligation légale de traçabilité.
- Une écriture datée dans un exercice déjà clôturé doit être refusée par construction.

### 1.4 — Grand livre et balance (dérivés, jamais saisis)
- **Grand livre** : pour chaque compte, l'historique chronologique de ses mouvements (débit/crédit) avec solde progressif — **résultat d'un calcul** sur les lignes d'écritures validées, jamais une saisie manuelle indépendante.
- **Balance générale** : pour une période donnée, la liste des comptes mouvementés avec leurs totaux débit/crédit et leur solde — également dérivée, avec le contrôle d'équilibre global mentionné en 1.3 comme critère de cohérence permanent.
- Ces deux documents font partie des **livres comptables obligatoires** au sens de l'Acte Uniforme (avec le livre-journal et le livre d'inventaire) — leur exactitude n'est pas une fonctionnalité de confort, c'est une obligation légale.

### 1.5 — Conservation et intégrité
- Les documents comptables (livre-journal notamment) doivent être conservés **au minimum 10 ans** après la clôture de l'exercice — à garder en tête pour la politique de rétention des données, même si l'implémentation technique de l'archivage n'est pas l'objet de cette phase.

### 1.6 — Critères d'acceptation métier (Definition of Done — Phase 1)
1. Un rôle autorisé peut créer un exercice comptable pour son entreprise (dates de début/fin cohérentes, statut ouvert par défaut).
2. Un rôle autorisé peut créer/lister les journaux de son entreprise (au minimum le journal général ; les journaux auxiliaires si retenus dès cette phase).
3. Un utilisateur autorisé peut saisir une écriture en brouillon avec au moins 2 lignes ; le système **empêche la validation** si `Σ débits ≠ Σ crédits`.
4. Une écriture validée devient immuable ; seule une contre-passation permet de l'annuler — aucune route de suppression/édition directe sur une écriture validée.
5. Le grand livre d'un compte donné et la balance d'une période donnée sont calculés automatiquement, cohérents entre eux et avec les écritures validées sous-jacentes.
6. La clôture d'un exercice bloque toute nouvelle écriture sur cet exercice et génère correctement les à-nouveaux (report des soldes de bilan, remise à zéro des comptes de gestion).
7. Toute écriture datée hors des bornes de l'exercice, ou sur un exercice clôturé, est rejetée.

### 1.7 — Questions ouvertes à trancher avant de démarrer l'implémentation
- **Journaux auxiliaires dès la Phase 1, ou report en Phase 2 ?** (Ils prennent tout leur sens avec la facturation automatique — à discuter : les inclure maintenant simplifie l'architecture des écritures, les reporter réduit le périmètre initial.)
- **Qui valide une écriture** (rôle dédié distinct de qui la saisit — séparation des tâches courante en cabinet — ou même rôle) ?
- **Réouverture d'exercice clôturé** : autorisée pour qui, avec quelle traçabilité (cas rare mais réel en pratique : correction post-clôture avant dépôt de la liasse fiscale) ?
- **Devise et arrondis** : montants tenus en unité entière ou avec décimales selon le pays/devise de l'entreprise (le FCFA et le CDF n'utilisent pas de centimes usuels) ?

---

## Phases 2 à 7 — Vue d'ensemble (à approfondir phase par phase, en temps voulu)

### Phase 2 — Facturation & tiers
**Objectif** : une facture client ou fournisseur génère automatiquement une écriture conforme (vente : crédit compte de produit classe 7, débit client 411, TVA collectée 443 le cas échéant ; achat : débit charge classe 6, crédit fournisseur 401, TVA déductible 445).
**Dépendances** : Phase 1 (moteur), module Tiers déjà existant, plan comptable déjà existant.
**Points d'attention réglementaires** : la TVA (taux et règles d'exonération varient par pays OHADA — hors périmètre SYSCOHADA pur, à cadrer séparément) ; le lettrage des règlements avec les factures ; la gestion des avoirs/notes de crédit.

### Phase 3 — Achats & immobilisations
**Objectif** : commandes/réceptions, et surtout un **plan d'amortissement** par immobilisation (méthode linéaire ou dégressive, sur la durée d'utilité) générant automatiquement les écritures de dotation (débit 68x dotations aux amortissements, crédit 28x amortissements cumulés) à chaque clôture de période.
**Dépendances** : Phase 1 (moteur), Phase 2 idéalement (une immobilisation naît souvent d'une facture d'achat).
**Points d'attention réglementaires** : distinction charge immédiate vs immobilisation à capitaliser (seuil de capitalisation) ; amortissement dérogatoire (écart fiscal/comptable, classe 15/145) ; sortie/cession d'immobilisation en classe 8 (Hors Activités Ordinaires).

### Phase 4 — RH & personnel
**Objectif** : contrats, absences, congés, et écritures de charges de personnel (661 appointements, 663/664 charges sociales, comptes organismes sociaux classe 43).
**Dépendances** : Phase 1 (moteur).
**Point d'attention majeur** : c'est le domaine **le plus dépendant de la législation nationale** (barèmes d'impôt sur les rémunérations, taux de cotisations sociales — CNSS, INSS selon le pays), qui n'est **pas** couvert par le SYSCOHADA lui-même (purement comptable). Cohérent avec le choix acté de rester "SYSCOHADA pur" pour l'instant : cette phase nécessitera un cadrage pays par pays le moment venu, distinct du présent document.

### Phase 5 — Gestion documentaire (transversal)
**Objectif** : rattacher des pièces justificatives à n'importe quelle écriture, facture ou contrat — ce n'est pas une fonctionnalité de confort, c'est la matérialisation de l'obligation légale de traçabilité mentionnée en 1.3/1.5.
**Point d'attention** : la politique de conservation doit respecter le délai légal minimal de 10 ans évoqué en 1.5.

### Phase 6 — États financiers
**Objectif** : Bilan, Compte de Résultat, État des Flux de Trésorerie (remplace l'ancien TAFIRE du système comptable OHADA de 1997 — le TFT actuel a des rubriques proches mais une construction simplifiée), Notes annexes — générés depuis les données accumulées dans les phases précédentes.
**Point d'attention important** : le contenu exact de la liasse **dépend du système comptable choisi par l'entreprise** (Système Normal / SMT / SYCEBNL ont chacun leur propre jeu d'états) — ne pas concevoir un format unique arbitraire, la variation est réglementaire, pas cosmétique.

### Phase 7 — Reporting & tableaux de bord
**Objectif** : ratios, KPI, tableaux de pilotage pour dirigeants et experts-comptables, au-delà des obligations légales strictes.
**Point d'attention** : peu de contrainte réglementaire ici — phase davantage guidée par les besoins utilisateurs à recueillir spécifiquement en temps voulu, plutôt que par le droit comptable.

---

## Sources consultées

- [OHADA — Uniform Act relating to accounting law and financial information (AUDCIF)](https://www.ohada.org/en/uniform-act-relating-to-accounting-law-and-financial-information-audcif/)
- [OHADA.com — Publication du nouvel AUDCIF](https://www.ohada.com/actualite/3349/publication-du-nouvel-acte-uniforme-relatif-au-droit-comptable-et-a-linformation-financiere-audcif.html)
- [Texte intégral AUDCIF 2017 (LegalRDC)](https://legalrdc.com/wp-content/uploads/2019/11/AUDCIF_2017_LegalRDC.pdf)
- [OHADA.com — Acte uniforme relatif au SYCEBNL](https://www.ohada.com/actualite/6569/acte-uniforme-relatif-au-systeme-comptable-des-entites-a-but-non-lucratif-sycebnl.html)
- [Tresogo — Les 9 classes du plan comptable SYSCOHADA](https://tresogo.com/syscohada/classe)
- [Masterclass.cd — La codification du plan comptable SYSCOHADA](https://masterclass.cd/lessons/2-la-codification-du-plan-comptable-syscohada/)
- [Actualités du droit OHADA — Les obligations comptables du commerçant](http://www.actualitesdroitohada.com/droit-commercial/le-statut-du-commercant/le-statut-du-commercant-en-droit-ohada/les-obligations-comptables-du-commercant)
- [Droit-Afrique — Acte Uniforme sur la comptabilité des entreprises (texte)](https://justice.gouv.km/wp-content/uploads/2025/03/ohada-acte-uniforme-2000-comptabilite.pdf)
- [Banana Comptabilité — OHADA Système Minimal de Trésorerie (SMT)](https://www.banana.ch/apps/fr/node/9618)
- [GEDBERED — SYSCOHADA révisé : plan de comptes & états financiers](https://www.gedbered.com/blog/syscohada-revise-obligations-comptables-pme.html)
- [Plan-comptable-ohada.com — Compte 28 : Amortissements](https://plan-comptable-ohada.com/nouvelle-norme-2016/compte/28.html)

**Note de fiabilité** : les seuils chiffrés du SMT (chiffre d'affaires) varient légèrement selon les textes nationaux de transposition — à reconfirmer avec le texte officiel du/des pays cibles avant implémentation de tout contrôle bloquant basé sur ces seuils. Le reste (structure des livres, principe de partie double, classes de comptes, structure des états financiers) est stable et commun aux 17 États membres.
