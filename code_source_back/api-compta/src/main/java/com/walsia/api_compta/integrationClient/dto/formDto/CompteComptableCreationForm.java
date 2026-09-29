package com.walsia.api_compta.integrationClient.dto.formDto;

import com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Création d'un compte comptable custom, rattaché au plan actif de l'entreprise appelante (résolu côté serveur). */
public record CompteComptableCreationForm(
        @NotBlank
        @Pattern(regexp = "\\d{1,10}", message = "Le numéro doit contenir uniquement des chiffres (10 caractères maximum)")
        String numero,

        @NotBlank
        String libelle,

        @NotBlank
        String classeCompteComptableId,

        String parentId,

        @NotNull
        SensCompte sensNormal,

        Boolean lettrable,
        Boolean actif
) {
    public CompteComptableCreationForm {
        lettrable = lettrable != null ? lettrable : false;
        actif = actif != null ? actif : true;
    }
}
