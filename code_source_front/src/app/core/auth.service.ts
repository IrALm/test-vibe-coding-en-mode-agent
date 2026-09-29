import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { UtilisateurReadDto } from './models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/auth`;

  login(email: string, motDePasse: string): Observable<UtilisateurReadDto> {
    return this.http.post<UtilisateurReadDto>(`${this.baseUrl}/login`, { email, motDePasse });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/logout`, {});
  }

  definirMotDePassePermanent(nouveauMotDePasse: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/definir-mot-de-passe`, { nouveauMotDePasse });
  }

  demanderReinitialisation(email: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/forgot-password`, { email });
  }

  reinitialiserMotDePasse(token: string, nouveauMotDePasse: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/reset-password`, { token, nouveauMotDePasse });
  }

  verifierEmail(token: string): Observable<void> {
    return this.http.get<void>(`${this.baseUrl}/verify-email`, { params: { token } });
  }

  renvoyerVerification(email: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/resend-verification`, { email });
  }
}
