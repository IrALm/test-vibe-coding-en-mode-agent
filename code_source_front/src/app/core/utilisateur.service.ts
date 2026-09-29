import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  UtilisateurCreationForm,
  UtilisateurPageReadDto,
  UtilisateurReadDto,
  UtilisateurSearchForm
} from './models';

@Injectable({ providedIn: 'root' })
export class UtilisateurService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/utilisateurs`;

  obtenirMonProfil(): Observable<UtilisateurReadDto> {
    return this.http.get<UtilisateurReadDto>(`${this.baseUrl}/me`);
  }

  ajouterUtilisateur(form: UtilisateurCreationForm): Observable<UtilisateurReadDto> {
    return this.http.post<UtilisateurReadDto>(this.baseUrl, form);
  }

  rechercherUtilisateurs(form: UtilisateurSearchForm): Observable<UtilisateurPageReadDto> {
    return this.http.post<UtilisateurPageReadDto>(`${this.baseUrl}/rechercher`, form);
  }

  activer(id: string): Observable<UtilisateurReadDto> {
    return this.http.patch<UtilisateurReadDto>(`${this.baseUrl}/${id}/activer`, {});
  }

  desactiver(id: string): Observable<UtilisateurReadDto> {
    return this.http.patch<UtilisateurReadDto>(`${this.baseUrl}/${id}/desactiver`, {});
  }
}
