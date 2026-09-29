package com.walsia.api_compta.moteurComptable.repository;

import com.walsia.api_compta.moteurComptable.entity.ecriture.Ecriture;
import com.walsia.api_compta.moteurComptable.entity.ecriture.StatutEcriture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EcritureRepository extends JpaRepository<Ecriture, String>, JpaSpecificationExecutor<Ecriture> {

    long countByExercice_IdAndStatut(String exerciceId, StatutEcriture statut);
}
