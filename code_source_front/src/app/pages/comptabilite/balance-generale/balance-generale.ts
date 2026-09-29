import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ComptabiliteService } from '../../../core/comptabilite.service';
import { formaterMontant } from '../../../core/devise.util';
import { EntiteService } from '../../../core/entite.service';
import { ExerciceService } from '../../../core/exercice.service';
import { extraireMessageErreur } from '../../../core/http-error.util';
import { BalanceReadDto, ExerciceReadDto } from '../../../core/models';
import { Alert } from '../../../shared/ui/alert/alert';
import { SelecteurExercice } from '../../../shared/ui/selecteur-exercice/selecteur-exercice';

@Component({
  selector: 'app-balance-generale',
  imports: [RouterLink, Alert, SelecteurExercice],
  templateUrl: './balance-generale.html',
  styleUrl: './balance-generale.scss',
})
export class BalanceGenerale {
  private readonly exerciceService = inject(ExerciceService);
  private readonly comptabiliteService = inject(ComptabiliteService);
  private readonly entiteService = inject(EntiteService);

  readonly enCours = signal(true);
  readonly erreur = signal<string | null>(null);
  readonly exercices = signal<ExerciceReadDto[]>([]);
  readonly exerciceId = signal('');
  readonly balance = signal<BalanceReadDto | null>(null);
  readonly devise = signal<string | undefined>(undefined);

  readonly sommeSoldesDebiteurs = computed(() =>
    (this.balance()?.lignes ?? []).reduce((total, l) => total + l.soldeDebiteur, 0),
  );
  readonly sommeSoldesCrediteurs = computed(() =>
    (this.balance()?.lignes ?? []).reduce((total, l) => total + l.soldeCrediteur, 0),
  );
  readonly ecartAbsolu = computed(() =>
    Math.abs(this.sommeSoldesDebiteurs() - this.sommeSoldesCrediteurs()),
  );
  readonly equilibree = computed(() => this.ecartAbsolu() < 0.005);

  constructor() {
    this.entiteService.obtenirMonEntite().subscribe({ next: (e) => this.devise.set(e.devise) });
    this.exerciceService.lister().subscribe({
      next: (exercices) => {
        this.exercices.set(exercices);
        const ouvert = exercices.find((e) => e.statut === 'OUVERT');
        const id = ouvert?.id ?? exercices[0]?.id ?? '';
        this.exerciceId.set(id);
        if (id) {
          this.chargerBalance(id);
        } else {
          this.enCours.set(false);
        }
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCours.set(false);
      },
    });
  }

  changerExercice(id: string): void {
    this.exerciceId.set(id);
    this.chargerBalance(id);
  }

  private chargerBalance(exerciceId: string): void {
    this.enCours.set(true);
    this.comptabiliteService.obtenirBalance(exerciceId).subscribe({
      next: (balance) => {
        this.balance.set(balance);
        this.enCours.set(false);
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCours.set(false);
      },
    });
  }

  protected formaterMontant(montant: number): string {
    return montant ? formaterMontant(montant, this.devise()) : '—';
  }
}
