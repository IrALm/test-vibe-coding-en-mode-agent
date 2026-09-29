import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { extraireMessageErreur } from '../../../core/http-error.util';
import { Role, UtilisateurPageReadDto, UtilisateurReadDto, UtilisateurSearchForm } from '../../../core/models';
import { UtilisateurService } from '../../../core/utilisateur.service';
import { RoleBadge } from '../../../shared/ui/role-badge/role-badge';
import { StatusBadge } from '../../../shared/ui/status-badge/status-badge';
import { Alert } from '../../../shared/ui/alert/alert';

const TAILLE_PAGE_CLIENT = 20;
const DELAI_DEBOUNCE_MS = 200;
const DELAI_FLASH_MS = 900;
const DELAI_TOAST_MS = 4000;

interface Confirmation {
  utilisateur: UtilisateurReadDto;
  versActif: boolean;
}

interface Toast {
  message: string;
  tone: 'success' | 'neutral' | 'error';
}

function normaliser(texte: string): string {
  return texte
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase();
}

@Component({
  selector: 'app-utilisateurs-liste',
  imports: [RouterLink, DatePipe, RoleBadge, StatusBadge, Alert],
  templateUrl: './utilisateurs-liste.html',
  styleUrl: './utilisateurs-liste.scss'
})
export class UtilisateursListe {
  private readonly fb = inject(FormBuilder);
  private readonly utilisateurService = inject(UtilisateurService);

  readonly roles: Role[] = ['ADMIN', 'ADMIN_FINANCIER', 'COMPTABLE', 'RH', 'ACHATS', 'CAISSIER', 'LECTURE_SEULE'];
  readonly enCours = signal(false);
  readonly erreur = signal<string | null>(null);
  private readonly resultat = signal<UtilisateurPageReadDto | null>(null);

  readonly filtres = this.fb.nonNullable.group({
    role: [''],
    actif: ['']
  });

  readonly query = signal('');
  private readonly queryDebouncee = signal('');
  private minuteurDebounce?: ReturnType<typeof setTimeout>;

  readonly pageClient = signal(0);

  readonly confirmation = signal<Confirmation | null>(null);
  readonly toast = signal<Toast | null>(null);
  readonly flashId = signal<string | null>(null);
  private minuteurToast?: ReturnType<typeof setTimeout>;
  private minuteurFlash?: ReturnType<typeof setTimeout>;

  readonly utilisateursFiltres = computed(() => {
    const contenu = this.resultat()?.content ?? [];
    const q = normaliser(this.queryDebouncee().trim());
    if (!q) return contenu;
    return contenu.filter((u) => {
      const hay = normaliser(`${u.nom ?? ''} ${u.postNom ?? ''} ${u.prenom ?? ''} ${u.email ?? ''}`);
      return hay.includes(q);
    });
  });

  readonly totalPagesClient = computed(() => Math.max(1, Math.ceil(this.utilisateursFiltres().length / TAILLE_PAGE_CLIENT)));

  readonly pageContenu = computed(() => {
    const debut = this.pageClient() * TAILLE_PAGE_CLIENT;
    return this.utilisateursFiltres().slice(debut, debut + TAILLE_PAGE_CLIENT);
  });

  constructor() {
    this.rechercher();
  }

  onSaisieRecherche(valeur: string): void {
    this.query.set(valeur);
    clearTimeout(this.minuteurDebounce);
    this.minuteurDebounce = setTimeout(() => {
      this.queryDebouncee.set(valeur);
      this.pageClient.set(0);
    }, DELAI_DEBOUNCE_MS);
  }

  filtrerParRole(role: Role | ''): void {
    this.filtres.patchValue({ role });
    this.rechercher();
  }

  filtrerParActif(actif: '' | 'true' | 'false'): void {
    this.filtres.patchValue({ actif });
    this.rechercher();
  }

  pagePrecedente(): void {
    this.pageClient.update((p) => Math.max(0, p - 1));
  }

  pageSuivante(): void {
    this.pageClient.update((p) => Math.min(this.totalPagesClient() - 1, p + 1));
  }

  demanderBascule(utilisateur: UtilisateurReadDto): void {
    this.confirmation.set({ utilisateur, versActif: !utilisateur.actif });
  }

  annulerConfirmation(): void {
    this.confirmation.set(null);
  }

  confirmerBascule(): void {
    const confirmation = this.confirmation();
    if (!confirmation) return;
    const { utilisateur, versActif } = confirmation;
    const appel = versActif ? this.utilisateurService.activer(utilisateur.id!) : this.utilisateurService.desactiver(utilisateur.id!);
    const nomComplet = `${utilisateur.nom} ${utilisateur.prenom}`.trim();

    appel.subscribe({
      next: (miseAJour) => {
        this.resultat.update((r) =>
          r ? { ...r, content: r.content.map((u) => (u.id === miseAJour.id ? miseAJour : u)) } : r
        );
        this.confirmation.set(null);
        this.declencherFlash(utilisateur.id!);
        this.afficherToast({
          message: versActif ? `${nomComplet} a été activé.` : `${nomComplet} a été désactivé.`,
          tone: versActif ? 'success' : 'neutral'
        });
      },
      error: (erreur) => {
        this.confirmation.set(null);
        this.afficherToast({ message: extraireMessageErreur(erreur), tone: 'error' });
      }
    });
  }

  fermerToast(): void {
    clearTimeout(this.minuteurToast);
    this.toast.set(null);
  }

  private afficherToast(toast: Toast): void {
    clearTimeout(this.minuteurToast);
    this.toast.set(toast);
    this.minuteurToast = setTimeout(() => this.toast.set(null), DELAI_TOAST_MS);
  }

  private declencherFlash(id: string): void {
    clearTimeout(this.minuteurFlash);
    this.flashId.set(id);
    this.minuteurFlash = setTimeout(() => this.flashId.set(null), DELAI_FLASH_MS);
  }

  private rechercher(): void {
    this.enCours.set(true);
    this.erreur.set(null);
    this.pageClient.set(0);

    const valeurs = this.filtres.getRawValue();
    // Le backend ne propose pas de recherche unifiée nom/post-nom/prénom/email : on
    // récupère ici tout l'ensemble filtré par rôle/statut, et le champ de recherche
    // filtre ensuite côté client (cf. utilisateursFiltres).
    const criteres: UtilisateurSearchForm = {
      role: (valeurs.role || undefined) as Role | undefined,
      actif: valeurs.actif === '' ? undefined : valeurs.actif === 'true',
      page: 0,
      size: 500,
      sortBy: 'nom',
      sortDirection: 'ASC'
    };

    this.utilisateurService.rechercherUtilisateurs(criteres).subscribe({
      next: (resultatPage) => {
        this.resultat.set(resultatPage);
        this.enCours.set(false);
      },
      error: (erreur) => {
        this.erreur.set(extraireMessageErreur(erreur));
        this.enCours.set(false);
      }
    });
  }
}
