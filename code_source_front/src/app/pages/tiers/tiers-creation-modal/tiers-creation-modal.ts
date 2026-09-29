import { Component, computed, inject, output, signal } from '@angular/core';

import { CompteComptableCreationService } from '../../../core/compte-comptable-creation.service';
import { extraireMessageErreur } from '../../../core/http-error.util';
import { ClasseCompteComptableReadDto, CompteOptionReadDto, TypeTiers } from '../../../core/models';
import { PlanComptableService } from '../../../core/plan-comptable.service';
import { TiersService } from '../../../core/tiers.service';
import { Alert } from '../../../shared/ui/alert/alert';
import { LIBELLES_TYPE_TIERS } from '../../../shared/ui/type-tiers-badge/type-tiers-badge';

type Etape = 1 | 2 | 3;

const TYPES_TIERS: TypeTiers[] = ['CLIENT', 'FOURNISSEUR', 'SALARIE', 'ORGANISME_SOCIAL', 'AUTRE'];

@Component({
  selector: 'app-tiers-creation-modal',
  imports: [Alert],
  templateUrl: './tiers-creation-modal.html',
  styleUrl: './tiers-creation-modal.scss',
})
export class TiersCreationModal {
  private readonly planComptableService = inject(PlanComptableService);
  private readonly compteComptableCreationService = inject(CompteComptableCreationService);
  private readonly tiersService = inject(TiersService);

  readonly ferme = output<void>();
  readonly tiersCree = output<void>();

  protected readonly typesTiers = TYPES_TIERS;
  protected readonly libellesType = LIBELLES_TYPE_TIERS;

  readonly etape = signal<Etape>(1);
  readonly confirmationFermeture = signal(false);
  readonly succes = signal(false);
  readonly enCoursCreation = signal(false);
  readonly erreurGenerale = signal<string | null>(null);

  // Étape 1 — Identité
  readonly type = signal<TypeTiers | ''>('');
  readonly raisonSociale = signal('');
  readonly numeroFiscal = signal('');
  readonly intitulePoste = signal('');

  // Étape 2 — Contact
  readonly nomContact = signal('');
  readonly email = signal('');
  readonly telephone = signal('');
  readonly adresse = signal('');

  // Étape 3 — Compte & options
  readonly classes = signal<ClasseCompteComptableReadDto[]>([]);
  readonly classeId = signal('');
  readonly comptesDeClasse = signal<CompteOptionReadDto[]>([]);
  readonly chargementComptes = signal(false);
  readonly compteAssocieId = signal('');
  readonly actif = signal(true);

  readonly classeSelectionnee = computed(
    () => this.classes().find((c) => c.id === this.classeId()) ?? null,
  );
  readonly compteSelectionne = computed(
    () => this.comptesDeClasse().find((c) => c.id === this.compteAssocieId()) ?? null,
  );

  readonly avertissementEmail = computed(() => {
    const valeur = this.email().trim();
    return !!valeur && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(valeur);
  });

  readonly typeLibelle = computed(() => {
    const type = this.type();
    return type ? this.libellesType[type] : '—';
  });

  readonly etape1Valide = computed(() => !!this.type() && !!this.raisonSociale().trim());
  readonly etapeValide = computed(() => (this.etape() === 1 ? this.etape1Valide() : true));

  readonly formModifie = computed(
    () =>
      !!this.type() ||
      !!this.raisonSociale().trim() ||
      !!this.numeroFiscal().trim() ||
      !!this.intitulePoste().trim() ||
      !!this.nomContact().trim() ||
      !!this.email().trim() ||
      !!this.telephone().trim() ||
      !!this.adresse().trim() ||
      !!this.classeId() ||
      !!this.compteAssocieId(),
  );

  constructor() {
    this.planComptableService.listerClasses().subscribe({
      next: (classes) => this.classes.set(classes),
    });
  }

  choisirType(type: TypeTiers): void {
    this.type.set(type);
    if (type !== 'SALARIE') {
      this.intitulePoste.set('');
    }
  }

  choisirClasse(classeId: string): void {
    this.classeId.set(classeId);
    this.compteAssocieId.set('');
    this.comptesDeClasse.set([]);
    if (!classeId) return;

    this.chargementComptes.set(true);
    this.compteComptableCreationService.listerComptesDeClasse(classeId).subscribe({
      next: (comptes) => {
        this.comptesDeClasse.set(comptes);
        this.chargementComptes.set(false);
      },
      error: () => this.chargementComptes.set(false),
    });
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

  creerTiers(): void {
    const type = this.type();
    if (!this.etapeValide() || !type) return;

    this.enCoursCreation.set(true);
    this.erreurGenerale.set(null);

    this.tiersService
      .creerTiers({
        type,
        raisonSociale: this.raisonSociale().trim(),
        nomContact: this.nomContact().trim() || undefined,
        email: this.email().trim() || undefined,
        telephone: this.telephone().trim() || undefined,
        adresse: this.adresse().trim() || undefined,
        numeroFiscal: this.numeroFiscal().trim() || undefined,
        intitulePoste: type === 'SALARIE' ? this.intitulePoste().trim() || undefined : undefined,
        actif: this.actif(),
        compteAssocieId: this.compteAssocieId() || undefined,
      })
      .subscribe({
        next: () => {
          this.enCoursCreation.set(false);
          this.succes.set(true);
        },
        error: (erreur: unknown) => {
          this.enCoursCreation.set(false);
          this.erreurGenerale.set(extraireMessageErreur(erreur));
        },
      });
  }

  creerUnAutre(): void {
    this.etape.set(1);
    this.succes.set(false);
    this.type.set('');
    this.raisonSociale.set('');
    this.numeroFiscal.set('');
    this.intitulePoste.set('');
    this.nomContact.set('');
    this.email.set('');
    this.telephone.set('');
    this.adresse.set('');
    this.classeId.set('');
    this.comptesDeClasse.set([]);
    this.compteAssocieId.set('');
    this.actif.set(true);
    this.erreurGenerale.set(null);
  }

  voirLaListe(): void {
    this.tiersCree.emit();
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
