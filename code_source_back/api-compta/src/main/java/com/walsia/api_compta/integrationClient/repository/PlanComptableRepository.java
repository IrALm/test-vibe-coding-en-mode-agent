package com.walsia.api_compta.integrationClient.repository;

import com.walsia.api_compta.integrationClient.entity.referentiel.PlanComptable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanComptableRepository extends JpaRepository<PlanComptable, String> {

    Optional<PlanComptable> findByReferentielComptable_IdAndActifTrue(String referentielComptableId);
}
