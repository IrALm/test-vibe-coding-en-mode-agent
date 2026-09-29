package com.walsia.api_compta.integrationClient.dto.readDto;

import com.walsia.api_compta.integrationClient.entity.entite.TypeEntite;
import com.walsia.api_compta.integrationClient.entity.referentiel.CodeReferentiel;

import java.time.LocalDate;

public record EntiteReadDto(
        String id,
        String raisonSociale,
        TypeEntite typeEntite,
        String pays,
        String devise,
        String numeroIdentification,
        LocalDate dateCreation,
        boolean actif,
        CodeReferentiel referentielComptableCode,
        String referentielComptableLibelle,
        long nombreClasses,
        long nombreComptes
) {
}
