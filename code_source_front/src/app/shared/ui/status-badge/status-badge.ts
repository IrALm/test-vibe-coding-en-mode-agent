import { Component, input } from '@angular/core';

export type ToneBadge = 'neutral' | 'warning' | 'success' | 'accent' | 'error';

/** Badge de statut — pill, fond "soft" + texte "on-soft". Tons : neutral (brouillon),
 * warning (en attente), success (validée), accent (payée), error (rejetée). */
@Component({
  selector: 'app-status-badge',
  template: `<span class="status-badge" [class]="'status-badge--' + tone()"><ng-content /></span>`,
  styles: [
    `
      .status-badge {
        display: inline-block;
        padding: 4px 12px;
        border-radius: var(--radius-pill);
        font-size: 12px;
        font-weight: 600;
        white-space: nowrap;
      }

      .status-badge--neutral {
        background: var(--n-200);
        color: var(--n-700);
      }

      .status-badge--warning {
        background: var(--warning-soft);
        color: var(--warning-on-soft);
      }

      .status-badge--success {
        background: var(--success-soft);
        color: var(--success-on-soft);
      }

      .status-badge--accent {
        background: var(--accent-soft);
        color: var(--accent-on-soft);
      }

      .status-badge--error {
        background: var(--error-soft);
        color: var(--error-on-soft);
      }
    `
  ]
})
export class StatusBadge {
  readonly tone = input<ToneBadge>('neutral');
}
