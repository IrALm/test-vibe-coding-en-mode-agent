package com.walsia.api_compta.integrationClient.dto.readDto;

import com.walsia.api_compta.integrationClient.entity.tiers.TypeTiers;

import java.util.Map;

public record TiersRecapReadDto(
        long total,
        Map<TypeTiers, Long> parType,
        long sansCompteAssocie
) {
}
