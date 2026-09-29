package com.walsia.api_compta.moteurComptable.service.impl;

import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.moteurComptable.entity.journal.Journal;
import com.walsia.api_compta.moteurComptable.repository.JournalRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Provisionnement paresseux et idempotent du journal général d'une entité (singleton,
 * cf. contrainte unique entite_id). Pas de service dédié à la création d'entreprise :
 * le journal apparaît au premier exercice ou à la première écriture qui en a besoin.
 */
@Service
class JournalProvisioningService {

    private final JournalRepository journalRepository;

    JournalProvisioningService(JournalRepository journalRepository) {
        this.journalRepository = journalRepository;
    }

    Journal journalGeneralDe(Entite entite) {
        return journalRepository.findByEntite_Id(entite.getId())
                .orElseGet(() -> creerJournalGeneral(entite));
    }

    private Journal creerJournalGeneral(Entite entite) {
        try {
            return journalRepository.save(Journal.builder()
                    .libelle("Journal général")
                    .entite(entite)
                    .dernierNumero(0)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // Course concurrente sur uk_journal_entite : un autre thread l'a créé entre le findBy et le save.
            return journalRepository.findByEntite_Id(entite.getId()).orElseThrow(() -> e);
        }
    }
}
