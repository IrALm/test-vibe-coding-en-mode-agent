package com.walsia.api_compta.integrationClient.dto.formDto;

import com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte;

public record CompteComptableSearchForm(
        String q,
        SensCompte sens,
        Integer page,
        Integer size
) {
    public CompteComptableSearchForm {
        page = page != null ? page : 0;
        size = size != null ? size : 6;
    }
}
