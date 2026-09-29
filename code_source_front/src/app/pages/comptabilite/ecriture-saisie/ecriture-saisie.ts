import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { CompteComptableCreationService } from '../../../core/compte-comptable-creation.service';
import { formaterMontant } from '../../../core/devise.util';
import { EcritureService } from '../../../core/ecriture.service';
import { EntiteService } from '../../../core/entite.service';
import { ExerciceService } from '../../../core/exercice.service';
import { extraireMessageErreur } from '../../../core/http-error.util';
import {
  ClasseCompteComptableReadDto,
  CompteOptionReadDto,
  EcritureCreationForm,
  ExerciceReadDto,
  LigneEcritureForm,
  SensCompte,
} from '../../../core/models';
import { PlanComptableService } from '../../../core/plan-comptable.service';
import { Alert } from '../../../shared/ui/alert/alert';
import { DatePicker } from '../../../shared/ui/date-picker/date-picker';

let prochainIdLigne = 1;

interface LigneRow {
  id: number;
  classeId: string;
  comptes: CompteOptionReadDto[];
  chargementComptes: boolean;
  compteId: string;
  compteNumero: string;
  compteLibelle: string;
  sens: SensCompte;
  montant: number | null;
  libelle: string;
}

function ligneVide(): LigneRow {
  return {
    id: prochainIdLigne++,
    classeId: '',
    comptes: [],
    chargementComptes: false,
    compteId: '',
    compteNumero: '',
    compteLibelle: '',
    sens: 'DEBIT',
    montant: null,
    libelle: '',
  };
}

@Component({
  selector: 'app-ecriture-saisie',
  imports: [RouterLink, Alert, DatePipe, DatePicker],
  templateUrl: './ecriture-saisie.html',
  styleUrl: './ecriture-saisie.scss',
})
export class EcritureSaisie {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ecritureService = inject(EcritureService);
  private readonly exerciceService = inject(ExerciceService);
  private readonly planComptableService = inject(PlanComptableService);
  private readonly compteComptableCreationService = inject(CompteComptableCreationService);
  private readonly entiteService = inject(EntiteService);

  private readonly ecritureId = this.route.snapshot.paramMap.get('id');
  readonly modeEdition = computed(() => this.ecritureId !== null);

  readonly enCoursChargement = signal(true);
  readonly erreur = signal<string | null>(null);
  readonly classes = signal<ClasseCompteComptableReadDto[]>([]);
  readonly exerciceOuvert = signal<ExerciceReadDto | null>(null);
  readonly devise = signal<string | undefined>(undefined);

  readonly date = signal('');
  readonly reference = signal('');
  readonly libelle = signal('');
  readonly lignes = signal<LigneRow[]>([ligneVide(), ligneVide()]);

  readonly enCoursEnregistrement = signal(false);
  readonly erreurEnregistrement = signal<string | null>(null);
  readonly confirmationSoumission = signal(false);

  // Modale "Choisir un compte" — ouverte pour une ligne à la fois.
  readonly pickerLigneId = signal<number | null>(null);
  readonly ligneEnCoursPicker = computed(() =>
    this.lignes().find((l) => l.id === this.pickerLigneId()) ?? null,
  );

  readonly totalDebit = computed(() =>
    this.lignes()
      .filter((l) => l.sens === 'DEBIT')
      .reduce((total, l) => total + (l.montant ?? 0), 0),
  );
  readonly totalCredit = computed(() =>
    this.lignes()
      .filter((l) => l.sens === 'CREDIT')
      .reduce((total, l) => total + (l.montant ?? 0), 0),
  );
  readonly ecart = computed(() => this.totalDebit() - this.totalCredit());
  readonly equilibree = computed(() => Math.abs(this.ecart()) < 0.005);

  constructor() {
    this.entiteService.obtenirMonEntite().subscribe({ next: (e) => this.devise.set(e.devise) });
    // Chaîné, jamais en parallèle (cf. piège documenté ailleurs sur le refresh Keycloak).
    this.planComptableService.listerClasses().subscribe({
      next: (classes) => {
        this.classes.set(classes);
        this.chargerSuite();
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCoursChargement.set(false);
      },
    });
  }

  private chargerSuite(): void {
    if (this.modeEdition()) {
      this.chargerEcritureExistante();
    } else {
      this.chargerExerciceOuvert();
    }
  }

  private chargerExerciceOuvert(): void {
    this.exerciceService.obtenirOuvert().subscribe({
      next: (exercice) => {
        this.exerciceOuvert.set(exercice);
        this.date.set(exercice.dateDebut);
        this.enCoursChargement.set(false);
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCoursChargement.set(false);
      },
    });
  }

  private chargerEcritureExistante(): void {
    const id = this.ecritureId;
    if (!id) return;
    this.ecritureService.obtenirDetail(id).subscribe({
      next: (ecriture) => {
        if (ecriture.statut !== 'BROUILLON') {
          this.erreur.set('Seule une écriture en brouillon peut être modifiée.');
          this.enCoursChargement.set(false);
          return;
        }
        this.date.set(ecriture.date);
        this.reference.set(ecriture.reference ?? '');
        this.libelle.set(ecriture.libelle);
        this.lignes.set(
          ecriture.lignes.map((l) => {
            const row = ligneVide();
            row.compteId = l.compteId;
            row.compteNumero = l.compteNumero;
            row.compteLibelle = l.compteLibelle;
            row.sens = l.sens;
            row.montant = l.montant;
            row.libelle = l.libelle ?? '';
            return row;
          }),
        );
        this.enCoursChargement.set(false);
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCoursChargement.set(false);
      },
    });
  }

