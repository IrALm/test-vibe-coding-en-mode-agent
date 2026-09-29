package com.walsia.api_compta.integrationClient.repository;

import com.walsia.api_compta.integrationClient.entity.referentiel.ClasseCompteComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClasseCompteComptableRepository
        extends JpaRepository<ClasseCompteComptable, String>, JpaSpecificationExecutor<ClasseCompteComptable> {

    long countByReferentielComptable_Id(String referentielComptableId);
}
