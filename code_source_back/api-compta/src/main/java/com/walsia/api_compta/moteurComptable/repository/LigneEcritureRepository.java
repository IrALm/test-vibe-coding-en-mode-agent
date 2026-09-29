package com.walsia.api_compta.moteurComptable.repository;

import com.walsia.api_compta.moteurComptable.entity.ecriture.LigneEcriture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface LigneEcritureRepository extends JpaRepository<LigneEcriture, String> {

    /**
     * Solde cumulé (crédit - débit) d'un compte avant le début d'un exercice, écritures
     * validées uniquement. Sert de "solde d'ouverture" (à-nouveaux) pour les comptes de
     * bilan (classes 1 à 5) dans le grand livre — un compte de gestion (6/7/8) reste à
     * zéro chaque exercice (le service ne l'appelle que pour les classes 1-5).
     */
    @Query("""
            SELECT COALESCE(SUM(CASE WHEN l.sens = com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte.CREDIT THEN l.montant ELSE -l.montant END), 0)
            FROM LigneEcriture l JOIN l.ecriture e
            WHERE l.compte.id = :compteId AND e.date < :dateDebutExercice AND e.statut = 'VALIDEE'
            """)
    BigDecimal calculerSoldeAvant(@Param("compteId") String compteId, @Param("dateDebutExercice") LocalDate dateDebutExercice);

    /** Mouvements validés d'un compte sur un exercice, triés chronologiquement (grand livre). */
    @Query("""
            SELECT l FROM LigneEcriture l JOIN FETCH l.ecriture e
            WHERE l.compte.id = :compteId AND e.exercice.id = :exerciceId AND e.statut = 'VALIDEE'
            ORDER BY e.date, e.numero
            """)
    List<LigneEcriture> findGrandLivre(@Param("compteId") String compteId, @Param("exerciceId") String exerciceId);

    /** Totaux débit/crédit par compte mouvementé, écritures validées uniquement (balance). */
    @Query("""
            SELECT l.compte.id, l.compte.numero, l.compte.libelle,
                   SUM(CASE WHEN l.sens = com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte.DEBIT THEN l.montant ELSE 0 END),
                   SUM(CASE WHEN l.sens = com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte.CREDIT THEN l.montant ELSE 0 END)
            FROM LigneEcriture l JOIN l.ecriture e
            WHERE e.exercice.id = :exerciceId AND e.entite.id = :entiteId AND e.statut = 'VALIDEE'
            GROUP BY l.compte.id, l.compte.numero, l.compte.libelle
            ORDER BY l.compte.numero
            """)
    List<Object[]> findBalance(@Param("exerciceId") String exerciceId, @Param("entiteId") String entiteId);

    /**
     * Résultat provisoire = Σ(crédit - débit) des classes 6 (charges), 7 (produits), 8 (HAO),
     * écritures validées uniquement. Exclut l'écriture de clôture système elle-même
     * (genereParCloture) : sinon, dès que cloturerExercice() la crée pour solder ces mêmes
     * comptes, ce résultat s'annulerait tout seul à 0 pour l'exercice qu'elle vient de clôturer.
     */
    @Query("""
            SELECT COALESCE(SUM(CASE WHEN l.sens = com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte.CREDIT THEN l.montant ELSE -l.montant END), 0)
            FROM LigneEcriture l JOIN l.ecriture e
            WHERE e.exercice.id = :exerciceId AND e.statut = 'VALIDEE' AND e.genereParCloture = false
            AND l.compte.classeCompteComptable.numero IN (6, 7, 8)
            """)
    BigDecimal calculerResultat(@Param("exerciceId") String exerciceId);

    /**
     * Solde net (crédit - débit) de chaque compte de gestion (classes 6/7/8) mouvementé sur un
     * exercice, écritures validées non-clôture uniquement - sert à construire les lignes de
     * soldage de l'écriture de clôture (cloturerExercice) : chaque compte retourné doit recevoir
     * une ligne de sens opposé pour être ramené à zéro.
     */
    @Query("""
            SELECT l.compte.id, SUM(CASE WHEN l.sens = com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte.CREDIT THEN l.montant ELSE -l.montant END) AS soldeNet
            FROM LigneEcriture l JOIN l.ecriture e
            WHERE e.exercice.id = :exerciceId AND e.statut = 'VALIDEE' AND e.genereParCloture = false
            AND l.compte.classeCompteComptable.numero IN (6, 7, 8)
            GROUP BY l.compte.id
            HAVING SUM(CASE WHEN l.sens = com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte.CREDIT THEN l.montant ELSE -l.montant END) <> 0
            """)
    List<Object[]> calculerSoldesGestion(@Param("exerciceId") String exerciceId);

    /**
     * Soldes d'ouverture (à-nouveaux) de tous les comptes de bilan (classes 1 à 5) mouvementés
     * avant le début d'un exercice - même formule que calculerSoldeAvant, mais pour tous les
     * comptes d'un coup. Sert à réconcilier la balance avec le grand livre (GrandLivreBalanceServiceImpl.
     * obtenirBalance) : sans ça, le solde d'un compte de bilan diffère entre les deux écrans dès
     * qu'il porte un solde reporté d'un exercice antérieur.
     */
    @Query("""
            SELECT l.compte.id, l.compte.numero, l.compte.libelle,
                   SUM(CASE WHEN l.sens = com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte.CREDIT THEN l.montant ELSE -l.montant END)
            FROM LigneEcriture l JOIN l.ecriture e
            WHERE e.entite.id = :entiteId AND e.date < :dateDebutExercice AND e.statut = 'VALIDEE'
            AND l.compte.classeCompteComptable.numero BETWEEN 1 AND 5
            GROUP BY l.compte.id, l.compte.numero, l.compte.libelle
            """)
    List<Object[]> calculerSoldesOuvertureBilan(@Param("entiteId") String entiteId,
                                                 @Param("dateDebutExercice") LocalDate dateDebutExercice);
}
