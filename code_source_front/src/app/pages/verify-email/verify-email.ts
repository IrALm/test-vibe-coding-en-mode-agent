import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth.service';
import { extraireMessageErreur } from '../../core/http-error.util';
import { Alert } from '../../shared/ui/alert/alert';

@Component({
  selector: 'app-verify-email',
  imports: [RouterLink, Alert],
  templateUrl: './verify-email.html'
})
export class VerifyEmail {
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  readonly enCours = signal(true);
  readonly erreur = signal<string | null>(null);
  readonly succes = signal(false);

  constructor() {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.erreur.set("Aucun token dans l'URL (attendu : ?token=...).");
      this.enCours.set(false);
      return;
    }
    this.authService.verifierEmail(token).subscribe({
      next: () => {
        this.succes.set(true);
        this.enCours.set(false);
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCours.set(false);
      }
    });
  }
}
