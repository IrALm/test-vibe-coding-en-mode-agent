package com.walsia.api_compta.integrationClient.repository;

import com.walsia.api_compta.integrationClient.entity.referentiel.CompteComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompteComptableRepository
        extends JpaRepository<CompteComptable, String>, JpaSpecificationExecutor<CompteComptable> {

    long countByClasseCompteComptable_ReferentielComptable_Id(String referentielComptableId);

    boolean existsByPlanComptable_IdAndNumero(String planComptableId, String numero);

    /** Sert à retrouver le compte de résultat (111 bénéfice / 119 perte) à la clôture d'un exercice. */
    Optional<CompteComptable> findByPlanComptable_IdAndNumero(String planComptableId, String numero);

    /** Comptes d'une classe dans un plan donné, triés par numero : standards (entite null) + spécifiques de l'appelant.
     * Sert à alimenter le select « Compte parent » du formulaire de création. */
    @Query("""
            SELECT c FROM CompteComptable c
            WHERE c.classeCompteComptable.id = :classeId
            AND c.planComptable.id = :planComptableId
            AND (c.entite IS NULL OR c.entite.id = :entiteId)
            ORDER BY c.numero
            """)
    List<CompteComptable> findVisiblesParClasse(
            @Param("classeId") String classeId,
            @Param("planComptableId") String planComptableId,
            @Param("entiteId") String entiteId);

    /** Comptes visibles d'une classe dans un plan donné : standards (entite null) + spécifiques de l'appelant. */
    @Query("""
            SELECT COUNT(c) FROM CompteComptable c
            WHERE c.classeCompteComptable.id = :classeId
            AND c.planComptable.id = :planComptableId
            AND (c.entite IS NULL OR c.entite.id = :entiteId)
            """)
    long countVisiblesParClasse(
            @Param("classeId") String classeId,
            @Param("planComptableId") String planComptableId,
            @Param("entiteId") String entiteId);

    /** Comptes visibles dans un plan donné, toutes classes confondues : standards (entite null) + spécifiques de l'appelant. */
    @Query("""
            SELECT COUNT(c) FROM CompteComptable c
            WHERE c.planComptable.id = :planComptableId
            AND (c.entite IS NULL OR c.entite.id = :entiteId)
            """)
    long countVisiblesParPlan(
            @Param("planComptableId") String planComptableId,
            @Param("entiteId") String entiteId);
}
