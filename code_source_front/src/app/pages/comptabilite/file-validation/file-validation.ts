import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';

import { EcritureService } from '../../../core/ecriture.service';
import { formaterMontant } from '../../../core/devise.util';
import { EntiteService } from '../../../core/entite.service';
import { ExerciceService } from '../../../core/exercice.service';
import { extraireMessageErreur } from '../../../core/http-error.util';
import { EcritureReadDto, ExerciceReadDto } from '../../../core/models';
import { Alert } from '../../../shared/ui/alert/alert';
import { StatusBadge } from '../../../shared/ui/status-badge/status-badge';

const DELAI_FLASH_MS = 900;
const DELAI_TOAST_SUCCES_MS = 1800;
const DELAI_TOAST_ERREUR_MS = 4000;

interface Toast {
  message: string;
  tone: 'success' | 'error';
}

@Component({
  selector: 'app-file-validation',
  imports: [RouterLink, Alert, StatusBadge, DatePipe],
  templateUrl: './file-validation.html',
  styleUrl: './file-validation.scss',
})
export class FileValidation {
  private readonly ecritureService = inject(EcritureService);
  private readonly exerciceService = inject(ExerciceService);
  private readonly entiteService = inject(EntiteService);

  readonly enCours = signal(true);
  readonly erreur = signal<string | null>(null);
  readonly ecritures = signal<EcritureReadDto[]>([]);
  readonly exercice = signal<ExerciceReadDto | null>(null);
  readonly valideesAujourdhui = signal(0);
  readonly devise = signal<string | undefined>(undefined);

  readonly enCoursValidation = signal<string | null>(null);
  readonly flashId = signal<string | null>(null);
  readonly toast = signal<Toast | null>(null);
  readonly ecriturePourConfirmation = signal<EcritureReadDto | null>(null);

  private timerFlash: ReturnType<typeof setTimeout> | undefined;
  private timerToast: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    this.entiteService.obtenirMonEntite().subscribe({ next: (e) => this.devise.set(e.devise) });
    this.exerciceService.obtenirOuvert().subscribe({
      next: (exercice) => {
        this.exercice.set(exercice);
        this.chargerFile(exercice.id);
      },
      error: (erreur: unknown) => {
        this.enCours.set(false);
        if (erreur instanceof HttpErrorResponse && erreur.status === 404) {
          this.erreur.set('Aucun exercice ouvert — rien à valider pour le moment.');
        } else {
          this.erreur.set(extraireMessageErreur(erreur));
        }
      },
    });
  }

  private chargerFile(exerciceId: string): void {
    this.enCours.set(true);
    this.ecritureService
      .rechercher({ exerciceId, statuts: ['EN_ATTENTE'], sort: 'date,asc', size: 100 })
      .subscribe({
        next: (page) => {
          this.ecritures.set(page.content);
          this.enCours.set(false);
        },
        error: (erreur) => {
          this.erreur.set(extraireMessageErreur(erreur));
          this.enCours.set(false);
        },
      });
  }

  demanderValidation(ecriture: EcritureReadDto): void {
    this.ecriturePourConfirmation.set(ecriture);
  }

  annulerValidation(): void {
    this.ecriturePourConfirmation.set(null);
  }

  confirmerValidation(): void {
    const ecriture = this.ecriturePourConfirmation();
    if (!ecriture) return;
    this.ecriturePourConfirmation.set(null);
    this.enCoursValidation.set(ecriture.id);
    this.ecritureService.valider(ecriture.id).subscribe({
      next: () => {
        this.enCoursValidation.set(null);
        this.valideesAujourdhui.update((n) => n + 1);
        this.afficherToast({ message: `✓ Écriture « ${ecriture.libelle} » validée.`, tone: 'success' });
        this.declencherFlashPuisRetrait(ecriture.id);
      },
      error: (erreur) => {
        this.enCoursValidation.set(null);
        this.afficherToast({ message: extraireMessageErreur(erreur), tone: 'error' });
      },
    });
  }

  private declencherFlashPuisRetrait(id: string): void {
    this.flashId.set(id);
    clearTimeout(this.timerFlash);
    this.timerFlash = setTimeout(() => {
      this.ecritures.update((liste) => liste.filter((e) => e.id !== id));
      this.flashId.set(null);
    }, DELAI_FLASH_MS);
  }

  private afficherToast(toast: Toast): void {
    this.toast.set(toast);
    clearTimeout(this.timerToast);
    const delai = toast.tone === 'success' ? DELAI_TOAST_SUCCES_MS : DELAI_TOAST_ERREUR_MS;
    this.timerToast = setTimeout(() => this.toast.set(null), delai);
  }

  protected formaterMontant(montant: number): string {
    return formaterMontant(montant, this.devise());
  }
}
