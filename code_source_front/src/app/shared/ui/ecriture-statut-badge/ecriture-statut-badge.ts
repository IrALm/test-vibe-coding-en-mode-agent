import { Component, computed, input } from '@angular/core';

import { StatutEcriture } from '../../../core/models';
import { StatusBadge, ToneBadge } from '../status-badge/status-badge';

export const LIBELLES_STATUT_ECRITURE: Record<StatutEcriture, string> = {
  BROUILLON: 'Brouillon',
  EN_ATTENTE: 'En attente',
  VALIDEE: 'Validée',
  CONTREPASSEE: 'Contre-passée',
};

const TONES_STATUT_ECRITURE: Record<StatutEcriture, ToneBadge> = {
  BROUILLON: 'neutral',
  EN_ATTENTE: 'warning',
  VALIDEE: 'success',
  CONTREPASSEE: 'accent',
};

/** Réutilise app-status-badge (tons déjà stylés) avec le libellé/ton propres à chaque statut
 * d'écriture, pour ne pas dupliquer ce mapping dans chaque écran (liste, détail, file de
 * validation, tableau de bord). */
@Component({
  selector: 'app-ecriture-statut-badge',
  imports: [StatusBadge],
  template: `<app-status-badge [tone]="tone()">{{ libelle() }}</app-status-badge>`,
})
export class EcritureStatutBadge {
  readonly statut = input.required<StatutEcriture>();

  protected readonly libelle = computed(() => LIBELLES_STATUT_ECRITURE[this.statut()]);
  protected readonly tone = computed(() => TONES_STATUT_ECRITURE[this.statut()]);
}
