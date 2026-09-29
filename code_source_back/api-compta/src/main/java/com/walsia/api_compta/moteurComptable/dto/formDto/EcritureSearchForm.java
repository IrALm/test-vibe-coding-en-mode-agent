package com.walsia.api_compta.moteurComptable.dto.formDto;

import com.walsia.api_compta.moteurComptable.entity.ecriture.StatutEcriture;

import java.time.LocalDate;
import java.util.List;

public record EcritureSearchForm(
        String q,
        List<StatutEcriture> statuts,
        String exerciceId,
        LocalDate dateDebut,
        LocalDate dateFin,
        String sort,
        Integer page,
        Integer size
) {
    public EcritureSearchForm {
        page = page != null ? page : 0;
        size = size != null ? size : 20;
    }
}
