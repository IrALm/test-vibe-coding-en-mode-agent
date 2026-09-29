package com.walsia.api_compta.moteurComptable.dto.formDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/** Remplace intégralement les lignes de l'écriture (delete-then-recreate) — pas de PATCH ligne par ligne en Phase 1. */
public record EcritureModificationForm(
        @NotNull LocalDate date,
        String reference,
        @NotBlank String libelle,
        @NotEmpty @Valid List<LigneEcritureForm> lignes
) {
}
