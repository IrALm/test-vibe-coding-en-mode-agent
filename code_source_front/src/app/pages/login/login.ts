import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth.service';
import { extraireMessageErreur } from '../../core/http-error.util';
import { Alert } from '../../shared/ui/alert/alert';
import { PasswordToggle } from '../../shared/ui/password-toggle/password-toggle';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, Alert, PasswordToggle],
  templateUrl: './login.html'
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly enCours = signal(false);
  readonly erreur = signal<string | null>(null);
  readonly mdpVisible = signal(false);

  readonly messageSucces = signal(
    this.route.snapshot.queryParamMap.get('motDePasseMisAJour') === '1' ? 'Mot de passe mis à jour.' : null
  );

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    motDePasse: ['', Validators.required]
  });

  soumettre(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enCours.set(true);
    this.erreur.set(null);

    const { email, motDePasse } = this.form.getRawValue();
    this.authService.login(email, motDePasse).subscribe({
      next: (resultat) => {
        this.enCours.set(false);

        if (!resultat.emailVerifie) {
          this.router.navigate(['/email-non-verifie'], { queryParams: { email } });
        } else if (resultat.motDePasseTemporaire) {
          this.router.navigate(['/definir-mot-de-passe']);
        } else {
          this.router.navigate(['/']);
        }
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCours.set(false);
      }
    });
  }
}
