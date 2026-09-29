import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  CompteComptableCreationForm,
  CompteComptableExisteReadDto,
  CompteComptableReadDto,
  CompteOptionReadDto,
} from './models';

@Injectable({ providedIn: 'root' })
export class CompteComptableCreationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api`;

  listerComptesDeClasse(classeId: string): Observable<CompteOptionReadDto[]> {
    return this.http.get<CompteOptionReadDto[]>(`${this.baseUrl}/classes/${classeId}/comptes`);
  }

  creerCompte(form: CompteComptableCreationForm): Observable<CompteComptableReadDto> {
    return this.http.post<CompteComptableReadDto>(`${this.baseUrl}/comptes`, form);
  }

  numeroExiste(numero: string): Observable<CompteComptableExisteReadDto> {
    const params = new HttpParams().set('numero', numero);
    return this.http.get<CompteComptableExisteReadDto>(`${this.baseUrl}/comptes/exists`, {
      params,
    });
  }
}
