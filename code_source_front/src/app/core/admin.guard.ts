import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { CurrentUserService } from './current-user.service';
import { UtilisateurService } from './utilisateur.service';

/** Ne se fie pas à CurrentUserService seul (vide en cas d'arrivée directe sur une URL
 * admin, ex. rechargement de page) : re-résout le profil à chaque activation. */
export const adminGuard: CanActivateFn = () => {
  const utilisateurService = inject(UtilisateurService);
  const currentUserService = inject(CurrentUserService);
  const router = inject(Router);

  return utilisateurService.obtenirMonProfil().pipe(
    map((profil) => {
      currentUserService.utilisateur.set(profil);
      return profil.role === 'ADMIN' ? true : router.createUrlTree(['/']);
    }),
    catchError(() => of(router.createUrlTree(['/'])))
  );
};
