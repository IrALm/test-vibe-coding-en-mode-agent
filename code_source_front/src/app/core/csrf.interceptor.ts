import { HttpInterceptorFn } from '@angular/common/http';

const NOM_COOKIE_CSRF = 'XSRF-TOKEN';
const NOM_HEADER_CSRF = 'X-XSRF-TOKEN';
const METHODES_SURES = new Set(['GET', 'HEAD', 'OPTIONS']);

function lireCookie(nom: string): string | null {
  const match = document.cookie.match(new RegExp(`(?:^|; )${nom}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

/** Double-submit CSRF : relit le cookie XSRF-TOKEN (posé au login, non httpOnly) et le
 * renvoie en header sur les requêtes qui modifient l'état. Un site tiers ne peut pas lire
 * ce cookie (same-origin policy) donc ne peut pas forger le header attendu, même s'il
 * parvient à déclencher la requête. Voir CsrfDoubleSubmitFilter côté backend. */
export const csrfInterceptor: HttpInterceptorFn = (req, next) => {
  if (METHODES_SURES.has(req.method)) {
    return next(req);
  }
  const token = lireCookie(NOM_COOKIE_CSRF);
  if (!token) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { [NOM_HEADER_CSRF]: token } }));
};
