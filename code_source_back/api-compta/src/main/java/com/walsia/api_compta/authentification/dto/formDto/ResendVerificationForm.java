package com.walsia.api_compta.authentification.dto.formDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationForm(
        @NotBlank @Email String email
) {
}
