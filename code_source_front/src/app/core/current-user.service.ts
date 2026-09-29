import { Injectable, signal } from '@angular/core';

import { UtilisateurReadDto } from './models';

@Injectable({ providedIn: 'root' })
export class CurrentUserService {
  readonly utilisateur = signal<UtilisateurReadDto | null>(null);
}
