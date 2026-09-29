package com.walsia.api_compta.integrationClient.dto.readDto;

import com.walsia.api_compta.integrationClient.entity.referentiel.CodeReferentiel;

public record PlanComptableRecapReadDto(
        CodeReferentiel referentielComptableCode,
        String referentielComptableLibelle,
        long nombreClasses,
        long nombreComptes
) {
}
