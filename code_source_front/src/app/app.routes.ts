import { Routes } from '@angular/router';

import { adminGuard } from './core/admin.guard';
import { validationGuard } from './core/validation.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/home/home').then((m) => m.Home),
  },
  {
    path: 'creer-entreprise',
    loadComponent: () =>
      import('./pages/create-entreprise/create-entreprise').then((m) => m.CreateEntreprise),
  },
  {
    path: 'connexion',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: 'email-non-verifie',
    loadComponent: () =>
      import('./pages/email-verification-required/email-verification-required').then(
        (m) => m.EmailVerificationRequired,
      ),
  },
  {
    path: 'definir-mot-de-passe',
    loadComponent: () =>
      import('./pages/set-permanent-password/set-permanent-password').then(
        (m) => m.SetPermanentPassword,
      ),
  },
  {
    path: 'mot-de-passe-oublie',
    loadComponent: () =>
      import('./pages/forgot-password/forgot-password').then((m) => m.ForgotPassword),
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./pages/reset-password/reset-password').then((m) => m.ResetPassword),
  },
  {
    path: 'verify-email',
    loadComponent: () => import('./pages/verify-email/verify-email').then((m) => m.VerifyEmail),
  },
  {
    path: 'renvoyer-verification',
    loadComponent: () =>
      import('./pages/resend-verification/resend-verification').then((m) => m.ResendVerification),
  },
  {
    path: 'plan-comptable',
    loadComponent: () =>
      import('./pages/plan-comptable/plan-comptable-liste/plan-comptable-liste').then(
        (m) => m.PlanComptableListe,
      ),
  },
  {
    path: 'tiers',
    loadComponent: () =>
      import('./pages/tiers/tiers-liste/tiers-liste').then((m) => m.TiersListe),
  },
  {
    path: 'comptabilite',
    loadComponent: () =>
      import('./pages/comptabilite/comptabilite-tableau-bord/comptabilite-tableau-bord').then(
        (m) => m.ComptabiliteTableauBord,
      ),
  },
  {
    path: 'comptabilite/exercices',
    loadComponent: () =>
      import('./pages/comptabilite/exercices-liste/exercices-liste').then(
        (m) => m.ExercicesListe,
      ),
  },
  {
    path: 'comptabilite/ecritures',
    loadComponent: () =>
      import('./pages/comptabilite/ecritures-liste/ecritures-liste').then(
        (m) => m.EcrituresListe,
      ),
  },
  {
    path: 'comptabilite/ecritures/nouveau',
    loadComponent: () =>
      import('./pages/comptabilite/ecriture-saisie/ecriture-saisie').then(
        (m) => m.EcritureSaisie,
      ),
  },
  {
    path: 'comptabilite/ecritures/:id/modifier',
    loadComponent: () =>
      import('./pages/comptabilite/ecriture-saisie/ecriture-saisie').then(
        (m) => m.EcritureSaisie,
      ),
  },
  {
    path: 'comptabilite/ecritures/:id',
    loadComponent: () =>
      import('./pages/comptabilite/ecriture-detail/ecriture-detail').then(
        (m) => m.EcritureDetail,
      ),
  },
  {
    path: 'comptabilite/validation',
    canActivate: [validationGuard],
    loadComponent: () =>
      import('./pages/comptabilite/file-validation/file-validation').then(
        (m) => m.FileValidation,
      ),
  },
  {
    path: 'comptabilite/grand-livre',
    loadComponent: () =>
      import('./pages/comptabilite/grand-livre/grand-livre').then((m) => m.GrandLivre),
  },
  {
    path: 'comptabilite/balance',
    loadComponent: () =>
      import('./pages/comptabilite/balance-generale/balance-generale').then(
        (m) => m.BalanceGenerale,
      ),
  },
  {
    path: 'admin/utilisateurs',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./pages/admin/utilisateurs-liste/utilisateurs-liste').then(
        (m) => m.UtilisateursListe,
      ),
  },
  {
    path: 'admin/utilisateurs/nouveau',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./pages/admin/utilisateur-creation/utilisateur-creation').then(
        (m) => m.UtilisateurCreation,
      ),
  },
  { path: '**', redirectTo: '' },
];
