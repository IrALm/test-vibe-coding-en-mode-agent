import { Component, computed, input } from '@angular/core';

import { compterReglesValides, evaluerReglesMotDePasse } from '../../../core/mot-de-passe.util';

/** Palier de robustesse 1..5 — pas de token dédié dans le design system pour une
 * échelle de sévérité à 5 crans : couleurs oklch choisies dans le même esprit que
 * les tokens existants (rouge → orange → jaune → vert clair → vert). */
const PALIERS = [
  { libelle: 'Très faible', couleur: 'oklch(0.58 0.17 25)' },
  { libelle: 'Faible', couleur: 'oklch(0.65 0.15 55)' },
  { libelle: 'Moyen', couleur: 'oklch(0.78 0.15 95)' },
  { libelle: 'Bon', couleur: 'oklch(0.72 0.12 145)' },
  { libelle: 'Fort', couleur: 'oklch(0.46 0.12 150)' }
];

@Component({
  selector: 'app-password-rules',
  template: `
    <ul class="liste-regles">
      <li [class.regle-ok]="regles().longueur"><span class="regle-puce">✓</span>Au moins 8 caractères</li>
      <li [class.regle-ok]="regles().majuscule"><span class="regle-puce">✓</span>Une majuscule (A-Z)</li>
      <li [class.regle-ok]="regles().minuscule"><span class="regle-puce">✓</span>Une minuscule (a-z)</li>
      <li [class.regle-ok]="regles().chiffre"><span class="regle-puce">✓</span>Un chiffre (0-9)</li>
      <li [class.regle-ok]="regles().special"><span class="regle-puce">✓</span>Un caractère spécial</li>
    </ul>
    @if (motDePasse().length > 0) {
      <div class="robustesse">
        <div class="robustesse-piste">
          <div class="robustesse-remplissage" [style.width.%]="nombreValide() * 20" [style.background]="palier().couleur"></div>
        </div>
        <span class="robustesse-libelle" [style.color]="palier().couleur">{{ palier().libelle }}</span>
      </div>
    }
  `,
  styles: [
    `
      .liste-regles {
        list-style: none;
        margin: 8px 0 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        gap: 4px;
        font-size: 12.5px;
      }

      .liste-regles li {
        display: flex;
        align-items: center;
        gap: 7px;
        color: var(--text-muted);
      }

      .regle-puce {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 14px;
        height: 14px;
        flex-shrink: 0;
        border-radius: 50%;
        border: 1px solid var(--border-strong);
        font-size: 10px;
        color: transparent;
      }

      .regle-ok {
        color: var(--success-on-soft);
        font-weight: 600;
      }

      .regle-ok .regle-puce {
        background: var(--success-soft);
        border-color: var(--success);
        color: var(--success-on-soft);
      }

      .robustesse {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-top: 10px;
      }

      .robustesse-piste {
        flex: 1;
        height: 6px;
        border-radius: var(--radius-pill);
        background: var(--surface-2);
        overflow: hidden;
      }

      .robustesse-remplissage {
        height: 100%;
        border-radius: var(--radius-pill);
        transition:
          width 0.2s ease,
          background 0.2s ease;
      }

      .robustesse-libelle {
        font-size: 12px;
        font-weight: 700;
        white-space: nowrap;
      }
    `
  ]
})
export class PasswordRules {
  readonly motDePasse = input.required<string>();

  protected readonly regles = computed(() => evaluerReglesMotDePasse(this.motDePasse()));
  protected readonly nombreValide = computed(() => compterReglesValides(this.regles()));
  protected readonly palier = computed(() => PALIERS[Math.max(1, this.nombreValide()) - 1]);
}
