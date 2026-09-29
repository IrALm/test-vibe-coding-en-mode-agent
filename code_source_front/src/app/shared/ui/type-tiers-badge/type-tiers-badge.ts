import { Component, computed, input } from '@angular/core';

import { TypeTiers } from '../../../core/models';

const SUFFIXES: Record<TypeTiers, string> = {
  CLIENT: 'caissier',
  FOURNISSEUR: 'admin',
  SALARIE: 'comptable',
  ORGANISME_SOCIAL: 'rh',
  AUTRE: 'lecture',
};

const LIBELLES: Record<TypeTiers, string> = {
  CLIENT: 'Client',
  FOURNISSEUR: 'Fournisseur',
  SALARIE: 'Salarié',
  ORGANISME_SOCIAL: 'Organisme social',
  AUTRE: 'Autre',
};

/** Badge de type de tiers — réutilise les tokens de couleur des badges de rôle
 * (mêmes teintes : Client=bleu/caissier, Fournisseur=cuivre/admin, Salarié=vert/comptable,
 * Organisme social=violet/rh, Autre=gris/lecture) plutôt que d'introduire une nouvelle palette. */
@Component({
  selector: 'app-type-tiers-badge',
  template: `<span
    class="type-tiers-badge"
    [style.background]="couleurs().bg"
    [style.color]="couleurs().fg"
    [style.border-color]="couleurs().border"
    >{{ libelle() }}</span
  >`,
  styles: [
    `
      .type-tiers-badge {
        display: inline-block;
        padding: 2px 10px;
        border-radius: var(--radius-md);
        border: 1px solid;
        font-size: 11px;
        font-weight: 700;
        letter-spacing: 0.02em;
        white-space: nowrap;
      }
    `,
  ],
})
export class TypeTiersBadge {
  readonly type = input.required<TypeTiers>();

  protected readonly libelle = computed(() => LIBELLES[this.type()]);
  protected readonly couleurs = computed(() => {
    const suffixe = SUFFIXES[this.type()];
    return {
      bg: `var(--role-${suffixe}-bg)`,
      fg: `var(--role-${suffixe}-fg)`,
      border: `var(--role-${suffixe}-border)`,
    };
  });
}

export const LIBELLES_TYPE_TIERS = LIBELLES;

/** Couleur "pleine" (pas soft) associée à un type, pour une pastille ponctuelle (KPI...). */
export function couleurPastilleTypeTiers(type: TypeTiers): string {
  return `var(--role-${SUFFIXES[type]}-fg)`;
}
