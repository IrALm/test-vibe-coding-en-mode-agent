export type CodeReferentiel = 'SYSCOHADA_NORMAL' | 'SYSCOHADA_SMT' | 'SYCEBNL';

export type TypeEntite = 'PME' | 'ONG' | 'ASSOCIATION' | 'ECOLE' | 'AUTRE';

export type Role = 'ADMIN' | 'ADMIN_FINANCIER' | 'COMPTABLE' | 'RH' | 'ACHATS' | 'CAISSIER' | 'LECTURE_SEULE';

export interface EntiteCreationForm {
  raisonSociale: string;
  typeEntite: TypeEntite;
  pays?: string;
  devise?: string;
  numeroIdentification?: string;
  referentielComptableCode: CodeReferentiel;
  adminNom: string;
  adminPostNom?: string;
  adminPrenom: string;
  adminEmail: string;
  adminTelephone?: string;
}

export interface EntiteReadDto {
  id: string;
  raisonSociale: string;
  typeEntite: TypeEntite;
  pays?: string;
  devise?: string;
  numeroIdentification?: string;
  dateCreation?: string;
  actif: boolean;
  referentielComptableCode: CodeReferentiel;
  referentielComptableLibelle: string;
  nombreClasses: number;
  nombreComptes: number;
}

/** DTO unique réutilisé par login (id/entiteId à null) et /api/utilisateurs/me
 * (id/entiteId à null aussi) : le backend ne peuple que les champs pertinents
 * à chaque endpoint, jamais un sous-type dédié par cas d'usage. */
export interface UtilisateurReadDto {
  id: string | null;
  nom: string | null;
  postNom: string | null;
  prenom: string | null;
  poste: string | null;
  email: string | null;
  role: Role | null;
  actif: boolean;
  emailVerifie: boolean;
  motDePasseTemporaire: boolean;
  entiteId: string | null;
  createdAt: string | null;
}

export interface UtilisateurCreationForm {
  nom: string;
  postNom?: string;
  prenom: string;
  poste?: string;
  email: string;
  role: Role;
}

