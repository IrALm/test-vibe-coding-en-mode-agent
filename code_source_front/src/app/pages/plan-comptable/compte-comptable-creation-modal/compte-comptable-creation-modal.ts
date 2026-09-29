import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, output, signal } from '@angular/core';

import { CompteComptableCreationService } from '../../../core/compte-comptable-creation.service';
import { extraireMessageErreur } from '../../../core/http-error.util';
import {
  ClasseCompteComptableReadDto,
  CompteOptionReadDto,
  SensCompte,
} from '../../../core/models';
import { PlanComptableService } from '../../../core/plan-comptable.service';
import { Alert } from '../../../shared/ui/alert/alert';
import { StatusBadge } from '../../../shared/ui/status-badge/status-badge';

const DELAI_DEBOUNCE_MS = 200;

type Etape = 1 | 2 | 3;

@Component({
  selector: 'app-compte-comptable-creation-modal',
  imports: [Alert, StatusBadge],
  templateUrl: './compte-comptable-creation-modal.html',
  styleUrl: './compte-comptable-creation-modal.scss',
})
export class CompteComptableCreationModal {
  private readonly planComptableService = inject(PlanComptableService);
  private readonly compteComptableCreationService = inject(CompteComptableCreationService);

  readonly ferme = output<void>();
  readonly compteCree = output<void>();

  readonly referentielLibelle = signal('');
  readonly classes = signal<ClasseCompteComptableReadDto[]>([]);

  readonly etape = signal<Etape>(1);
  readonly confirmationFermeture = signal(false);
  readonly succes = signal(false);
  readonly enCoursCreation = signal(false);
  readonly erreurGenerale = signal<string | null>(null);
  readonly erreurNumero = signal<string | null>(null);

  // Étape 1 — Rattachement
  readonly classeCompteComptableId = signal('');
  readonly parentId = signal('');
  readonly numero = signal('');
  readonly comptesDeClasse = signal<CompteOptionReadDto[]>([]);
  readonly chargementComptesParent = signal(false);
  readonly numeroExisteDeja = signal(false);
  readonly verificationNumeroEnCours = signal(false);
  private minuteurDebounceNumero?: ReturnType<typeof setTimeout>;

  // Étape 2 — Définition
  readonly libelle = signal('');
  readonly sensNormal = signal<SensCompte | ''>('');

  // Étape 3 — Options
  readonly lettrable = signal(false);
  readonly actif = signal(true);

  readonly classeSelectionnee = computed(
    () => this.classes().find((c) => c.id === this.classeCompteComptableId()) ?? null,
  );
  readonly parentSelectionne = computed(
    () => this.comptesDeClasse().find((c) => c.id === this.parentId()) ?? null,
  );

  /** Un sous-compte hérite du sens de son parent : l'utilisateur ne peut plus le modifier. */
  readonly sensVerrouille = computed(() => !!this.parentSelectionne());

  readonly numeroValideFormat = computed(() => /^\d{1,10}$/.test(this.numero()));

  readonly prefixeAttendu = computed(() => {
    const parent = this.parentSelectionne();
    if (parent) return parent.numero;
    const classe = this.classeSelectionnee();
    return classe ? String(classe.numero) : '';
  });

  readonly avertissementPrefixe = computed(() => {
    const prefixe = this.prefixeAttendu();
    return !!prefixe && this.numeroValideFormat() && !this.numero().startsWith(prefixe);
  });

  readonly etape1Valide = computed(
    () =>
      !!this.classeCompteComptableId() &&
      this.numeroValideFormat() &&
      !this.numeroExisteDeja() &&
      !this.verificationNumeroEnCours(),
  );
  readonly etape2Valide = computed(() => !!this.libelle().trim() && !!this.sensNormal());
  readonly etapeValide = computed(() => {
    switch (this.etape()) {
      case 1:
        return this.etape1Valide();
      case 2:
        return this.etape2Valide();
      default:
        return true;
    }
  });

  readonly formModifie = computed(
    () =>
      !!this.classeCompteComptableId() ||
      !!this.numero() ||
      !!this.libelle().trim() ||
      !!this.sensNormal() ||
      this.lettrable() ||
      !this.actif(),
  );

  constructor() {
    this.planComptableService.obtenirRecap().subscribe({
      next: (recap) => this.referentielLibelle.set(recap.referentielComptableLibelle),
    });
    this.planComptableService.listerClasses().subscribe({
      next: (classes) => this.classes.set(classes),
    });
  }

