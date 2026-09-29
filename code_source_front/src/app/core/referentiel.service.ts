import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { ReferentielComptableReadDto } from './models';

@Injectable({ providedIn: 'root' })
export class ReferentielService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/referentiels-comptables`;

  lister(): Observable<ReferentielComptableReadDto[]> {
    return this.http.get<ReferentielComptableReadDto[]>(this.baseUrl);
  }
}
