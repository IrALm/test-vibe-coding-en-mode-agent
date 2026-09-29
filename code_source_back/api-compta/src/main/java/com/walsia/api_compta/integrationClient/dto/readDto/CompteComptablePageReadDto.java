package com.walsia.api_compta.integrationClient.dto.readDto;

import org.springframework.data.domain.Page;

import java.util.List;

public record CompteComptablePageReadDto(
        List<CompteComptableReadDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean premierePage,
        boolean dernierePage
) {
    public static CompteComptablePageReadDto from(Page<CompteComptableReadDto> page) {
        return new CompteComptablePageReadDto(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
