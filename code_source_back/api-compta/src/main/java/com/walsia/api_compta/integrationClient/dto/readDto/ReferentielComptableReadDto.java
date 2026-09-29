package com.walsia.api_compta.integrationClient.dto.readDto;

import com.walsia.api_compta.integrationClient.entity.referentiel.CodeReferentiel;

public record ReferentielComptableReadDto(
        String id,
        CodeReferentiel code,
        String libelle,
        String description,
        String version
) {
}
