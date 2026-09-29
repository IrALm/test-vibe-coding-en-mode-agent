package com.walsia.api_compta.moteurComptable.dto.formDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record EcritureCreationForm(
        @NotNull LocalDate date,
        String reference,
        @NotBlank String libelle,
        @NotEmpty @Valid List<LigneEcritureForm> lignes
) {
}