  ajouterLigne(): void {
    this.lignes.update((lignes) => [...lignes, ligneVide()]);
  }

  retirerLigne(id: number): void {
    if (this.lignes().length <= 2) return;
    this.lignes.update((lignes) => lignes.filter((l) => l.id !== id));
  }

  ouvrirPicker(ligneId: number): void {
    this.pickerLigneId.set(ligneId);
  }

  fermerPicker(): void {
    this.pickerLigneId.set(null);
  }

  choisirClasseDansPicker(classeId: string): void {
    const ligneId = this.pickerLigneId();
    if (ligneId === null) return;
    this.choisirClasse(ligneId, classeId);
  }

  choisirCompteDansPicker(compte: CompteOptionReadDto): void {
    const ligneId = this.pickerLigneId();
    if (ligneId === null) return;
    this.choisirCompte(ligneId, compte.id);
    this.fermerPicker();
  }

  private choisirClasse(id: number, classeId: string): void {
    this.lignes.update((lignes) =>
      lignes.map((l) =>
        l.id === id
          ? { ...l, classeId, compteId: '', compteNumero: '', compteLibelle: '', comptes: [] }
          : l,
      ),
    );
    if (!classeId) return;
    this.lignes.update((lignes) =>
      lignes.map((l) => (l.id === id ? { ...l, chargementComptes: true } : l)),
    );
    this.compteComptableCreationService.listerComptesDeClasse(classeId).subscribe({
      next: (comptes) =>
        this.lignes.update((lignes) =>
          lignes.map((l) => (l.id === id ? { ...l, comptes, chargementComptes: false } : l)),
        ),
      error: () =>
        this.lignes.update((lignes) =>
          lignes.map((l) => (l.id === id ? { ...l, chargementComptes: false } : l)),
        ),
    });
  }

  private choisirCompte(id: number, compteId: string): void {
    this.lignes.update((lignes) =>
      lignes.map((l) => {
        if (l.id !== id) return l;
        const compte = l.comptes.find((c) => c.id === compteId);
        return {
          ...l,
          compteId,
          compteNumero: compte?.numero ?? '',
          compteLibelle: compte?.libelle ?? '',
        };
      }),
    );
  }

  choisirSens(id: number, sens: SensCompte): void {
    this.lignes.update((lignes) => lignes.map((l) => (l.id === id ? { ...l, sens } : l)));
  }

  changerMontant(id: number, valeur: string): void {
    const montant = valeur === '' ? null : Number(valeur);
    this.lignes.update((lignes) => lignes.map((l) => (l.id === id ? { ...l, montant } : l)));
  }

  changerLibelleLigne(id: number, libelle: string): void {
    this.lignes.update((lignes) => lignes.map((l) => (l.id === id ? { ...l, libelle } : l)));
  }

  protected formaterMontant(montant: number): string {
    return formaterMontant(montant, this.devise());
  }

  private construireForm(): EcritureCreationForm | null {
    if (!this.date() || !this.libelle().trim()) return null;
    const lignes: LigneEcritureForm[] = [];
    for (const l of this.lignes()) {
      if (!l.compteId || l.montant === null || l.montant <= 0) return null;
      lignes.push({
        compteId: l.compteId,
        sens: l.sens,
        montant: l.montant,
        libelle: l.libelle.trim() || undefined,
      });
    }
    if (lignes.length < 2) return null;
    return {
      date: this.date(),
      reference: this.reference().trim() || undefined,
      libelle: this.libelle().trim(),
      lignes,
    };
  }

  enregistrerBrouillon(): void {
    const form = this.construireForm();
    if (!form) {
      this.erreurEnregistrement.set(
        'Complète le libellé et toutes les lignes (compte, sens, montant).',
      );
      return;
    }
    this.enCoursEnregistrement.set(true);
    this.erreurEnregistrement.set(null);
    const appel = this.modeEdition()
      ? this.ecritureService.modifier(this.ecritureId!, form)
      : this.ecritureService.creer(form);
    appel.subscribe({
      next: (ecriture) => {
        this.enCoursEnregistrement.set(false);
        this.router.navigate(['/comptabilite/ecritures', ecriture.id]);
      },
      error: (erreur) => {
        this.erreurEnregistrement.set(extraireMessageErreur(erreur));
        this.enCoursEnregistrement.set(false);
      },
    });
  }

  demanderSoumission(): void {
    if (!this.construireForm() || !this.equilibree()) return;
    this.confirmationSoumission.set(true);
  }

  annulerSoumission(): void {
    this.confirmationSoumission.set(false);
  }

  confirmerSoumission(): void {
    this.confirmationSoumission.set(false);
    const form = this.construireForm();
    if (!form || !this.equilibree()) return;
    this.enCoursEnregistrement.set(true);
    this.erreurEnregistrement.set(null);
    const appel = this.modeEdition()
      ? this.ecritureService.modifier(this.ecritureId!, form)
      : this.ecritureService.creer(form);
    appel.subscribe({
      next: (ecriture) =>
        this.ecritureService.soumettre(ecriture.id).subscribe({
          next: () => {
            this.enCoursEnregistrement.set(false);
            this.router.navigate(['/comptabilite/ecritures', ecriture.id]);
          },
          error: (erreur) => {
            this.erreurEnregistrement.set(extraireMessageErreur(erreur));
            this.enCoursEnregistrement.set(false);
          },
        }),
      error: (erreur) => {
        this.erreurEnregistrement.set(extraireMessageErreur(erreur));
        this.enCoursEnregistrement.set(false);
      },
    });
  }
}
