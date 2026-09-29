import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  ClotureCheckReadDto,
  ExerciceCreationForm,
  ExerciceReadDto,
  ResultatReadDto,
} from './models';

@Injectable({ providedIn: 'root' })
export class ExerciceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/exercices`;

  lister(): Observable<ExerciceReadDto[]> {
    return this.http.get<ExerciceReadDto[]>(this.baseUrl);
  }

  obtenirOuvert(): Observable<ExerciceReadDto> {
    return this.http.get<ExerciceReadDto>(`${this.baseUrl}/ouvert`);
  }

  obtenirResultat(id: string): Observable<ResultatReadDto> {
    return this.http.get<ResultatReadDto>(`${this.baseUrl}/${id}/resultat`);
  }

  creer(form: ExerciceCreationForm): Observable<ExerciceReadDto> {
    return this.http.post<ExerciceReadDto>(this.baseUrl, form);
  }

  verifierCloture(id: string): Observable<ClotureCheckReadDto> {
    return this.http.get<ClotureCheckReadDto>(`${this.baseUrl}/${id}/cloture-check`);
  }

  cloturer(id: string): Observable<ExerciceReadDto> {
    return this.http.post<ExerciceReadDto>(`${this.baseUrl}/${id}/cloturer`, {});
  }
}
