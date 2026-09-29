package com.walsia.api_compta.integrationClient.dto.formDto;

import com.walsia.api_compta.integrationClient.entity.utilisateur.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Ajout d'un utilisateur (comptable, caissier...) à l'entreprise de l'admin qui fait la demande. */
public record UtilisateurCreationForm(
        @NotBlank
        String nom,

        String postNom,

        @NotBlank
        String prenom,

        @NotBlank
        @Email
        String email,

        @NotNull
        Role role
) {
}
