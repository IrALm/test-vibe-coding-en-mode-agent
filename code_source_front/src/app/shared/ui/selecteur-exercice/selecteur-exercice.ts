import { DatePipe } from '@angular/common';
import { Component, HostListener, computed, input, output, signal } from '@angular/core';

import { ExerciceReadDto } from '../../../core/models';
import { StatusBadge } from '../status-badge/status-badge';

/** Sélecteur d'exercice accessible (bouton + listbox), remplace le <select> natif — même
 * pattern réutilisé sur Grand livre et Balance générale (handoff §I/§J, 2026-09-07).
 * `value`/`valuesChange` plutôt qu'un `model()` : le parent garde la main sur le rechargement
 * des données au changement (mêmes noms de méthode `changerExercice` déjà en place). */
@Component({
  selector: 'app-selecteur-exercice',
  imports: [DatePipe, StatusBadge],
  templateUrl: './selecteur-exercice.html',
  styleUrl: './selecteur-exercice.scss',
})
export class SelecteurExercice {
  readonly exercices = input<ExerciceReadDto[]>([]);
  readonly value = input<string>('');
  readonly valueChange = output<string>();

  readonly ouvert = signal(false);

  readonly exerciceSelectionne = computed(
    () => this.exercices().find((e) => e.id === this.value()) ?? null,
  );

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.ouvert.set(false);
  }

  toggle(): void {
    this.ouvert.update((o) => !o);
  }

  fermer(): void {
    this.ouvert.set(false);
  }

  choisir(id: string): void {
    this.fermer();
    if (id !== this.value()) this.valueChange.emit(id);
  }
}
