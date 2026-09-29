import { HttpErrorResponse } from '@angular/common/http';

import { ApiErrorBody } from './models';

export function extraireMessageErreur(erreur: unknown): string {
  if (erreur instanceof HttpErrorResponse) {
    const corps = erreur.error as ApiErrorBody | undefined;
    if (corps?.erreurs) {
      return Object.entries(corps.erreurs)
        .map(([champ, message]) => `${champ} : ${message}`)
        .join(' | ');
    }
    if (corps?.message) {
      return corps.message;
    }
    return `Erreur HTTP ${erreur.status}`;
  }
  return 'Erreur inattendue';
}
