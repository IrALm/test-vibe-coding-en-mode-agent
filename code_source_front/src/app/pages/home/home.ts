import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth.service';
import { CurrentUserService } from '../../core/current-user.service';
import { EntiteService } from '../../core/entite.service';
import { EntiteReadDto } from '../../core/models';
import { UtilisateurService } from '../../core/utilisateur.service';
import { LogoMark } from '../../shared/ui/logo-mark/logo-mark';

type OngletCompte = 'infos' | 'admin';

@Component({
  selector: 'app-home',
  imports: [RouterLink, LogoMark],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class Home {
  private readonly authService = inject(AuthService);
  private readonly utilisateurService = inject(UtilisateurService);
  private readonly entiteService = inject(EntiteService);
  protected readonly currentUserService = inject(CurrentUserService);

  readonly entite = signal<EntiteReadDto | null>(null);
  readonly onglet = signal<OngletCompte>('infos');

  protected readonly initiales = computed(() => {
    const u = this.currentUserService.utilisateur();
    if (!u) return '';
    return `${u.nom?.[0] ?? ''}${u.prenom?.[0] ?? ''}`.toUpperCase();
  });

  constructor() {
    // Seule source de vérité pour l'état "connecté" : tenté à chaque arrivée sur
    // l'accueil (fonctionne aussi après un rechargement de page, cookie encore valide).
    this.utilisateurService.obtenirMonProfil().subscribe({
      next: (profil) => {
        this.currentUserService.utilisateur.set(profil);
        this.entiteService.obtenirMonEntite().subscribe({
          next: (entite) => this.entite.set(entite),
          error: () => this.entite.set(null)
        });
      },
      error: () => this.currentUserService.utilisateur.set(null)
    });
  }

  choisirOnglet(onglet: OngletCompte): void {
    this.onglet.set(onglet);
  }

  seDeconnecter(): void {
    this.authService.logout().subscribe({
      // Le logout backend est idempotent : même en erreur réseau, on efface l'état local.
      next: () => this.apresDeconnexion(),
      error: () => this.apresDeconnexion()
    });
  }

  private apresDeconnexion(): void {
    this.currentUserService.utilisateur.set(null);
    this.entite.set(null);
  }
}
