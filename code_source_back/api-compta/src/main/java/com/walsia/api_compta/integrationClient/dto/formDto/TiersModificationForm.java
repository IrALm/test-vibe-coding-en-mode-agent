package com.walsia.api_compta.integrationClient.dto.formDto;

import com.walsia.api_compta.integrationClient.entity.tiers.TypeTiers;
import jakarta.validation.constraints.Email;

/** Mise à jour partielle des champs métier d'un tiers - un champ null n'est pas modifié.
 * L'association/dissociation du compte comptable passe par un endpoint dédié, pas ce formulaire. */
public record TiersModificationForm(
        TypeTiers type,
        String raisonSociale,
        String nomContact,

        @Email
        String email,

        String telephone,
        String adresse,
        String numeroFiscal,
        String intitulePoste,
        Boolean actif
) {
}
