import { Component, inject } from '@angular/core';

import { ThemeService } from '../../../core/theme.service';

@Component({
  selector: 'app-theme-toggle',
  template: `
    <button type="button" class="theme-toggle" (click)="themeService.basculer()">
      {{ themeService.theme() === 'light' ? '☀ Clair' : '● Sombre' }}
    </button>
  `,
  styles: [
    `
      .theme-toggle {
        position: fixed;
        top: 16px;
        right: 16px;
        z-index: 30;
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 14px;
        border-radius: var(--radius-pill);
        border: 1px solid var(--border-strong);
        background: var(--surface-2);
        color: var(--text);
        font-size: 13px;
        font-weight: 600;
        font-family: var(--font-sans);
        cursor: pointer;
        box-shadow: var(--shadow-sm);
      }
    `
  ]
})
export class ThemeToggle {
  protected readonly themeService = inject(ThemeService);
}
