import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth.service';
import { extraireMessageErreur } from '../../core/http-error.util';
import { evaluerReglesMotDePasse, toutesReglesValides } from '../../core/mot-de-passe.util';
import { Alert } from '../../shared/ui/alert/alert';
import { PasswordRules } from '../../shared/ui/password-rules/password-rules';
import { PasswordToggle } from '../../shared/ui/password-toggle/password-toggle';

@Component({
  selector: 'app-set-permanent-password',
  imports: [ReactiveFormsModule, Alert, PasswordToggle, PasswordRules],
  templateUrl: './set-permanent-password.html'
})
export class SetPermanentPassword {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly enCours = signal(false);
  readonly erreur = signal<string | null>(null);

  readonly mdpVisible = signal(false);
  readonly confirmationVisible = signal(false);

  readonly form = this.fb.nonNullable.group({
    nouveauMotDePasse: ['', Validators.required],
    confirmation: ['', Validators.required]
  });

  protected readonly nouveauMotDePasseTexte = toSignal(this.form.controls.nouveauMotDePasse.valueChanges, {
    initialValue: ''
  });
  protected readonly confirmationTexte = toSignal(this.form.controls.confirmation.valueChanges, { initialValue: '' });

  protected readonly reglesValides = computed(() =>
    toutesReglesValides(evaluerReglesMotDePasse(this.nouveauMotDePasseTexte()))
  );
  protected readonly correspond = computed(
    () => this.confirmationTexte().length > 0 && this.confirmationTexte() === this.nouveauMotDePasseTexte()
  );
  protected readonly peutSoumettre = computed(() => this.reglesValides() && this.correspond());

  soumettre(): void {
    if (!this.peutSoumettre()) {
      this.form.markAllAsTouched();
      return;
    }
    this.enCours.set(true);
    this.erreur.set(null);
    this.authService.definirMotDePassePermanent(this.form.getRawValue().nouveauMotDePasse).subscribe({
      next: () => {
        this.enCours.set(false);
        this.router.navigate(['/']);
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCours.set(false);
      }
    });
  }
}
