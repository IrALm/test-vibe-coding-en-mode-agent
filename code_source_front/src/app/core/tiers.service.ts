import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  TiersCreationForm,
  TiersPageReadDto,
  TiersReadDto,
  TiersRecapReadDto,
  TiersSearchParams,
} from './models';

@Injectable({ providedIn: 'root' })
export class TiersService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/tiers`;

  obtenirRecap(): Observable<TiersRecapReadDto> {
    return this.http.get<TiersRecapReadDto>(`${this.baseUrl}/recap`);
  }

  rechercherTiers(criteres: TiersSearchParams): Observable<TiersPageReadDto> {
    let params = new HttpParams();
    if (criteres.q) params = params.set('q', criteres.q);
    if (criteres.type) params = params.set('type', criteres.type);
    if (criteres.actif !== undefined) params = params.set('actif', criteres.actif);
    if (criteres.sort) params = params.set('sort', criteres.sort);
    if (criteres.page !== undefined) params = params.set('page', criteres.page);
    if (criteres.size !== undefined) params = params.set('size', criteres.size);

    return this.http.get<TiersPageReadDto>(this.baseUrl, { params });
  }

  creerTiers(form: TiersCreationForm): Observable<TiersReadDto> {
    return this.http.post<TiersReadDto>(this.baseUrl, form);
  }

  /** compteAssocieId = null pour dissocier. */
  associerCompte(id: string, compteAssocieId: string | null): Observable<TiersReadDto> {
    return this.http.patch<TiersReadDto>(`${this.baseUrl}/${id}/compte-associe`, { compteAssocieId });
  }
}
