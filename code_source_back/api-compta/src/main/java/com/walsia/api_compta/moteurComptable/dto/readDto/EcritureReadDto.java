package com.walsia.api_compta.moteurComptable.dto.readDto;

import com.walsia.api_compta.moteurComptable.entity.ecriture.StatutEcriture;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EcritureReadDto(
        String id,
        LocalDate date,
        String reference,
        String libelle,
        StatutEcriture statut,
        String numero,
        String journalId,
        String journalLibelle,
        String exerciceId,
        List<LigneEcritureReadDto> lignes,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        boolean equilibree,
        String creeParId,
        String creeParNom,
        LocalDateTime creeLe,
        String valideParId,
        String valideParNom,
        LocalDateTime valideLe,
        String motifRejet,
        String ecritureMiroirId,
        String contrePassationDeId
) {
}
