import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';

import { CurrentUserService } from '../../../core/current-user.service';
import { formaterMontant } from '../../../core/devise.util';
import { EcritureService } from '../../../core/ecriture.service';
import { EntiteService } from '../../../core/entite.service';
import { ExerciceService } from '../../../core/exercice.service';
import { extraireMessageErreur } from '../../../core/http-error.util';
import { EcritureStatsReadDto, ExerciceReadDto, ResultatReadDto } from '../../../core/models';
import { Alert } from '../../../shared/ui/alert/alert';
import { StatusBadge } from '../../../shared/ui/status-badge/status-badge';

const ROLES_VALIDATION = ['ADMIN', 'ADMIN_FINANCIER'];

@Component({
  selector: 'app-comptabilite-tableau-bord',
  imports: [RouterLink, Alert, StatusBadge, DatePipe],
  templateUrl: './comptabilite-tableau-bord.html',
  styleUrl: './comptabilite-tableau-bord.scss',
})
export class ComptabiliteTableauBord {
  private readonly exerciceService = inject(ExerciceService);
  private readonly ecritureService = inject(EcritureService);
  private readonly currentUserService = inject(CurrentUserService);
  private readonly entiteService = inject(EntiteService);

  readonly enCours = signal(true);
  readonly erreur = signal<string | null>(null);
  readonly exercice = signal<ExerciceReadDto | null>(null);
  readonly stats = signal<EcritureStatsReadDto | null>(null);
  readonly resultat = signal<ResultatReadDto | null>(null);
  readonly devise = signal<string | undefined>(undefined);

  readonly peutValider = computed(() =>
    ROLES_VALIDATION.includes(this.currentUserService.utilisateur()?.role ?? ''),
  );

  constructor() {
    this.entiteService.obtenirMonEntite().subscribe({ next: (e) => this.devise.set(e.devise) });
    this.chargerExerciceOuvert();
  }

  protected formaterResultat(resultat: number | null | undefined): string {
    if (resultat === null || resultat === undefined) return '—';
    const signe = resultat > 0 ? '+' : '';
    return `${signe}${formaterMontant(resultat, this.devise())}`;
  }

  private chargerExerciceOuvert(): void {
    this.enCours.set(true);
    // Chaîné (jamais parallèle) : deux appels authentifiés simultanés peuvent chacun
    // déclencher un refresh du token Keycloak à usage unique et s'invalider l'un l'autre.
    this.exerciceService.obtenirOuvert().subscribe({
      next: (exercice) => {
        this.exercice.set(exercice);
        this.chargerStats(exercice.id);
      },
      error: (erreur: unknown) => {
        this.exercice.set(null);
        this.enCours.set(false);
        if (!(erreur instanceof HttpErrorResponse) || erreur.status !== 404) {
          this.erreur.set(extraireMessageErreur(erreur));
        }
      },
    });
  }

  private chargerStats(exerciceId: string): void {
    this.ecritureService.obtenirStats(exerciceId).subscribe({
      next: (stats) => {
        this.stats.set(stats);
        this.chargerResultat(exerciceId);
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCours.set(false);
      },
    });
  }

  private chargerResultat(exerciceId: string): void {
    this.exerciceService.obtenirResultat(exerciceId).subscribe({
      next: (resultat) => {
        this.resultat.set(resultat);
        this.enCours.set(false);
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCours.set(false);
      },
    });
  }
}
