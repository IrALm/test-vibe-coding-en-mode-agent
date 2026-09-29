package com.walsia.api_compta.integrationClient.dto.readDto;

import com.walsia.api_compta.integrationClient.entity.tiers.TypeTiers;

public record TiersReadDto(
        String id,
        TypeTiers type,
        String raisonSociale,
        String nomContact,
        String email,
        String telephone,
        String adresse,
        String numeroFiscal,
        String intitulePoste,
        boolean actif,
        String compteAssocieId,
        String compteAssocieNumero,
        String compteAssocieLibelle
) {
}
