import { Component, ElementRef, HostListener, computed, effect, inject, input, model, signal } from '@angular/core';

import {
  aujourdhuiIso,
  construireIso,
  formaterIsoEnJJMMAAAA,
  joursDansLeMois,
  parserJJMMAAAAenIso,
  premierJourSemaineDuMois,
} from '../../../core/date.util';

const NOMS_MOIS = [
  'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
  'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre',
];
const NOMS_JOURS = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];

type Vue = 'jours' | 'mois' | 'annees';

interface CelluleJour {
  jour: number;
  iso: string;
  horsMois: boolean;
  desactive: boolean;
  estAujourdhui: boolean;
  estSelectionne: boolean;
}

/** Calendrier maison (aucune dépendance date-picker dans ce front) : saisie manuelle jj/mm/aaaa
 * synchronisée avec un panneau calendrier à 3 niveaux (jours -> mois -> années) pour naviguer
 * vite sur une date éloignée, sans dépendre d'un <input type="date"> natif (rendu très variable
 * d'un navigateur/OS à l'autre). `value` est le model (ISO 'yyyy-MM-dd'), `min`/`max` (ISO,
 * optionnels) désactivent les jours hors bornes dans la grille. */
@Component({
  selector: 'app-date-picker',
  templateUrl: './date-picker.html',
  styleUrl: './date-picker.scss',
})
export class DatePicker {
  private readonly elementRef = inject(ElementRef);

  readonly value = model<string>('');
  readonly min = input<string | undefined>(undefined);
  readonly max = input<string | undefined>(undefined);
  readonly placeholder = input<string>('jj/mm/aaaa');

  private readonly aujourdhui = aujourdhuiIso();

  readonly ouvert = signal(false);
  readonly vue = signal<Vue>('jours');
  readonly texteAffiche = signal('');
  readonly anneeAffichee = signal(this.decomposer(this.aujourdhui).annee);
  readonly moisAffiche = signal(this.decomposer(this.aujourdhui).mois);

  readonly nomsJours = NOMS_JOURS;
  readonly nomMois = computed(() => NOMS_MOIS[this.moisAffiche() - 1]);
  readonly decenieDebut = computed(() => Math.floor(this.anneeAffichee() / 10) * 10 - 1);

  readonly celluleJours = computed<CelluleJour[]>(() => this.construireGrilleJours());
  readonly celluleMois = computed(() =>
    NOMS_MOIS.map((nom, i) => ({ nom: nom.slice(0, 3), mois: i + 1, actif: i + 1 === this.moisAffiche() })),
  );
  readonly celluleAnnees = computed(() => {
    const debut = this.decenieDebut();
    return Array.from({ length: 12 }, (_, i) => debut + i).map((annee) => ({
      annee,
      actif: annee === this.anneeAffichee(),
    }));
  });

  constructor() {
    effect(() => {
      const iso = this.value();
      this.texteAffiche.set(formaterIsoEnJJMMAAAA(iso));
      const base = this.decomposer(iso || this.aujourdhui);
      this.anneeAffichee.set(base.annee);
      this.moisAffiche.set(base.mois);
    });
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.ouvert.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.ouvert.set(false);
  }

  togglePanneau(): void {
    this.ouvert.update((o) => !o);
    if (this.ouvert()) this.vue.set('jours');
  }

  onSaisieTexte(texte: string): void {
    this.texteAffiche.set(texte);
  }

  onBlurTexte(): void {
    this.commitTexte();
  }

  onEntreeTexte(): void {
    this.commitTexte();
    this.ouvert.set(false);
  }

  private commitTexte(): void {
    const texte = this.texteAffiche().trim();
    if (texte === '') return;
    const iso = parserJJMMAAAAenIso(texte);
    if (iso && this.estDansLesBornes(iso)) {
      this.value.set(iso);
    } else {
      this.texteAffiche.set(formaterIsoEnJJMMAAAA(this.value()));
    }
  }

  cliquerEntete(): void {
    if (this.vue() === 'jours') this.vue.set('mois');
    else if (this.vue() === 'mois') this.vue.set('annees');
  }

  precedent(): void {
    if (this.vue() === 'jours') this.changerMois(-1);
    else if (this.vue() === 'mois') this.anneeAffichee.update((a) => a - 1);
    else this.anneeAffichee.update((a) => a - 10);
  }

  suivant(): void {
    if (this.vue() === 'jours') this.changerMois(1);
    else if (this.vue() === 'mois') this.anneeAffichee.update((a) => a + 1);
    else this.anneeAffichee.update((a) => a + 10);
  }

  private changerMois(delta: number): void {
    let mois = this.moisAffiche() + delta;
    let annee = this.anneeAffichee();
    if (mois < 1) {
      mois = 12;
      annee--;
    } else if (mois > 12) {
      mois = 1;
      annee++;
    }
    this.moisAffiche.set(mois);
    this.anneeAffichee.set(annee);
  }

  choisirJour(cellule: CelluleJour): void {
    if (cellule.desactive) return;
    if (cellule.horsMois) {
      const base = this.decomposer(cellule.iso);
      this.anneeAffichee.set(base.annee);
      this.moisAffiche.set(base.mois);
    }
    this.value.set(cellule.iso);
    this.ouvert.set(false);
  }

  choisirMois(mois: number): void {
    this.moisAffiche.set(mois);
    this.vue.set('jours');
  }

  choisirAnnee(annee: number): void {
    this.anneeAffichee.set(annee);
    this.vue.set('mois');
  }

  allerAujourdhui(): void {
    this.value.set(this.aujourdhui);
    this.ouvert.set(false);
  }

  private estDansLesBornes(iso: string): boolean {
    const min = this.min();
    const max = this.max();
    if (min && iso < min) return false;
    if (max && iso > max) return false;
    return true;
  }

  private decomposer(iso: string): { annee: number; mois: number; jour: number } {
    const [annee, mois, jour] = iso.split('-').map(Number);
    return { annee, mois, jour };
  }

  private creerCellule(annee: number, mois: number, jour: number, horsMois: boolean): CelluleJour {
    const iso = construireIso(annee, mois, jour);
    return {
      jour,
      iso,
      horsMois,
      desactive: !this.estDansLesBornes(iso),
      estAujourdhui: iso === this.aujourdhui,
      estSelectionne: iso === this.value(),
    };
  }

  private construireGrilleJours(): CelluleJour[] {
    const annee = this.anneeAffichee();
    const mois = this.moisAffiche();
    const decalage = premierJourSemaineDuMois(annee, mois);
    const totalJours = joursDansLeMois(annee, mois);
    const moisPrecedent = mois === 1 ? 12 : mois - 1;
    const anneePrecedente = mois === 1 ? annee - 1 : annee;
    const joursMoisPrecedent = joursDansLeMois(anneePrecedente, moisPrecedent);

    const cellules: CelluleJour[] = [];
    for (let i = decalage - 1; i >= 0; i--) {
      cellules.push(this.creerCellule(anneePrecedente, moisPrecedent, joursMoisPrecedent - i, true));
    }
    for (let jour = 1; jour <= totalJours; jour++) {
      cellules.push(this.creerCellule(annee, mois, jour, false));
    }
    const moisSuivant = mois === 12 ? 1 : mois + 1;
    const anneeSuivante = mois === 12 ? annee + 1 : annee;
    let jourSuivant = 1;
    while (cellules.length % 7 !== 0) {
      cellules.push(this.creerCellule(anneeSuivante, moisSuivant, jourSuivant++, true));
    }
    return cellules;
  }
}
