import { Component, inject, input } from '@angular/core';

import { ThemeService } from '../../../core/theme.service';

/** Monogramme "W" — seule exception à la règle des tokens sémantiques : le
 * cuivre d'origine en clair devient noir pur en sombre (identité de marque fixe). */
@Component({
  selector: 'app-logo-mark',
  template: `
    <div class="logo-mark" [style.width.px]="size()" [style.height.px]="size()">
      <span [style.fontSize.px]="size() * 0.47" [style.color]="lettreCouleur()">C</span>
    </div>
  `,
  styles: [
    `
      .logo-mark {
        border-radius: 50%;
        background: var(--n-800);
        border: 3px solid var(--copper-500);
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }

      span {
        font-weight: 800;
      }
    `
  ]
})
export class LogoMark {
  private readonly themeService = inject(ThemeService);

  readonly size = input(36);

  protected lettreCouleur(): string {
    return this.themeService.theme() === 'light' ? 'var(--copper-200)' : '#000000';
  }
}