export interface UtilisateurSearchForm {
  nom?: string;
  email?: string;
  role?: Role;
  actif?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface UtilisateurPageReadDto {
  content: UtilisateurReadDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  premierePage: boolean;
  dernierePage: boolean;
}

export interface EntiteCreeeReadDto {
  entite: EntiteReadDto;
  administrateur: UtilisateurReadDto;
}

export interface ReferentielComptableReadDto {
  id: string;
  code: CodeReferentiel;
  libelle: string;
  description?: string;
  version?: string;
}

export type SensCompte = 'DEBIT' | 'CREDIT';

export interface PlanComptableRecapReadDto {
  referentielComptableCode: CodeReferentiel;
  referentielComptableLibelle: string;
  nombreClasses: number;
  nombreComptes: number;
}

export interface ClasseCompteComptableReadDto {
  id: string;
  numero: number;
  titre: string;
  description?: string;
  nombreComptes: number;
}

export interface CompteComptableReadDto {
  id: string;
  numero: string;
  libelle: string;
  sensNormal: SensCompte;
  lettrable: boolean;
  actif: boolean;
}

export interface CompteComptableSearchParams {
  q?: string;
  sens?: SensCompte;
  page?: number;
  size?: number;
}

export interface CompteComptablePageReadDto {
  content: CompteComptableReadDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  premierePage: boolean;
  dernierePage: boolean;
}

export interface CompteOptionReadDto {
  id: string;
  numero: string;
  libelle: string;
  sensNormal: SensCompte;
}

export interface CompteComptableCreationForm {
  numero: string;
  libelle: string;
  classeCompteComptableId: string;
  parentId?: string;
  sensNormal: SensCompte;
  lettrable?: boolean;
  actif?: boolean;
}

export interface CompteComptableExisteReadDto {
  exists: boolean;
}

export type TypeTiers = 'CLIENT' | 'FOURNISSEUR' | 'SALARIE' | 'ORGANISME_SOCIAL' | 'AUTRE';

export interface TiersReadDto {
  id: string;
  type: TypeTiers;
  raisonSociale: string;
  nomContact?: string;
  email?: string;
  telephone?: string;
  adresse?: string;
  numeroFiscal?: string;
  intitulePoste?: string;
  actif: boolean;
  compteAssocieId?: string;
  compteAssocieNumero?: string;
  compteAssocieLibelle?: string;
}

export interface TiersSearchParams {
  q?: string;
  type?: TypeTiers;
  actif?: boolean;
  sort?: string;
  page?: number;
  size?: number;
}

export interface TiersPageReadDto {
  content: TiersReadDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  premierePage: boolean;
  dernierePage: boolean;
}

export interface TiersRecapReadDto {
  total: number;
  parType: Record<TypeTiers, number>;
  sansCompteAssocie: number;
}

export interface TiersCreationForm {
  type: TypeTiers;
  raisonSociale: string;
  nomContact?: string;
  email?: string;
  telephone?: string;
  adresse?: string;
  numeroFiscal?: string;
  intitulePoste?: string;
  actif?: boolean;
  compteAssocieId?: string;
}

export interface ApiErrorBody {
  timestamp?: string;
  status?: number;
  message?: string;
  erreurs?: Record<string, string>;
}

export type StatutExercice = 'OUVERT' | 'CLOS';

export interface ExerciceReadDto {
  id: string;
  dateDebut: string;
  dateFin: string;
  statut: StatutExercice;
}

export interface ExerciceCreationForm {
  dateDebut: string;
  dateFin: string;
}

export interface ClotureCheckReadDto {
  nombreBrouillon: number;
  nombreEnAttente: number;
  cloturable: boolean;
}

export interface ResultatReadDto {
  exerciceId: string;
  resultat: number;
}

export type StatutEcriture = 'BROUILLON' | 'EN_ATTENTE' | 'VALIDEE' | 'CONTREPASSEE';

export interface LigneEcritureReadDto {
  id: string;
  compteId: string;
  compteNumero: string;
  compteLibelle: string;
  sens: SensCompte;
  montant: number;
  libelle?: string;
}

export interface LigneEcritureForm {
  compteId: string;
  sens: SensCompte;
  montant: number;
  libelle?: string;
}

export interface EcritureReadDto {
  id: string;
  date: string;
  reference?: string;
  libelle: string;
  statut: StatutEcriture;
  numero?: string;
  journalId: string;
  journalLibelle: string;
  exerciceId: string;
  lignes: LigneEcritureReadDto[];
  totalDebit: number;
  totalCredit: number;
  equilibree: boolean;
  creeParId: string;
  creeParNom: string;
  creeLe: string;
  valideParId?: string;
  valideParNom?: string;
  valideLe?: string;
  motifRejet?: string;
  ecritureMiroirId?: string;
  contrePassationDeId?: string;
}

export interface EcritureCreationForm {
  date: string;
  reference?: string;
  libelle: string;
  lignes: LigneEcritureForm[];
}

export type EcritureModificationForm = EcritureCreationForm;

export interface EcritureSearchParams {
  q?: string;
  statuts?: StatutEcriture[];
  exerciceId?: string;
  dateDebut?: string;
  dateFin?: string;
  sort?: string;
  page?: number;
  size?: number;
}

export interface EcriturePageReadDto {
  content: EcritureReadDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  premierePage: boolean;
  dernierePage: boolean;
}

export interface RenvoyerBrouillonForm {
  motif?: string;
}

export interface ContrePassationForm {
  date?: string;
}

export interface EcritureStatsReadDto {
  brouillon: number;
  enAttente: number;
  validees: number;
}

export interface GrandLivreLigneReadDto {
  date: string;
  ecritureId: string;
  numero?: string;
  reference?: string;
  libelle?: string;
  debit: number;
  credit: number;
  soldeProgressif: number;
}

export interface GrandLivreReadDto {
  compteId: string;
  compteNumero: string;
  compteLibelle: string;
  soldeOuverture: number;
  lignes: GrandLivreLigneReadDto[];
  soldeCloture: number;
}

export interface BalanceLigneReadDto {
  compteId: string;
  compteNumero: string;
  compteLibelle: string;
  totalDebit: number;
  totalCredit: number;
  soldeDebiteur: number;
  soldeCrediteur: number;
}

export interface BalanceReadDto {
  lignes: BalanceLigneReadDto[];
  totalDebit: number;
  totalCredit: number;
}
