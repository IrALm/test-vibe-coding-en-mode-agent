package com.walsia.api_compta.authentification.dto.formDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DefinirMotDePasseForm(
        @NotBlank @Size(min = 8, max = 100) String nouveauMotDePasse
) {
}
