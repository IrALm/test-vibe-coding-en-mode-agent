import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { formaterMontant } from '../../../core/devise.util';
import { EcritureService } from '../../../core/ecriture.service';
import { EntiteService } from '../../../core/entite.service';
import { ExerciceService } from '../../../core/exercice.service';
import { extraireMessageErreur } from '../../../core/http-error.util';
import { EcriturePageReadDto, ExerciceReadDto, StatutEcriture } from '../../../core/models';
import { Alert } from '../../../shared/ui/alert/alert';
import {
  EcritureStatutBadge,
  LIBELLES_STATUT_ECRITURE,
} from '../../../shared/ui/ecriture-statut-badge/ecriture-statut-badge';

const TAILLE_PAGE = 20;
const DELAI_DEBOUNCE_MS = 200;
const STATUTS: StatutEcriture[] = ['BROUILLON', 'EN_ATTENTE', 'VALIDEE', 'CONTREPASSEE'];

@Component({
  selector: 'app-ecritures-liste',
  imports: [RouterLink, Alert, EcritureStatutBadge, DatePipe],
  templateUrl: './ecritures-liste.html',
  styleUrl: './ecritures-liste.scss',
})
export class EcrituresListe {
  private readonly ecritureService = inject(EcritureService);
  private readonly exerciceService = inject(ExerciceService);
  private readonly entiteService = inject(EntiteService);

  readonly statuts = STATUTS;
  readonly libelles = LIBELLES_STATUT_ECRITURE;

  readonly enCours = signal(true);
  readonly erreur = signal<string | null>(null);
  readonly page = signal<EcriturePageReadDto | null>(null);
  readonly query = signal('');
  readonly statutFiltre = signal<StatutEcriture | null>(null);
  readonly pageIndex = signal(0);
  readonly exercice = signal<ExerciceReadDto | null>(null);
  readonly devise = signal<string | undefined>(undefined);

  private minuteurDebounce: ReturnType<typeof setTimeout> | undefined;

  readonly ecritures = computed(() => this.page()?.content ?? []);
  readonly pageAffichee = computed(() => (this.page()?.page ?? 0) + 1);
  readonly totalPages = computed(() => this.page()?.totalPages ?? 0);
  readonly totalElements = computed(() => this.page()?.totalElements ?? 0);
  readonly estPremierePage = computed(() => this.page()?.premierePage ?? true);
  readonly estDernierePage = computed(() => this.page()?.dernierePage ?? true);

  constructor() {
    this.entiteService.obtenirMonEntite().subscribe({ next: (e) => this.devise.set(e.devise) });
    this.exerciceService.obtenirOuvert().subscribe({ next: (e) => this.exercice.set(e) });
    this.chargerEcritures();
  }

  protected formaterMontant(montant: number): string {
    return formaterMontant(montant, this.devise());
  }

  onSaisieRecherche(valeur: string): void {
    this.query.set(valeur);
    clearTimeout(this.minuteurDebounce);
    this.minuteurDebounce = setTimeout(() => {
      this.pageIndex.set(0);
      this.chargerEcritures();
    }, DELAI_DEBOUNCE_MS);
  }

  filtrerParStatut(statut: StatutEcriture | null): void {
    this.statutFiltre.set(statut);
    this.pageIndex.set(0);
    this.chargerEcritures();
  }

  pagePrecedente(): void {
    if (this.estPremierePage()) return;
    this.pageIndex.update((i) => i - 1);
    this.chargerEcritures();
  }

  pageSuivante(): void {
    if (this.estDernierePage()) return;
    this.pageIndex.update((i) => i + 1);
    this.chargerEcritures();
  }

  private chargerEcritures(): void {
    this.enCours.set(true);
    this.ecritureService
      .rechercher({
        q: this.query() || undefined,
        statuts: this.statutFiltre() ? [this.statutFiltre()!] : undefined,
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
