import { HttpInterceptorFn } from '@angular/common/http';

/** Le login backend-mediated (BFF) repose sur un cookie de session httpOnly : sans ce
 * flag, le navigateur ne l'envoie/l'accepte pas sur les requêtes cross-origin. */
export const withCredentialsInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req.clone({ withCredentials: true }));
};
