import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { EntiteService } from '../../core/entite.service';
import { ReferentielService } from '../../core/referentiel.service';
import { extraireMessageErreur } from '../../core/http-error.util';
import { EntiteCreeeReadDto, ReferentielComptableReadDto, TypeEntite } from '../../core/models';
import { Alert } from '../../shared/ui/alert/alert';
import { Stepper } from '../../shared/ui/stepper/stepper';
import { SuccessCard } from '../../shared/ui/success-card/success-card';

const ETAPE_LABELS = ['Entreprise', 'Référentiel', 'Administrateur'];

const CHAMPS_PAR_ETAPE: Record<number, string[]> = {
  1: ['raisonSociale', 'typeEntite', 'pays', 'numeroIdentification'],
  2: ['referentielComptableCode', 'devise'],
  3: ['adminNom', 'adminPostNom', 'adminPrenom', 'adminEmail', 'adminTelephone']
};

@Component({
  selector: 'app-create-entreprise',
  imports: [ReactiveFormsModule, RouterLink, Alert, Stepper, SuccessCard],
  templateUrl: './create-entreprise.html'
})
export class CreateEntreprise {
  private readonly fb = inject(FormBuilder);
  private readonly entiteService = inject(EntiteService);
  private readonly referentielService = inject(ReferentielService);

  readonly etapeLabels = ETAPE_LABELS;
  readonly typesEntite: TypeEntite[] = ['PME', 'ONG', 'ASSOCIATION', 'ECOLE', 'AUTRE'];
  readonly referentiels = signal<ReferentielComptableReadDto[]>([]);
  readonly enCours = signal(false);
  readonly erreur = signal<string | null>(null);
  readonly resultat = signal<EntiteCreeeReadDto | null>(null);
  readonly etapeCourante = signal(1);

  readonly form = this.fb.nonNullable.group({
    raisonSociale: ['', Validators.required],
    typeEntite: ['PME' as TypeEntite, Validators.required],
    pays: [''],
    devise: [''],
    numeroIdentification: [''],
    referentielComptableCode: ['', Validators.required],
    adminNom: ['', Validators.required],
    adminPostNom: [''],
    adminPrenom: ['', Validators.required],
    adminEmail: ['', [Validators.required, Validators.email]],
    adminTelephone: ['']
  });

  constructor() {
    this.referentielService.lister().subscribe({
      next: (referentiels) => this.referentiels.set(referentiels),
      error: (erreur) => this.erreur.set(extraireMessageErreur(erreur))
    });
  }

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

    const valeurs = this.form.getRawValue();
    this.entiteService
      .creerEntite({
        ...valeurs,
        referentielComptableCode: valeurs.referentielComptableCode as EntiteCreeeReadDto['entite']['referentielComptableCode']
      })
      .subscribe({
        next: (resultat) => {
          this.resultat.set(resultat);
          this.enCours.set(false);
        },
        error: (erreur) => {
          this.erreur.set(extraireMessageErreur(erreur));
          this.enCours.set(false);
        }
      });
  }
}
