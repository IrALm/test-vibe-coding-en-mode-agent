import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, output, signal } from '@angular/core';

import { extraireMessageErreur } from '../../../core/http-error.util';
import { ClasseCompteComptableReadDto, CompteComptableReadDto, TiersReadDto } from '../../../core/models';
import { PlanComptableService } from '../../../core/plan-comptable.service';
import { TiersService } from '../../../core/tiers.service';
import { Alert } from '../../../shared/ui/alert/alert';

const DELAI_DEBOUNCE_MS = 200;
const TAILLE_PAGE_COMPTES = 8;

@Component({
  selector: 'app-tiers-association-modal',
  imports: [Alert],
  templateUrl: './tiers-association-modal.html',
  styleUrl: './tiers-association-modal.scss',
})
export class TiersAssociationModal {
  private readonly planComptableService = inject(PlanComptableService);
  private readonly tiersService = inject(TiersService);

  readonly tiers = input.required<TiersReadDto>();
  readonly ferme = output<void>();
  readonly associe = output<TiersReadDto>();

  readonly classes = signal<ClasseCompteComptableReadDto[]>([]);
  readonly queryClasses = signal('');
  private minuteurDebounceClasses?: ReturnType<typeof setTimeout>;

  readonly classeSelectionneeId = signal<string | null>(null);
  readonly classeSelectionnee = computed(
    () => this.classes().find((c) => c.id === this.classeSelectionneeId()) ?? null,
  );

  readonly comptes = signal<CompteComptableReadDto[]>([]);
  readonly queryComptes = signal('');
  private minuteurDebounceComptes?: ReturnType<typeof setTimeout>;
  readonly enCoursComptes = signal(false);

  readonly compteSelectionneId = signal<string | null>(null);
  readonly compteSelectionne = computed(
    () => this.comptes().find((c) => c.id === this.compteSelectionneId()) ?? null,
  );

  readonly enCoursAssociation = signal(false);
  readonly erreur = signal<string | null>(null);

  constructor() {
    this.chargerClasses();
  }

  onSaisieRechercheClasses(valeur: string): void {
    this.queryClasses.set(valeur);
    clearTimeout(this.minuteurDebounceClasses);
    this.minuteurDebounceClasses = setTimeout(() => this.chargerClasses(valeur), DELAI_DEBOUNCE_MS);
  }

  selectionnerClasse(classe: ClasseCompteComptableReadDto): void {
    if (this.classeSelectionneeId() === classe.id) return;
    this.classeSelectionneeId.set(classe.id);
    this.compteSelectionneId.set(null);
    this.queryComptes.set('');
    this.chargerComptes();
  }

  onSaisieRechercheComptes(valeur: string): void {
    this.queryComptes.set(valeur);
    clearTimeout(this.minuteurDebounceComptes);
    this.minuteurDebounceComptes = setTimeout(() => this.chargerComptes(), DELAI_DEBOUNCE_MS);
  }

  selectionnerCompte(compteId: string): void {
    this.compteSelectionneId.set(compteId);
  }

  associerCompte(): void {
    const compteId = this.compteSelectionneId();
    if (!compteId) return;

    this.enCoursAssociation.set(true);
    this.erreur.set(null);
    this.tiersService.associerCompte(this.tiers().id, compteId).subscribe({
      next: (tiersMisAJour) => {
        this.enCoursAssociation.set(false);
        this.associe.emit(tiersMisAJour);
      },
      error: (erreur: HttpErrorResponse) => {
        this.enCoursAssociation.set(false);
        this.erreur.set(extraireMessageErreur(erreur));
      },
    });
  }

  fermer(): void {
    this.ferme.emit();
  }

  private chargerClasses(q?: string): void {
    this.planComptableService.listerClasses(q).subscribe({
      next: (classes) => this.classes.set(classes),
    });
  }

  private chargerComptes(): void {
    const classeId = this.classeSelectionneeId();
    if (!classeId) return;

    this.enCoursComptes.set(true);
    this.planComptableService
      .rechercherComptes(classeId, {
        q: this.queryComptes().trim() || undefined,
        size: TAILLE_PAGE_COMPTES,
      })
      .subscribe({
        next: (page) => {
          this.comptes.set(page.content);
          this.enCoursComptes.set(false);
        },
        error: () => this.enCoursComptes.set(false),
      });
  }
}
