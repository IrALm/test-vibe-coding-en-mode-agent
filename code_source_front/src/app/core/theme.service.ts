import { Injectable, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const CLE_STOCKAGE = 'comptano-theme';

/** Le thème est piloté par l'utilisateur (bouton), pas seulement prefers-color-scheme :
 * on ne lit la préférence système qu'à défaut de choix explicite déjà mémorisé. */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<Theme>(this.themeInitial());

  constructor() {
    this.appliquer(this.theme());
  }

  basculer(): void {
    const suivant: Theme = this.theme() === 'light' ? 'dark' : 'light';
    this.theme.set(suivant);
    this.appliquer(suivant);
    localStorage.setItem(CLE_STOCKAGE, suivant);
  }

  private themeInitial(): Theme {
    const stocke = localStorage.getItem(CLE_STOCKAGE);
    if (stocke === 'light' || stocke === 'dark') {
      return stocke;
    }
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }

  private appliquer(theme: Theme): void {
    document.documentElement.setAttribute('data-theme', theme);
  }
}
