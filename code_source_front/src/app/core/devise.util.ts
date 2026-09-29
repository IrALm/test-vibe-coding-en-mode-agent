/** Décimales usuelles par devise, pour le formatage d'affichage uniquement — le montant
 * transmis au backend n'est jamais arrondi ici, c'est un problème d'affichage, pas de saisie. */
export function decimalesDevise(devise?: string | null): number {
  return devise?.toUpperCase() === 'CDF' ? 0 : 2;
}

export function formaterMontant(montant: number, devise?: string | null): string {
  return montant.toLocaleString('fr-FR', {
    minimumFractionDigits: decimalesDevise(devise),
    maximumFractionDigits: decimalesDevise(devise),
  });
}
