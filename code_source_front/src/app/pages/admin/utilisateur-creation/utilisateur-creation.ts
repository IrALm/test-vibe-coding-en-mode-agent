import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Role, UtilisateurReadDto } from '../../../core/models';
import { UtilisateurService } from '../../../core/utilisateur.service';
import { extraireMessageErreur } from '../../../core/http-error.util';
import { Alert } from '../../../shared/ui/alert/alert';
import { Stepper } from '../../../shared/ui/stepper/stepper';
import { SuccessCard } from '../../../shared/ui/success-card/success-card';

const ETAPE_LABELS = ['Informations', 'Rôle', 'Confirmation'];

const CHAMPS_PAR_ETAPE: Record<number, string[]> = {
  1: ['nom', 'postNom', 'prenom', 'poste', 'email'],
  2: ['role'],
  3: []
};

@Component({
  selector: 'app-utilisateur-creation',
  imports: [ReactiveFormsModule, RouterLink, Alert, Stepper, SuccessCard],
  templateUrl: './utilisateur-creation.html'
})
export class UtilisateurCreation {
  private readonly fb = inject(FormBuilder);
  private readonly utilisateurService = inject(UtilisateurService);

  readonly etapeLabels = ETAPE_LABELS;
  readonly roles: Role[] = ['ADMIN', 'ADMIN_FINANCIER', 'COMPTABLE', 'RH', 'ACHATS', 'CAISSIER', 'LECTURE_SEULE'];
  readonly enCours = signal(false);
  readonly erreur = signal<string | null>(null);
  readonly resultat = signal<UtilisateurReadDto | null>(null);
  readonly etapeCourante = signal(1);

  readonly form = this.fb.nonNullable.group({
    nom: ['', Validators.required],
    postNom: [''],
    prenom: ['', Validators.required],
    poste: [''],
    email: ['', [Validators.required, Validators.email]],
    role: ['COMPTABLE' as Role, Validators.required]
  });

  etapePrecedente(): void {
    this.etapeCourante.update((e) => Math.max(1, e - 1));
  }

  etapeSuivante(): void {
    if (!this.etapeValide(this.etapeCourante())) {
      this.marquerEtapeTouchee(this.etapeCourante());
      return;
    }
    if (this.etapeCourante() < this.etapeLabels.length) {
      this.etapeCourante.update((e) => e + 1);
    } else {
      this.soumettre();
    }
  }

  private etapeValide(etape: number): boolean {
    return CHAMPS_PAR_ETAPE[etape].every((champ) => this.form.controls[champ as keyof typeof this.form.controls].valid);
  }

  private marquerEtapeTouchee(etape: number): void {
    CHAMPS_PAR_ETAPE[etape].forEach((champ) =>
      this.form.controls[champ as keyof typeof this.form.controls].markAsTouched()
    );
  }

  soumettre(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enCours.set(true);
    this.erreur.set(null);
    this.resultat.set(null);

    this.utilisateurService.ajouterUtilisateur(this.form.getRawValue()).subscribe({
      next: (utilisateur) => {
        this.resultat.set(utilisateur);
        this.enCours.set(false);
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCours.set(false);
      }
    });
  }

  ajouterAutre(): void {
    this.resultat.set(null);
    this.form.reset({ role: 'COMPTABLE' as Role });
    this.etapeCourante.set(1);
  }
}
