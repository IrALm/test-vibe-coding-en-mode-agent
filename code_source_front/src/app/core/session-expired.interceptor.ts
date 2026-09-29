import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { CurrentUserService } from './current-user.service';

/** Sur un 401, ne redirige que si l'utilisateur était connu comme connecté juste avant
 * cet appel - ça couvre la vraie déconnexion brutale en plein milieu d'usage, sans
 * perturber les vérifications routinières "suis-je connecté ?" (home.ts, admin.guard.ts)
 * qui reçoivent normalement un 401 pour un visiteur anonyme et le gèrent déjà elles-mêmes
 * (affichage de la page publique, pas une erreur à signaler). */
export const sessionExpiredInterceptor: HttpInterceptorFn = (req, next) => {
  const currentUserService = inject(CurrentUserService);
  const router = inject(Router);
  const etaitConnecte = currentUserService.utilisateur() !== null;

  return next(req).pipe(
    catchError((erreur: unknown) => {
      if (etaitConnecte && erreur instanceof HttpErrorResponse && erreur.status === 401) {
        currentUserService.utilisateur.set(null);
        router.navigate(['/connexion']);
      }
      return throwError(() => erreur);
    })
  );
};
