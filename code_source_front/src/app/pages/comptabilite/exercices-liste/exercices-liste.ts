import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { CurrentUserService } from '../../../core/current-user.service';
import { EcritureService } from '../../../core/ecriture.service';
import { formaterMontant } from '../../../core/devise.util';
import { EntiteService } from '../../../core/entite.service';
import { ExerciceService } from '../../../core/exercice.service';
import { extraireMessageErreur } from '../../../core/http-error.util';
import { ClotureCheckReadDto, ExerciceCreationForm, ExerciceReadDto } from '../../../core/models';
import { Alert } from '../../../shared/ui/alert/alert';
import { DatePicker } from '../../../shared/ui/date-picker/date-picker';
import { StatusBadge } from '../../../shared/ui/status-badge/status-badge';

const ROLES_GESTION_EXERCICES = ['ADMIN'];

@Component({
  selector: 'app-exercices-liste',
  imports: [RouterLink, Alert, StatusBadge, DatePipe, DatePicker],
  templateUrl: './exercices-liste.html',
  styleUrl: './exercices-liste.scss',
})
export class ExercicesListe {
  private readonly exerciceService = inject(ExerciceService);
  private readonly ecritureService = inject(EcritureService);
  private readonly entiteService = inject(EntiteService);
  private readonly currentUserService = inject(CurrentUserService);

  readonly enCours = signal(true);
  readonly erreur = signal<string | null>(null);
  readonly exercices = signal<ExerciceReadDto[]>([]);
  readonly devise = signal<string | undefined>(undefined);
  readonly resultatsParExercice = signal<Record<string, number>>({});

  readonly peutGererExercices = computed(() =>
    ROLES_GESTION_EXERCICES.includes(this.currentUserService.utilisateur()?.role ?? ''),
  );
  readonly unExerciceOuvertExiste = computed(() =>
    this.exercices().some((e) => e.statut === 'OUVERT'),
  );

  readonly modaleCreationOuverte = signal(false);
  readonly dateDebut = signal('');
  readonly dateFin = signal('');
  readonly enCoursCreation = signal(false);
  readonly erreurCreation = signal<string | null>(null);

  readonly exercicePourCloture = signal<ExerciceReadDto | null>(null);
  readonly clotureCheck = signal<ClotureCheckReadDto | null>(null);
  readonly nombreValideesCloture = signal<number | null>(null);
  readonly resultatCloture = signal<number | null>(null);
  readonly enCoursVerificationCloture = signal(false);
  readonly enCoursCloture = signal(false);
  readonly erreurCloture = signal<string | null>(null);

  constructor() {
    this.entiteService.obtenirMonEntite().subscribe({ next: (e) => this.devise.set(e.devise) });
    this.chargerExercices();
  }

  private chargerExercices(): void {
    this.enCours.set(true);
    this.exerciceService.lister().subscribe({
      next: (exercices) => {
        this.exercices.set(exercices);
        this.enCours.set(false);
        this.chargerResultats(exercices);
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCours.set(false);
      },
    });
  }

  private chargerResultats(exercices: ExerciceReadDto[]): void {
    forkJoin(
      exercices.map((e) =>
        this.exerciceService.obtenirResultat(e.id).pipe(catchError(() => of(null))),
      ),
    ).subscribe((resultats) => {
      const map: Record<string, number> = {};
      resultats.forEach((r) => {
        if (r) map[r.exerciceId] = r.resultat;
      });
      this.resultatsParExercice.set(map);
    });
  }

  ouvrirModaleCreation(): void {
    this.dateDebut.set('');
    this.dateFin.set('');
    this.erreurCreation.set(null);
    this.modaleCreationOuverte.set(true);
  }

  fermerModaleCreation(): void {
    this.modaleCreationOuverte.set(false);
  }

  creerExercice(): void {
    if (!this.dateDebut() || !this.dateFin()) {
      this.erreurCreation.set('Les deux dates sont requises.');
      return;
    }
    this.enCoursCreation.set(true);
    this.erreurCreation.set(null);
    const form: ExerciceCreationForm = { dateDebut: this.dateDebut(), dateFin: this.dateFin() };
    this.exerciceService.creer(form).subscribe({
      next: () => {
        this.enCoursCreation.set(false);
        this.modaleCreationOuverte.set(false);
        this.chargerExercices();
      },
      error: (erreur) => {
        this.erreurCreation.set(extraireMessageErreur(erreur));
        this.enCoursCreation.set(false);
      },
    });
  }

  demanderCloture(exercice: ExerciceReadDto): void {
    this.exercicePourCloture.set(exercice);
    this.clotureCheck.set(null);
    this.nombreValideesCloture.set(null);
    this.resultatCloture.set(null);
    this.erreurCloture.set(null);
    this.enCoursVerificationCloture.set(true);
    forkJoin({
      check: this.exerciceService.verifierCloture(exercice.id),
      validees: this.ecritureService.rechercher({
        exerciceId: exercice.id,
        statuts: ['VALIDEE'],
        size: 1,
      }),
      resultat: this.exerciceService.obtenirResultat(exercice.id),
    }).subscribe({
      next: ({ check, validees, resultat }) => {
        this.clotureCheck.set(check);
        this.nombreValideesCloture.set(validees.totalElements);
        this.resultatCloture.set(resultat.resultat);
        this.enCoursVerificationCloture.set(false);
      },
      error: (erreur) => {
        this.erreurCloture.set(extraireMessageErreur(erreur));
        this.enCoursVerificationCloture.set(false);
      },
    });
  }

  annulerCloture(): void {
    this.exercicePourCloture.set(null);
  }

  protected formaterResultat(resultat: number | null | undefined): string {
    if (resultat === null || resultat === undefined) return '—';
    const signe = resultat > 0 ? '+' : '';
    return `${signe}${formaterMontant(resultat, this.devise())} ${this.devise() ?? ''}`.trim();
  }

  confirmerCloture(): void {
    const exercice = this.exercicePourCloture();
    if (!exercice) return;
    this.enCoursCloture.set(true);
    this.exerciceService.cloturer(exercice.id).subscribe({
      next: () => {
        this.enCoursCloture.set(false);
        this.exercicePourCloture.set(null);
        this.chargerExercices();
      },
      error: (erreur) => {
        this.erreurCloture.set(extraireMessageErreur(erreur));
        this.enCoursCloture.set(false);
      },
    });
  }
}
