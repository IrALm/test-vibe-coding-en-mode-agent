package com.walsia.api_compta.integrationClient.dto.readDto;

import com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte;

public record CompteComptableReadDto(
        String id,
        String numero,
        String libelle,
        SensCompte sensNormal,
        boolean lettrable,
        boolean actif
) {
}
