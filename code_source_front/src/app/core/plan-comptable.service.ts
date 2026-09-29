import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  ClasseCompteComptableReadDto,
  CompteComptablePageReadDto,
  CompteComptableSearchParams,
  PlanComptableRecapReadDto,
} from './models';

@Injectable({ providedIn: 'root' })
export class PlanComptableService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api`;

  obtenirRecap(): Observable<PlanComptableRecapReadDto> {
    return this.http.get<PlanComptableRecapReadDto>(`${this.baseUrl}/plan-comptable/recap`);
  }

  listerClasses(q?: string): Observable<ClasseCompteComptableReadDto[]> {
    const params = q ? new HttpParams().set('q', q) : undefined;
    return this.http.get<ClasseCompteComptableReadDto[]>(`${this.baseUrl}/classes-comptables`, {
      params,
    });
  }

  rechercherComptes(
    classeId: string,
    criteres: CompteComptableSearchParams,
  ): Observable<CompteComptablePageReadDto> {
    return this.http.get<CompteComptablePageReadDto>(
      `${this.baseUrl}/classes-comptables/${classeId}/comptes`,
      { params: this.paramsRecherche(criteres) },
    );
  }

  /** Recherche à travers toutes les classes du plan actif (pas de classe sélectionnée) - ex. retrouver un compte par numéro/libellé sans savoir dans quelle classe il se trouve. */
  rechercherComptesGlobal(criteres: CompteComptableSearchParams): Observable<CompteComptablePageReadDto> {
    return this.http.get<CompteComptablePageReadDto>(`${this.baseUrl}/comptes/rechercher`, {
      params: this.paramsRecherche(criteres),
    });
  }

  private paramsRecherche(criteres: CompteComptableSearchParams): HttpParams {
    let params = new HttpParams();
    if (criteres.q) params = params.set('q', criteres.q);
    if (criteres.sens) params = params.set('sens', criteres.sens);
    if (criteres.page !== undefined) params = params.set('page', criteres.page);
    if (criteres.size !== undefined) params = params.set('size', criteres.size);
    return params;
  }
}
