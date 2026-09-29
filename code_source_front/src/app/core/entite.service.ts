import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { EntiteCreationForm, EntiteCreeeReadDto, EntiteReadDto } from './models';

@Injectable({ providedIn: 'root' })
export class EntiteService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/entites`;

  creerEntite(form: EntiteCreationForm): Observable<EntiteCreeeReadDto> {
    return this.http.post<EntiteCreeeReadDto>(this.baseUrl, form);
  }

  obtenirMonEntite(): Observable<EntiteReadDto> {
    return this.http.get<EntiteReadDto>(`${this.baseUrl}/moi`);
  }
}
