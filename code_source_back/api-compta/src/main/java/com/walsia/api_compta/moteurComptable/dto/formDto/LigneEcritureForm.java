package com.walsia.api_compta.moteurComptable.dto.formDto;

import com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LigneEcritureForm(
        @NotNull String compteId,
        @NotNull SensCompte sens,
        @NotNull @DecimalMin(value = "0.01") BigDecimal montant,
        String libelle
) {
}
