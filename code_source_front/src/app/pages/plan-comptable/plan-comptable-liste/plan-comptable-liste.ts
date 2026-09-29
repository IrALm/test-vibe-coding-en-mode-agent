import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CompteComptableCreationModal } from '../compte-comptable-creation-modal/compte-comptable-creation-modal';
import { CurrentUserService } from '../../../core/current-user.service';
import { extraireMessageErreur } from '../../../core/http-error.util';
import {
  ClasseCompteComptableReadDto,
  CompteComptablePageReadDto,
  CompteComptableReadDto,
  PlanComptableRecapReadDto,
  SensCompte,
} from '../../../core/models';
import { PlanComptableService } from '../../../core/plan-comptable.service';
import { Alert } from '../../../shared/ui/alert/alert';
import { StatusBadge } from '../../../shared/ui/status-badge/status-badge';

const TAILLE_PAGE_COMPTES = 6;
const DELAI_DEBOUNCE_MS = 200;
const ROLES_CREATION_COMPTE = ['ADMIN', 'ADMIN_FINANCIER'];

@Component({
  selector: 'app-plan-comptable-liste',
  imports: [RouterLink, Alert, StatusBadge, CompteComptableCreationModal],
  templateUrl: './plan-comptable-liste.html',
  styleUrl: './plan-comptable-liste.scss',
})
export class PlanComptableListe {
  private readonly planComptableService = inject(PlanComptableService);
  private readonly currentUserService = inject(CurrentUserService);

  readonly peutCreerCompte = computed(() =>
    ROLES_CREATION_COMPTE.includes(this.currentUserService.utilisateur()?.role ?? ''),
  );
  readonly modaleCreationOuverte = signal(false);

  readonly recap = signal<PlanComptableRecapReadDto | null>(null);
  readonly erreurRecap = signal<string | null>(null);

  readonly classes = signal<ClasseCompteComptableReadDto[]>([]);
  // Snapshot non filtré des classes (mis à jour uniquement quand la recherche du rail est vide) :
  // sert à retrouver la classe d'un compte affiché en résultat de recherche globale, indépendamment
  // d'un filtre de rail actif en parallèle qui pourrait exclure cette classe de `classes()`.
  readonly toutesLesClasses = signal<ClasseCompteComptableReadDto[]>([]);
  readonly erreurClasses = signal<string | null>(null);
  readonly queryClasses = signal('');
  private minuteurDebounceClasses?: ReturnType<typeof setTimeout>;

  readonly classeSelectionneeId = signal<string | null>(null);
  readonly classeSelectionnee = computed(
    () => this.classes().find((c) => c.id === this.classeSelectionneeId()) ?? null,
  );

  readonly enCoursComptes = signal(false);
  readonly erreurComptes = signal<string | null>(null);
  readonly page = signal<CompteComptablePageReadDto | null>(null);
  readonly queryComptes = signal('');
  readonly sensFiltre = signal<SensCompte | ''>('');
  readonly pageIndex = signal(0);
  private minuteurDebounceComptes?: ReturnType<typeof setTimeout>;

  // Recherche globale (toutes classes) dès que le champ compte contient une requête - la recherche
  // scopée à la classe sélectionnée reste le comportement par défaut (champ vide).
  readonly rechercheGlobaleActive = computed(() => this.queryComptes().trim() !== '');

  readonly comptes = computed(() => this.page()?.content ?? []);
  readonly pageAffichee = computed(() => (this.page()?.page ?? 0) + 1);
  readonly totalPages = computed(() => Math.max(1, this.page()?.totalPages ?? 1));
  readonly totalElements = computed(() => this.page()?.totalElements ?? 0);
  readonly estPremierePage = computed(() => this.page()?.premierePage ?? true);
  readonly estDernierePage = computed(() => this.page()?.dernierePage ?? true);

  constructor() {
    // Chargées volontairement en séquence, pas en parallèle : deux requêtes authentifiées
    // simultanées peuvent toutes les deux déclencher un rafraîchissement de session Keycloak
    // (refresh token à usage unique - revokeRefreshToken=true côté realm), et l'une des deux
    // se retrouver avec un refresh token déjà consommé par l'autre, invalidant la session.
    this.chargerRecap();
  }

