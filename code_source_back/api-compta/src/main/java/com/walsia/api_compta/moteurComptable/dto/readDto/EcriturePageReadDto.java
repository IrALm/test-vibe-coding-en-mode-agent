package com.walsia.api_compta.moteurComptable.dto.readDto;

import org.springframework.data.domain.Page;

import java.util.List;

public record EcriturePageReadDto(
        List<EcritureReadDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean premierePage,
        boolean dernierePage
) {
    public static EcriturePageReadDto from(Page<EcritureReadDto> page) {
        return new EcriturePageReadDto(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(),
                page.isFirst(), page.isLast());
    }
}
