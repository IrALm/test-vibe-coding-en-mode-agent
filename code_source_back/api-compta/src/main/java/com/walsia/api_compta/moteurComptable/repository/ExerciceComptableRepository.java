package com.walsia.api_compta.moteurComptable.repository;

import com.walsia.api_compta.moteurComptable.entity.exercice.ExerciceComptable;
import com.walsia.api_compta.moteurComptable.entity.exercice.StatutExercice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExerciceComptableRepository extends JpaRepository<ExerciceComptable, String> {

    List<ExerciceComptable> findByEntite_IdOrderByDateDebutDesc(String entiteId);

    Optional<ExerciceComptable> findByEntite_IdAndStatut(String entiteId, StatutExercice statut);

    boolean existsByEntite_IdAndStatut(String entiteId, StatutExercice statut);

    long countByEntite_Id(String entiteId);
}