  onSaisieRechercheClasses(valeur: string): void {
    this.queryClasses.set(valeur);
    clearTimeout(this.minuteurDebounceClasses);
    this.minuteurDebounceClasses = setTimeout(() => this.chargerClasses(valeur), DELAI_DEBOUNCE_MS);
  }

  selectionnerClasse(classe: ClasseCompteComptableReadDto): void {
    if (this.classeSelectionneeId() === classe.id) return;
    this.classeSelectionneeId.set(classe.id);
    this.reinitialiserPanneau();
    this.chargerComptes();
  }

  onSaisieRechercheComptes(valeur: string): void {
    this.queryComptes.set(valeur);
    clearTimeout(this.minuteurDebounceComptes);
    this.minuteurDebounceComptes = setTimeout(() => {
      this.pageIndex.set(0);
      this.chargerComptes();
    }, DELAI_DEBOUNCE_MS);
  }

  filtrerParSens(sens: SensCompte | ''): void {
    this.sensFiltre.set(sens);
    this.pageIndex.set(0);
    this.chargerComptes();
  }

  pagePrecedente(): void {
    if (this.estPremierePage()) return;
    this.pageIndex.update((p) => Math.max(0, p - 1));
    this.chargerComptes();
  }

  pageSuivante(): void {
    if (this.estDernierePage()) return;
    this.pageIndex.update((p) => p + 1);
    this.chargerComptes();
  }

  ouvrirModaleCreation(): void {
    this.modaleCreationOuverte.set(true);
  }

  fermerModaleCreation(): void {
    this.modaleCreationOuverte.set(false);
  }

  /** Le compte créé peut modifier le compteur de la classe (rail) et la liste affichée du panneau. */
  onCompteCree(): void {
    this.chargerClasses(this.queryClasses());
    this.chargerComptes();
  }

  private chargerRecap(): void {
    this.erreurRecap.set(null);
    this.planComptableService.obtenirRecap().subscribe({
      next: (recap) => {
        this.recap.set(recap);
        this.chargerClasses();
      },
      error: (erreur) => {
        this.erreurRecap.set(extraireMessageErreur(erreur));
        this.chargerClasses();
      },
    });
  }

  private chargerClasses(q?: string): void {
    this.erreurClasses.set(null);
    this.planComptableService.listerClasses(q).subscribe({
      next: (classes) => {
        this.classes.set(classes);
        if (!q) {
          this.toutesLesClasses.set(classes);
        }
        const selectionEncorePresente = classes.some((c) => c.id === this.classeSelectionneeId());
        if (!selectionEncorePresente) {
          this.classeSelectionneeId.set(classes[0]?.id ?? null);
          this.reinitialiserPanneau();
          if (classes[0]) {
            this.chargerComptes();
          } else {
            this.page.set(null);
          }
        }
      },
      error: (erreur) => this.erreurClasses.set(extraireMessageErreur(erreur)),
    });
  }

  private reinitialiserPanneau(): void {
    this.queryComptes.set('');
    this.sensFiltre.set('');
    this.pageIndex.set(0);
    this.page.set(null);
    this.erreurComptes.set(null);
  }

  /** Classe d'un compte affiché en résultat de recherche globale, déduite du premier chiffre de son numéro (codification décimale SYSCOHADA : la classe est toujours ce premier chiffre). */
  classeDuCompte(compte: CompteComptableReadDto): ClasseCompteComptableReadDto | undefined {
    const premierChiffre = compte.numero.charAt(0);
    return this.toutesLesClasses().find((c) => String(c.numero) === premierChiffre);
  }

  private chargerComptes(): void {
    const classeId = this.classeSelectionneeId();
    if (!classeId) return;

    this.enCoursComptes.set(true);
    this.erreurComptes.set(null);

    const criteres = {
      q: this.queryComptes().trim() || undefined,
      sens: this.sensFiltre() || undefined,
      page: this.pageIndex(),
      size: TAILLE_PAGE_COMPTES,
    };

    const requete = this.rechercheGlobaleActive()
      ? this.planComptableService.rechercherComptesGlobal(criteres)
      : this.planComptableService.rechercherComptes(classeId, criteres);

    requete.subscribe({
      next: (page) => {
        this.page.set(page);
        this.enCoursComptes.set(false);
      },
      error: (erreur) => {
        this.erreurComptes.set(extraireMessageErreur(erreur));
        this.enCoursComptes.set(false);
      },
    });
  }
}