  choisirClasse(classeId: string): void {
    this.classeCompteComptableId.set(classeId);
    this.parentId.set('');
    this.numero.set('');
    this.numeroExisteDeja.set(false);
    this.comptesDeClasse.set([]);
    if (!classeId) return;

    this.chargementComptesParent.set(true);
    this.compteComptableCreationService.listerComptesDeClasse(classeId).subscribe({
      next: (comptes) => {
        this.comptesDeClasse.set(comptes);
        this.chargementComptesParent.set(false);
      },
      error: () => this.chargementComptesParent.set(false),
    });
  }

  choisirParent(parentId: string): void {
    this.parentId.set(parentId);
    const parent = this.comptesDeClasse().find((c) => c.id === parentId);
    if (parent) {
      this.onSaisieNumero(parent.numero);
      this.sensNormal.set(parent.sensNormal);
    } else {
      this.sensNormal.set('');
    }
  }

  onSaisieNumero(valeur: string): void {
    this.numero.set(valeur);
    this.erreurNumero.set(null);
    this.numeroExisteDeja.set(false);
    clearTimeout(this.minuteurDebounceNumero);
    if (!/^\d{1,10}$/.test(valeur)) return;

    this.minuteurDebounceNumero = setTimeout(() => {
      this.verificationNumeroEnCours.set(true);
      this.compteComptableCreationService.numeroExiste(valeur).subscribe({
        next: (resultat) => {
          this.numeroExisteDeja.set(resultat.exists);
          this.verificationNumeroEnCours.set(false);
        },
        error: () => this.verificationNumeroEnCours.set(false),
      });
    }, DELAI_DEBOUNCE_MS);
  }

  choisirSens(sens: SensCompte): void {
    if (this.sensVerrouille()) return;
    this.sensNormal.set(sens);
  }

  toggleLettrable(): void {
    this.lettrable.update((v) => !v);
  }

  toggleActif(): void {
    this.actif.update((v) => !v);
  }

  suivant(): void {
    if (!this.etapeValide()) return;
    this.etape.update((e) => (e < 3 ? ((e + 1) as Etape) : e));
  }

  precedent(): void {
    this.etape.update((e) => (e > 1 ? ((e - 1) as Etape) : e));
  }

  creerCompte(): void {
    const sens = this.sensNormal();
    if (!this.etapeValide() || !sens) return;

    this.enCoursCreation.set(true);
    this.erreurGenerale.set(null);
    this.erreurNumero.set(null);

    this.compteComptableCreationService
      .creerCompte({
        numero: this.numero(),
        libelle: this.libelle().trim(),
        classeCompteComptableId: this.classeCompteComptableId(),
        parentId: this.parentId() || undefined,
        sensNormal: sens,
        lettrable: this.lettrable(),
        actif: this.actif(),
      })
      .subscribe({
        next: () => {
          this.enCoursCreation.set(false);
          this.succes.set(true);
        },
        error: (erreur: unknown) => {
          this.enCoursCreation.set(false);
          if (erreur instanceof HttpErrorResponse && erreur.status === 409) {
            this.erreurNumero.set(extraireMessageErreur(erreur));
            this.etape.set(1);
          } else {
            this.erreurGenerale.set(extraireMessageErreur(erreur));
          }
        },
      });
  }

  creerUnAutre(): void {
    this.etape.set(1);
    this.succes.set(false);
    this.classeCompteComptableId.set('');
    this.parentId.set('');
    this.numero.set('');
    this.comptesDeClasse.set([]);
    this.numeroExisteDeja.set(false);
    this.libelle.set('');
    this.sensNormal.set('');
    this.lettrable.set(false);
    this.actif.set(true);
    this.erreurGenerale.set(null);
    this.erreurNumero.set(null);
  }

  voirPlanComptable(): void {
    this.compteCree.emit();
    this.ferme.emit();
  }

  demanderFermeture(): void {
    if (this.succes() || !this.formModifie()) {
      this.ferme.emit();
      return;
    }
    this.confirmationFermeture.set(true);
  }

  annulerFermeture(): void {
    this.confirmationFermeture.set(false);
  }

  confirmerFermeture(): void {
    this.confirmationFermeture.set(false);
    this.ferme.emit();
  }
}
