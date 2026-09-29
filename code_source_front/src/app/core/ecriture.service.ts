import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  ContrePassationForm,
  EcritureCreationForm,
  EcriturePageReadDto,
  EcritureModificationForm,
  EcritureReadDto,
  EcritureSearchParams,
  EcritureStatsReadDto,
  RenvoyerBrouillonForm,
} from './models';

@Injectable({ providedIn: 'root' })
export class EcritureService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/ecritures`;

  rechercher(criteres: EcritureSearchParams): Observable<EcriturePageReadDto> {
    let params = new HttpParams();
    if (criteres.q) params = params.set('q', criteres.q);
    if (criteres.statuts?.length) {
      for (const statut of criteres.statuts) params = params.append('statuts', statut);
    }
    if (criteres.exerciceId) params = params.set('exerciceId', criteres.exerciceId);
    if (criteres.dateDebut) params = params.set('dateDebut', criteres.dateDebut);
    if (criteres.dateFin) params = params.set('dateFin', criteres.dateFin);
    if (criteres.sort) params = params.set('sort', criteres.sort);
    if (criteres.page !== undefined) params = params.set('page', criteres.page);
    if (criteres.size !== undefined) params = params.set('size', criteres.size);

    return this.http.get<EcriturePageReadDto>(this.baseUrl, { params });
  }

  obtenirDetail(id: string): Observable<EcritureReadDto> {
    return this.http.get<EcritureReadDto>(`${this.baseUrl}/${id}`);
  }

  obtenirStats(exerciceId: string): Observable<EcritureStatsReadDto> {
    const params = new HttpParams().set('exerciceId', exerciceId);
    return this.http.get<EcritureStatsReadDto>(`${this.baseUrl}/stats`, { params });
  }

  creer(form: EcritureCreationForm): Observable<EcritureReadDto> {
    return this.http.post<EcritureReadDto>(this.baseUrl, form);
  }

  modifier(id: string, form: EcritureModificationForm): Observable<EcritureReadDto> {
    return this.http.patch<EcritureReadDto>(`${this.baseUrl}/${id}`, form);
  }

  supprimer(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  soumettre(id: string): Observable<EcritureReadDto> {
    return this.http.post<EcritureReadDto>(`${this.baseUrl}/${id}/soumettre`, {});
  }

  valider(id: string): Observable<EcritureReadDto> {
    return this.http.post<EcritureReadDto>(`${this.baseUrl}/${id}/valider`, {});
  }

  renvoyerEnBrouillon(id: string, form: RenvoyerBrouillonForm): Observable<EcritureReadDto> {
    return this.http.post<EcritureReadDto>(`${this.baseUrl}/${id}/renvoyer-en-brouillon`, form);
  }

  contrePasser(id: string, form: ContrePassationForm): Observable<EcritureReadDto> {
    return this.http.post<EcritureReadDto>(`${this.baseUrl}/${id}/contre-passer`, form);
  }
}
