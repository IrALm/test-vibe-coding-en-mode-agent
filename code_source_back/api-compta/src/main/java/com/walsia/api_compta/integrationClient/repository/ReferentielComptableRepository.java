package com.walsia.api_compta.integrationClient.repository;

import com.walsia.api_compta.integrationClient.entity.referentiel.CodeReferentiel;
import com.walsia.api_compta.integrationClient.entity.referentiel.ReferentielComptable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReferentielComptableRepository extends JpaRepository<ReferentielComptable, String> {

    Optional<ReferentielComptable> findByCode(CodeReferentiel code);
}
