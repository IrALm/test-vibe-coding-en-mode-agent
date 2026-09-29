package com.walsia.api_compta.moteurComptable.dto.readDto;

import com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte;

import java.math.BigDecimal;

public record LigneEcritureReadDto(
        String id,
        String compteId,
        String compteNumero,
        String compteLibelle,
        SensCompte sens,
        BigDecimal montant,
        String libelle
) {
}
