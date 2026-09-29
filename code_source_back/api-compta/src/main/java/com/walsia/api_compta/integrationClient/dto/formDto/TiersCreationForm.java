package com.walsia.api_compta.integrationClient.dto.formDto;

import com.walsia.api_compta.integrationClient.entity.tiers.TypeTiers;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Création d'un tiers rattaché à l'entreprise appelante (résolue côté serveur). compteAssocieId optionnel. */
public record TiersCreationForm(
        @NotNull
        TypeTiers type,

        @NotBlank
        String raisonSociale,

        String nomContact,

        @Email
        String email,

        String telephone,
        String adresse,
        String numeroFiscal,
        String intitulePoste,
        Boolean actif,
        String compteAssocieId
) {
    public TiersCreationForm {
        actif = actif != null ? actif : true;
    }
}
