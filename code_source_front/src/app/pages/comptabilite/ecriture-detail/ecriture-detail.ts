import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { CurrentUserService } from '../../../core/current-user.service';
import { formaterMontant } from '../../../core/devise.util';
import { EcritureService } from '../../../core/ecriture.service';
import { EntiteService } from '../../../core/entite.service';
import { ExerciceService } from '../../../core/exercice.service';
import { extraireMessageErreur } from '../../../core/http-error.util';
import { EcritureReadDto, ExerciceReadDto } from '../../../core/models';
import { Alert } from '../../../shared/ui/alert/alert';
import { DatePicker } from '../../../shared/ui/date-picker/date-picker';
import { EcritureStatutBadge } from '../../../shared/ui/ecriture-statut-badge/ecriture-statut-badge';

const ROLES_VALIDATION = ['ADMIN', 'ADMIN_FINANCIER'];
const ROLES_SAISIE = ['ADMIN', 'ADMIN_FINANCIER', 'COMPTABLE'];

@Component({
  selector: 'app-ecriture-detail',
  imports: [RouterLink, Alert, EcritureStatutBadge, DatePipe, DatePicker],
  templateUrl: './ecriture-detail.html',
  styleUrl: './ecriture-detail.scss',
})
export class EcritureDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ecritureService = inject(EcritureService);
  private readonly currentUserService = inject(CurrentUserService);
  private readonly entiteService = inject(EntiteService);
  private readonly exerciceService = inject(ExerciceService);

  private readonly ecritureId = this.route.snapshot.paramMap.get('id')!;

  readonly enCours = signal(true);
  readonly erreur = signal<string | null>(null);
  readonly ecriture = signal<EcritureReadDto | null>(null);
  readonly devise = signal<string | undefined>(undefined);

  readonly enCoursAction = signal(false);
  readonly erreurAction = signal<string | null>(null);
  readonly confirmationSuppression = signal(false);
  readonly confirmationValidation = signal(false);
  readonly confirmationContrePassation = signal(false);
  readonly dateContrePassation = signal('');
  readonly exerciceOuvert = signal<ExerciceReadDto | null>(null);
  readonly modaleRenvoiOuverte = signal(false);
  readonly motifRenvoi = signal('');

  private readonly role = computed(() => this.currentUserService.utilisateur()?.role ?? '');

  readonly peutValider = computed(
    () => ROLES_VALIDATION.includes(this.role()) && this.ecriture()?.statut === 'EN_ATTENTE',
  );
  readonly peutRenvoyer = this.peutValider;
  readonly peutContrePasser = computed(
    () => ROLES_VALIDATION.includes(this.role()) && this.ecriture()?.statut === 'VALIDEE',
  );
  // Pas de restriction au créateur : le back autorise tout rôle de saisie de l'entreprise à
  // modifier/supprimer un brouillon, pas seulement son auteur (décision confirmée par le user).
  readonly peutModifier = computed(() => {
    const e = this.ecriture();
    return !!e && e.statut === 'BROUILLON' && ROLES_SAISIE.includes(this.role());
  });
  readonly peutSupprimer = this.peutModifier;

  constructor() {
    this.entiteService.obtenirMonEntite().subscribe({ next: (e) => this.devise.set(e.devise) });
    this.exerciceService.obtenirOuvert().subscribe({
      next: (exercice) => this.exerciceOuvert.set(exercice),
      error: () => this.exerciceOuvert.set(null),
    });
    this.charger();
  }

  protected formaterMontant(montant: number): string {
    return formaterMontant(montant, this.devise());
  }

  private charger(): void {
    this.enCours.set(true);
    this.ecritureService.obtenirDetail(this.ecritureId).subscribe({
      next: (ecriture) => {
        this.ecriture.set(ecriture);
        this.enCours.set(false);
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCours.set(false);
      },
    });
  }

  modifier(): void {
    this.router.navigate(['/comptabilite/ecritures', this.ecritureId, 'modifier']);
  }

  demanderSuppression(): void {
    this.confirmationSuppression.set(true);
  }

  annulerSuppression(): void {
    this.confirmationSuppression.set(false);
  }

  confirmerSuppression(): void {
    this.enCoursAction.set(true);
    this.ecritureService.supprimer(this.ecritureId).subscribe({
      next: () => this.router.navigate(['/comptabilite/ecritures']),
      error: (erreur) => {
        this.erreurAction.set(extraireMessageErreur(erreur));
        this.enCoursAction.set(false);
        this.confirmationSuppression.set(false);
      },
    });
  }

  demanderValidation(): void {
    this.confirmationValidation.set(true);
  }

  annulerValidation(): void {
    this.confirmationValidation.set(false);
  }

  confirmerValidation(): void {
    this.confirmationValidation.set(false);
    this.enCoursAction.set(true);
    this.erreurAction.set(null);
    this.ecritureService.valider(this.ecritureId).subscribe({
      next: (ecriture) => {
        this.ecriture.set(ecriture);
        this.enCoursAction.set(false);
      },
      error: (erreur) => {
        this.erreurAction.set(extraireMessageErreur(erreur));
        this.enCoursAction.set(false);
      },
    });
  }

  ouvrirModaleRenvoi(): void {
    this.motifRenvoi.set('');
    this.modaleRenvoiOuverte.set(true);
  }

  fermerModaleRenvoi(): void {
    this.modaleRenvoiOuverte.set(false);
  }

  confirmerRenvoi(): void {
    this.enCoursAction.set(true);
    this.erreurAction.set(null);
    this.ecritureService
      .renvoyerEnBrouillon(this.ecritureId, { motif: this.motifRenvoi() || undefined })
      .subscribe({
        next: (ecriture) => {
          this.ecriture.set(ecriture);
          this.enCoursAction.set(false);
          this.modaleRenvoiOuverte.set(false);
        },
        error: (erreur) => {
          this.erreurAction.set(extraireMessageErreur(erreur));
          this.enCoursAction.set(false);
        },
      });
  }

  demanderContrePassation(): void {
    this.dateContrePassation.set(this.ecriture()?.date ?? '');
    this.confirmationContrePassation.set(true);
  }

  annulerContrePassation(): void {
    this.confirmationContrePassation.set(false);
  }

  confirmerContrePassation(): void {
    this.confirmationContrePassation.set(false);
    this.enCoursAction.set(true);
    this.erreurAction.set(null);
    this.ecritureService.contrePasser(this.ecritureId, { date: this.dateContrePassation() || undefined }).subscribe({
      next: (miroir) => {
        this.enCoursAction.set(false);
        this.router.navigate(['/comptabilite/ecritures', miroir.id]);
      },
      error: (erreur) => {
        this.erreurAction.set(extraireMessageErreur(erreur));
        this.enCoursAction.set(false);
      },
    });
  }
}
