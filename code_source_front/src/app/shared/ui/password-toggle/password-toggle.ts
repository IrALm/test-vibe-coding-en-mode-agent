import { Component, model } from '@angular/core';

/** Bouton texte + icône œil, positionné dans le padding droit réservé du champ
 * mot de passe parent (voir .champ-mdp dans styles/components.scss). */
@Component({
  selector: 'app-password-toggle',
  template: `
    <button
      type="button"
      class="btn-toggle-mdp"
      [attr.aria-pressed]="visible()"
      [attr.aria-label]="visible() ? 'Masquer le mot de passe' : 'Afficher le mot de passe'"
      (click)="visible.set(!visible())"
    >
      @if (visible()) {
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 3l18 18" />
          <path
            d="M10.6 5.2A11 11 0 0 1 12 5c7 0 11 7 11 7a13.2 13.2 0 0 1-3.2 3.9M6.6 6.6C3.5 8.4 1 12 1 12s4 7 11 7a10.4 10.4 0 0 0 5.4-1.5"
          />
          <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" />
        </svg>
      } @else {
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7Z" />
          <circle cx="12" cy="12" r="3" />
        </svg>
      }
      <span>{{ visible() ? 'Masquer' : 'Afficher' }}</span>
    </button>
  `,
  styles: [
    `
      .btn-toggle-mdp {
        position: absolute;
        right: 6px;
        top: 50%;
        transform: translateY(-50%);
        display: inline-flex;
        align-items: center;
        gap: 5px;
        height: 28px;
        padding: 0 10px;
        border-radius: var(--radius-sm);
        border: 1px solid var(--border-strong);
        background: var(--surface-2);
        color: var(--accent-on-soft);
        font-size: 12px;
        font-weight: 600;
        font-family: var(--font-sans);
        cursor: pointer;
      }

      svg {
        width: 14px;
        height: 14px;
        flex-shrink: 0;
      }
    `
  ]
})
export class PasswordToggle {
  readonly visible = model(false);
}
