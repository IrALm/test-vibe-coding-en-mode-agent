package com.walsia.api_compta.integrationClient.dto.formDto;

import com.walsia.api_compta.integrationClient.entity.tiers.TypeTiers;

public record TiersSearchForm(
        String q,
        TypeTiers type,
        Boolean actif,
        String sort,
        Integer page,
        Integer size
) {
    public TiersSearchForm {
        page = page != null ? page : 0;
        size = size != null ? size : 6;
    }
}
