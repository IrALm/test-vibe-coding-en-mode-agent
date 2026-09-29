/** Utilitaires date ISO ('yyyy-MM-dd', format transmis par le back) <-> affichage jj/mm/aaaa.
 * Pas de dépendance externe (date-fns, etc.) — pur JS, cohérent avec le reste du front. */

export function formaterIsoEnJJMMAAAA(iso: string | null | undefined): string {
  if (!iso) return '';
  const [annee, mois, jour] = iso.split('-');
  if (!annee || !mois || !jour) return '';
  return `${jour}/${mois}/${annee}`;
}

/** Retourne l'ISO correspondant, ou null si le texte n'est pas une date jj/mm/aaaa valide. */
export function parserJJMMAAAAenIso(texte: string): string | null {
  const correspondance = texte.trim().match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
  if (!correspondance) return null;
  const jour = Number(correspondance[1]);
  const mois = Number(correspondance[2]);
  const annee = Number(correspondance[3]);
  const date = new Date(annee, mois - 1, jour);
  // Rejette les débordements silencieux de Date (ex. 31/02 -> 03/03).
  if (date.getFullYear() !== annee || date.getMonth() !== mois - 1 || date.getDate() !== jour) {
    return null;
  }
  return construireIso(annee, mois, jour);
}

export function construireIso(annee: number, mois: number, jour: number): string {
  return `${String(annee).padStart(4, '0')}-${String(mois).padStart(2, '0')}-${String(jour).padStart(2, '0')}`;
}

export function aujourdhuiIso(): string {
  const maintenant = new Date();
  return construireIso(maintenant.getFullYear(), maintenant.getMonth() + 1, maintenant.getDate());
}

/** Nombre de jours dans un mois donné (mois 1-12). */
export function joursDansLeMois(annee: number, mois: number): number {
  return new Date(annee, mois, 0).getDate();
}

/** Jour de la semaine du 1er du mois, 0 = lundi .. 6 = dimanche (convention FR). */
export function premierJourSemaineDuMois(annee: number, mois: number): number {
  const jsDay = new Date(annee, mois - 1, 1).getDay(); // 0 = dimanche .. 6 = samedi
  return (jsDay + 6) % 7;
}
