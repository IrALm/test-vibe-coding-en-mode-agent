package com.walsia.api_compta.moteurComptable.repository;

import com.walsia.api_compta.moteurComptable.entity.journal.Journal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JournalRepository extends JpaRepository<Journal, String> {

    Optional<Journal> findByEntite_Id(String entiteId);

    /** Verrou de ligne pour l'assignation atomique du prochain numéro séquentiel (cf. EcritureServiceImpl.valider). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM Journal j WHERE j.id = :id")
    Optional<Journal> findByIdForUpdate(@Param("id") String id);
}
