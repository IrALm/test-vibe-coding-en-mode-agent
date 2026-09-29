package com.walsia.api_compta.authentification.dto.formDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginForm(
        @NotBlank @Email String email,
        @NotBlank String motDePasse
) {
}
