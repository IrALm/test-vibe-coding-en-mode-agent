package com.walsia.api_compta.moteurComptable.dto.formDto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ExerciceCreationForm(
        @NotNull LocalDate dateDebut,
        @NotNull LocalDate dateFin
) {
}
