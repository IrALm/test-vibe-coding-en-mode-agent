package com.walsia.api_compta.integrationClient.dto.formDto;

import com.walsia.api_compta.integrationClient.entity.entite.TypeEntite;
import com.walsia.api_compta.integrationClient.entity.referentiel.CodeReferentiel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Formulaire de création d'une Entite, incluant les informations de son
 * administrateur (chef d'entreprise), auto-créé avec le rôle ADMIN.
 */
public record EntiteCreationForm(

        @NotBlank
        String raisonSociale,

        @NotNull
        TypeEntite typeEntite,

        String pays,

        String devise,

        String numeroIdentification,

        @NotNull
        CodeReferentiel referentielComptableCode,

        @NotBlank
        String adminNom,

        String adminPostNom,

        @NotBlank
        String adminPrenom,

        @NotBlank
        @Email
        String adminEmail,

        String adminTelephone
) {
}
