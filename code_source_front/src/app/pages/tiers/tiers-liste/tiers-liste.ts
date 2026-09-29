import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CurrentUserService } from '../../../core/current-user.service';
import { extraireMessageErreur } from '../../../core/http-error.util';
import { TiersPageReadDto, TiersReadDto, TiersRecapReadDto, TypeTiers } from '../../../core/models';
import { TiersService } from '../../../core/tiers.service';
import { Alert } from '../../../shared/ui/alert/alert';
import { StatusBadge } from '../../../shared/ui/status-badge/status-badge';
import {
  couleurPastilleTypeTiers,
  LIBELLES_TYPE_TIERS,
  TypeTiersBadge,
} from '../../../shared/ui/type-tiers-badge/type-tiers-badge';
import { TiersAssociationModal } from '../tiers-association-modal/tiers-association-modal';
import { TiersCreationModal } from '../tiers-creation-modal/tiers-creation-modal';

const TAILLE_PAGE = 6;
const DELAI_DEBOUNCE_MS = 200;
const ROLES_GESTION_TIERS = ['ADMIN', 'COMPTABLE'];

const TYPES_TIERS: TypeTiers[] = ['CLIENT', 'FOURNISSEUR', 'SALARIE', 'ORGANISME_SOCIAL', 'AUTRE'];

@Component({
  selector: 'app-tiers-liste',
  imports: [RouterLink, Alert, StatusBadge, TypeTiersBadge, TiersCreationModal, TiersAssociationModal],
  templateUrl: './tiers-liste.html',
  styleUrl: './tiers-liste.scss',
})
export class TiersListe {
  private readonly tiersService = inject(TiersService);
  private readonly currentUserService = inject(CurrentUserService);

  protected readonly typesTiers = TYPES_TIERS;
  protected readonly libellesType = LIBELLES_TYPE_TIERS;

  readonly peutGererTiers = computed(() =>
    ROLES_GESTION_TIERS.includes(this.currentUserService.utilisateur()?.role ?? ''),
  );

  readonly recap = signal<TiersRecapReadDto | null>(null);
  readonly erreurRecap = signal<string | null>(null);
  readonly kpiParType = computed(() => {
    const parType = this.recap()?.parType;
    return this.typesTiers.map((type) => ({
      type,
      libelle: this.libellesType[type],
      total: parType?.[type] ?? 0,
      couleur: couleurPastilleTypeTiers(type),
    }));
  });

  readonly enCours = signal(false);
  readonly erreur = signal<string | null>(null);
  readonly page = signal<TiersPageReadDto | null>(null);

  readonly query = signal('');
  readonly typeFiltre = signal<TypeTiers | ''>('');
  readonly statutFiltre = signal<'' | 'true' | 'false'>('');
  readonly sort = signal('raison,asc');
  readonly pageIndex = signal(0);
  private minuteurDebounce?: ReturnType<typeof setTimeout>;

  readonly tiers = computed(() => this.page()?.content ?? []);
  readonly pageAffichee = computed(() => (this.page()?.page ?? 0) + 1);
  readonly totalPages = computed(() => Math.max(1, this.page()?.totalPages ?? 1));
  readonly totalElements = computed(() => this.page()?.totalElements ?? 0);
  readonly estPremierePage = computed(() => this.page()?.premierePage ?? true);
  readonly estDernierePage = computed(() => this.page()?.dernierePage ?? true);

  readonly modaleCreationOuverte = signal(false);
  readonly tiersPourAssociation = signal<TiersReadDto | null>(null);
  readonly confirmationDissociation = signal<TiersReadDto | null>(null);
  readonly enCoursDissociation = signal(false);

  constructor() {
    this.chargerRecap();
    this.chargerTiers();
  }

  onSaisieRecherche(valeur: string): void {
    this.query.set(valeur);
    clearTimeout(this.minuteurDebounce);
    this.minuteurDebounce = setTimeout(() => {
      this.pageIndex.set(0);
      this.chargerTiers();
    }, DELAI_DEBOUNCE_MS);
  }

  filtrerParType(type: TypeTiers | ''): void {
    this.typeFiltre.set(type);
    this.pageIndex.set(0);
    this.chargerTiers();
  }

  filtrerParStatut(statut: '' | 'true' | 'false'): void {
    this.statutFiltre.set(statut);
    this.pageIndex.set(0);
    this.chargerTiers();
  }

  changerTri(sort: string): void {
    this.sort.set(sort);
    this.pageIndex.set(0);
    this.chargerTiers();
  }

  pagePrecedente(): void {
    if (this.estPremierePage()) return;
    this.pageIndex.update((p) => Math.max(0, p - 1));
    this.chargerTiers();
  }

  pageSuivante(): void {
    if (this.estDernierePage()) return;
    this.pageIndex.update((p) => p + 1);
    this.chargerTiers();
  }

  ouvrirModaleCreation(): void {
    this.modaleCreationOuverte.set(true);
  }

  fermerModaleCreation(): void {
    this.modaleCreationOuverte.set(false);
  }

  onTiersCree(): void {
    this.chargerRecap();
    this.chargerTiers();
  }

  ouvrirAssociation(tiers: TiersReadDto): void {
    this.tiersPourAssociation.set(tiers);
  }

  fermerAssociation(): void {
    this.tiersPourAssociation.set(null);
  }

  onCompteAssocie(tiersMisAJour: TiersReadDto): void {
    this.remplacerDansLaPage(tiersMisAJour);
    this.tiersPourAssociation.set(null);
    this.chargerRecap();
  }

  demanderDissociation(tiers: TiersReadDto): void {
    this.confirmationDissociation.set(tiers);
  }

  annulerDissociation(): void {
    this.confirmationDissociation.set(null);
  }

  confirmerDissociation(): void {
    const tiers = this.confirmationDissociation();
    if (!tiers) return;

    this.enCoursDissociation.set(true);
    this.tiersService.associerCompte(tiers.id, null).subscribe({
      next: (tiersMisAJour) => {
        this.enCoursDissociation.set(false);
        this.remplacerDansLaPage(tiersMisAJour);
        this.confirmationDissociation.set(null);
        this.chargerRecap();
      },
      error: (erreur) => {
        this.enCoursDissociation.set(false);
        this.erreur.set(extraireMessageErreur(erreur));
        this.confirmationDissociation.set(null);
      },
    });
  }

  private remplacerDansLaPage(tiersMisAJour: TiersReadDto): void {
    this.page.update((p) =>
      p ? { ...p, content: p.content.map((t) => (t.id === tiersMisAJour.id ? tiersMisAJour : t)) } : p,
    );
  }

  private chargerRecap(): void {
    this.erreurRecap.set(null);
    this.tiersService.obtenirRecap().subscribe({
      next: (recap) => this.recap.set(recap),
      error: (erreur) => this.erreurRecap.set(extraireMessageErreur(erreur)),
    });
  }

  private chargerTiers(): void {
    this.enCours.set(true);
    this.erreur.set(null);

    this.tiersService
      .rechercherTiers({
        q: this.query().trim() || undefined,
        type: this.typeFiltre() || undefined,
        actif: this.statutFiltre() === '' ? undefined : this.statutFiltre() === 'true',
        sort: this.sort(),
        page: this.pageIndex(),
        size: TAILLE_PAGE,
      })
      .subscribe({
        next: (page) => {
          this.page.set(page);
          this.enCours.set(false);
        },
        error: (erreur) => {
          this.erreur.set(extraireMessageErreur(erreur));
          this.enCours.set(false);
        },
      });
  }
}
