import { AfterViewInit, Component, ElementRef, input, viewChild } from '@angular/core';

/** Confirmation de création épurée (variante A) — jamais de détails de l'objet créé
 * (nom, email, rôle, identifiants...) dans title/message : uniquement une phrase
 * générique. Les actions sont projetées par l'appelant ; le focus se pose sur le
 * premier bouton .btn-primary trouvé parmi elles. */
@Component({
  selector: 'app-success-card',
  template: `
    <div class="success-card" role="status" aria-live="polite">
      <div class="success-pastille">
        <div class="success-pastille-interne">
          <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
            <path d="M5 12.5l4.5 4.5L19 7.5" />
          </svg>
        </div>
      </div>
      <h2 class="success-titre">{{ title() }}</h2>
      <p class="success-message">{{ message() }}</p>
      <div class="success-actions" #actions>
        <ng-content />
      </div>
    </div>
  `,
  styles: [
    `
      .success-card {
        width: 100%;
        max-width: 420px;
        margin: 0 auto;
        background: var(--surface);
        border: 1px solid var(--border);
        border-radius: 16px;
        box-shadow: var(--shadow-md);
        padding: 40px 32px;
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;
        gap: 6px;
      }

      // Vert fixe (identité "succès"), volontairement non lié aux tokens --success
      // qui basculent en teinte pastel en thème sombre (illisible en pastille pleine
      // avec coche blanche) — même logique d'exception documentée que app-logo-mark.
      .success-pastille {
        width: 64px;
        height: 64px;
        border-radius: 50%;
        background: #e6f4ea;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 14px;
        animation: success-pop 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
      }

      @media (prefers-reduced-motion: reduce) {
        .success-pastille {
          animation: none;
        }
      }

      .success-pastille-interne {
        width: 44px;
        height: 44px;
        border-radius: 50%;
        background: #1f9d55;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .success-pastille-interne svg {
        width: 22px;
        height: 22px;
      }

      @keyframes success-pop {
        0% {
          transform: scale(0.5);
          opacity: 0;
        }
        60% {
          transform: scale(1.08);
          opacity: 1;
        }
        100% {
          transform: scale(1);
          opacity: 1;
        }
      }

      .success-titre {
        font-size: 19px;
        font-weight: 700;
        color: var(--text);
        margin: 0;
      }

      .success-message {
        font-size: 14px;
        color: var(--text-muted);
        max-width: 320px;
        margin: 0;
      }

      .success-actions {
        display: flex;
        gap: 10px;
        margin-top: 18px;
      }
    `
  ]
})
export class SuccessCard implements AfterViewInit {
  readonly title = input.required<string>();
  readonly message = input.required<string>();

  private readonly actions = viewChild.required<ElementRef<HTMLElement>>('actions');

  ngAfterViewInit(): void {
    this.actions().nativeElement.querySelector<HTMLElement>('.btn-primary')?.focus();
  }
}
