export interface ReglesMotDePasse {
  longueur: boolean;
  majuscule: boolean;
  minuscule: boolean;
  chiffre: boolean;
  special: boolean;
}

/** Seuils minimaux pour l'UX : la politique réellement appliquée vit côté serveur
 * (Keycloak / API), qui doit rester la seule source de vérité en cas de désaccord. */
export function evaluerReglesMotDePasse(motDePasse: string): ReglesMotDePasse {
  return {
    longueur: motDePasse.length >= 8,
    majuscule: /[A-Z]/.test(motDePasse),
    minuscule: /[a-z]/.test(motDePasse),
    chiffre: /[0-9]/.test(motDePasse),
    special: /[^A-Za-z0-9]/.test(motDePasse)
  };
}

export function toutesReglesValides(regles: ReglesMotDePasse): boolean {
  return Object.values(regles).every(Boolean);
}

export function compterReglesValides(regles: ReglesMotDePasse): number {
  return Object.values(regles).filter(Boolean).length;
}
