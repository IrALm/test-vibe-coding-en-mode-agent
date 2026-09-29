import { Component, DestroyRef, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { interval } from 'rxjs';

import { CurrentUserService } from './core/current-user.service';
import { UtilisateurService } from './core/utilisateur.service';
import { ThemeToggle } from './shared/ui/theme-toggle/theme-toggle';

/** Keycloak expire le refresh token après ssoSessionIdleTimeout (30 min par défaut,
 * non surchargé dans keycloak/realm-export.json) sans utilisation. En pingant un
 * endpoint léger bien avant cette limite tant que l'onglet est ouvert et visible, on
 * évite qu'un utilisateur actif (qui lit/remplit un formulaire sans déclencher d'appel
 * HTTP pendant un moment) se retrouve déconnecté brutalement en plein milieu d'une tâche. */
const INTERVALLE_HEARTBEAT_MS = 5 * 60 * 1000;

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ThemeToggle],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  private readonly currentUserService = inject(CurrentUserService);
  private readonly utilisateurService = inject(UtilisateurService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    const subscription = interval(INTERVALLE_HEARTBEAT_MS).subscribe(() => {
      if (this.currentUserService.utilisateur() && document.visibilityState === 'visible') {
        this.utilisateurService.obtenirMonProfil().subscribe({ error: () => {} });
      }
    });
    this.destroyRef.onDestroy(() => subscription.unsubscribe());
  }
}
