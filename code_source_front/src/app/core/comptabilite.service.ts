import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { BalanceReadDto, GrandLivreReadDto } from './models';

@Injectable({ providedIn: 'root' })
export class ComptabiliteService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/comptabilite`;

  obtenirGrandLivre(exerciceId: string, compteId: string): Observable<GrandLivreReadDto> {
    const params = new HttpParams().set('exerciceId', exerciceId).set('compteId', compteId);
    return this.http.get<GrandLivreReadDto>(`${this.baseUrl}/grand-livre`, { params });
  }

  obtenirBalance(exerciceId: string): Observable<BalanceReadDto> {
    const params = new HttpParams().set('exerciceId', exerciceId);
    return this.http.get<BalanceReadDto>(`${this.baseUrl}/balance`, { params });
  }
}
