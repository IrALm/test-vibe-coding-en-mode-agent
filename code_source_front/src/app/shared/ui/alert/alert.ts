import { Component, input } from '@angular/core';

export type TypeAlerte = 'error' | 'warning' | 'success';

/** Alerte — bandeau plein-largeur, fond "soft", pas d'icône requise pour le MVP. */
@Component({
  selector: 'app-alert',
  template: `<div class="alert" [class]="'alert--' + type()" role="alert"><ng-content /></div>`,
  styles: [
    `
      .alert {
        display: flex;
        gap: 10px;
        align-items: flex-start;
        padding: 12px 16px;
        border-radius: var(--radius-md);
        font-size: 14px;
        white-space: pre-wrap;
      }

      .alert--error {
        background: var(--error-soft);
        color: var(--error-on-soft);
      }

      .alert--warning {
        background: var(--warning-soft);
        color: var(--warning-on-soft);
      }

      .alert--success {
        background: var(--success-soft);
        color: var(--success-on-soft);
      }
    `
  ]
})
export class Alert {
  readonly type = input<TypeAlerte>('error');
}
