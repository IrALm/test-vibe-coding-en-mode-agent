package com.walsia.api_compta.integrationClient.dto.readDto;

import org.springframework.data.domain.Page;

import java.util.List;

public record UtilisateurPageReadDto(
        List<UtilisateurReadDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean premierePage,
        boolean dernierePage
) {
    public static UtilisateurPageReadDto from(Page<UtilisateurReadDto> page) {
        return new UtilisateurPageReadDto(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
