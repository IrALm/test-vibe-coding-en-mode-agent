import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { CurrentUserService } from './current-user.service';
import { UtilisateurService } from './utilisateur.service';

/** Ne se fie pas à CurrentUserService seul (vide en cas d'arrivée directe sur l'URL, ex.
 * rechargement de page) : re-résout le profil à chaque activation. Réservé ADMIN/ADMIN_FINANCIER
 * — la file de validation doit être absente, pas seulement désactivée, pour un COMPTABLE. */
export const validationGuard: CanActivateFn = () => {
  const utilisateurService = inject(UtilisateurService);
  const currentUserService = inject(CurrentUserService);
  const router = inject(Router);

  return utilisateurService.obtenirMonProfil().pipe(
    map((profil) => {
      currentUserService.utilisateur.set(profil);
      return profil.role === 'ADMIN' || profil.role === 'ADMIN_FINANCIER'
        ? true
        : router.createUrlTree(['/']);
    }),
    catchError(() => of(router.createUrlTree(['/'])))
  );
};
