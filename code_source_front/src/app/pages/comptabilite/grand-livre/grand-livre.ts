import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { ComptabiliteService } from '../../../core/comptabilite.service';
import { formaterMontant } from '../../../core/devise.util';
import { EntiteService } from '../../../core/entite.service';
import { ExerciceService } from '../../../core/exercice.service';
import { extraireMessageErreur } from '../../../core/http-error.util';
import {
  ClasseCompteComptableReadDto,
  CompteComptableReadDto,
  ExerciceReadDto,
  GrandLivreReadDto,
} from '../../../core/models';
import { PlanComptableService } from '../../../core/plan-comptable.service';
import { Alert } from '../../../shared/ui/alert/alert';
import { SelecteurExercice } from '../../../shared/ui/selecteur-exercice/selecteur-exercice';

interface GroupeClasseComptes {
  classe: ClasseCompteComptableReadDto;
  comptes: CompteComptableReadDto[];
  ouverte: boolean;
}

@Component({
  selector: 'app-grand-livre',
  imports: [RouterLink, Alert, DatePipe, SelecteurExercice],
  templateUrl: './grand-livre.html',
  styleUrl: './grand-livre.scss',
})
export class GrandLivre {
  private readonly route = inject(ActivatedRoute);
  private readonly exerciceService = inject(ExerciceService);
  private readonly planComptableService = inject(PlanComptableService);
  private readonly comptabiliteService = inject(ComptabiliteService);
  private readonly entiteService = inject(EntiteService);

  readonly enCours = signal(true);
  readonly erreur = signal<string | null>(null);
  readonly devise = signal<string | undefined>(undefined);

  readonly exercices = signal<ExerciceReadDto[]>([]);
  readonly exerciceId = signal('');

  readonly classes = signal<ClasseCompteComptableReadDto[]>([]);
  readonly comptesParClasse = signal<Record<string, CompteComptableReadDto[]>>({});
  readonly query = signal('');
  readonly classeOuverteId = signal<string | null>(null);
  readonly compteSelectionne = signal<CompteComptableReadDto | null>(null);

  readonly grandLivre = signal<GrandLivreReadDto | null>(null);
  readonly enCoursGrandLivre = signal(false);
  readonly erreurGrandLivre = signal<string | null>(null);

  // Accordion : une seule classe dépliée à la fois. Pendant une recherche, on ignore ce
  // repli manuel et on déplie automatiquement toutes les classes ayant un compte
  // correspondant (et on masque celles qui n'en ont aucun).
  readonly groupes = computed<GroupeClasseComptes[]>(() => {
    const q = this.query().trim().toLowerCase();
    const enRecherche = q !== '';
    return this.classes()
      .map((classe) => {
        const tousComptes = this.comptesParClasse()[classe.id] ?? [];
        if (enRecherche) {
          const comptes = tousComptes.filter((c) =>
            `${c.numero} ${c.libelle}`.toLowerCase().includes(q),
          );
          return { classe, comptes, ouverte: comptes.length > 0 };
        }
        const ouverte = this.classeOuverteId() === classe.id;
        return { classe, comptes: ouverte ? tousComptes : [], ouverte };
      })
      .filter((g) => !enRecherche || g.comptes.length > 0);
  });

  constructor() {
    this.entiteService.obtenirMonEntite().subscribe({ next: (e) => this.devise.set(e.devise) });
    const exerciceIdInitial = this.route.snapshot.queryParamMap.get('exerciceId');
    // Chaîné (exercices -> classes -> comptes), jamais en parallèle.
    this.exerciceService.lister().subscribe({
      next: (exercices) => {
        this.exercices.set(exercices);
        const ouvert = exercices.find((e) => e.statut === 'OUVERT');
        this.exerciceId.set(exerciceIdInitial ?? ouvert?.id ?? exercices[0]?.id ?? '');
        this.chargerClasses();
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCours.set(false);
      },
    });
  }

  private chargerClasses(): void {
    this.planComptableService.listerClasses().subscribe({
      next: (classes) => {
        this.classes.set(classes);
        this.chargerComptesDeChaqueClasse(classes);
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCours.set(false);
      },
    });
  }

  private chargerComptesDeChaqueClasse(classes: ClasseCompteComptableReadDto[]): void {
    forkJoin(
      classes.map((c) => this.planComptableService.rechercherComptes(c.id, { size: 200 })),
    ).subscribe({
      next: (pages) => {
        const map: Record<string, CompteComptableReadDto[]> = {};
        classes.forEach((c, i) => (map[c.id] = pages[i].content));
        this.comptesParClasse.set(map);
        this.enCours.set(false);
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCours.set(false);
      },
    });
  }

  changerExercice(id: string): void {
    this.exerciceId.set(id);
    if (this.compteSelectionne()) this.chargerGrandLivre();
  }

  onSaisieRecherche(valeur: string): void {
    this.query.set(valeur);
  }

  toggleClasse(classeId: string): void {
    this.classeOuverteId.update((id) => (id === classeId ? null : classeId));
  }

  selectionnerCompte(compte: CompteComptableReadDto): void {
    this.compteSelectionne.set(compte);
    this.chargerGrandLivre();
  }

  private chargerGrandLivre(): void {
    const compte = this.compteSelectionne();
    const exerciceId = this.exerciceId();
    if (!compte || !exerciceId) return;
    this.enCoursGrandLivre.set(true);
    this.erreurGrandLivre.set(null);
    this.comptabiliteService.obtenirGrandLivre(exerciceId, compte.id).subscribe({
      next: (grandLivre) => {
        this.grandLivre.set(grandLivre);
        this.enCoursGrandLivre.set(false);
      },
      error: (erreur) => {
        this.erreurGrandLivre.set(extraireMessageErreur(erreur));
        this.enCoursGrandLivre.set(false);
      },
    });
  }

  protected formaterMontant(montant: number): string {
    return montant ? formaterMontant(montant, this.devise()) : '—';
  }
}
