package com.walsia.api_compta.integrationClient.dto.readDto;

import org.springframework.data.domain.Page;

import java.util.List;

public record TiersPageReadDto(
        List<TiersReadDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean premierePage,
        boolean dernierePage
) {
    public static TiersPageReadDto from(Page<TiersReadDto> page) {
        return new TiersPageReadDto(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
