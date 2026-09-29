import { Component, computed, input } from '@angular/core';

import { Role } from '../../../core/models';

/** Badge de rôle — une teinte par rôle, coins légèrement arrondis (6px, pas pill)
 * pour se distinguer visuellement des badges de statut. */
@Component({
  selector: 'app-role-badge',
  template: `<span class="role-badge" [style.background]="couleurs().bg" [style.color]="couleurs().fg" [style.border-color]="couleurs().border">{{ role() }}</span>`,
  styles: [
    `
      .role-badge {
        display: inline-block;
        padding: 2px 10px;
        border-radius: var(--radius-md);
        border: 1px solid;
        font-size: 11px;
        font-weight: 700;
        letter-spacing: 0.02em;
        white-space: nowrap;
      }
    `
  ]
})
export class RoleBadge {
  readonly role = input.required<Role>();

  protected readonly couleurs = computed(() => {
    const suffixe = this.suffixePourRole(this.role());
    return {
      bg: `var(--role-${suffixe}-bg)`,
      fg: `var(--role-${suffixe}-fg)`,
      border: `var(--role-${suffixe}-border)`
    };
  });

  private suffixePourRole(role: Role): string {
    const table: Record<Role, string> = {
      ADMIN: 'admin',
      ADMIN_FINANCIER: 'admin-financier',
      COMPTABLE: 'comptable',
      RH: 'rh',
      ACHATS: 'achats',
      CAISSIER: 'caissier',
      LECTURE_SEULE: 'lecture'
    };
    return table[role];
  }
}
