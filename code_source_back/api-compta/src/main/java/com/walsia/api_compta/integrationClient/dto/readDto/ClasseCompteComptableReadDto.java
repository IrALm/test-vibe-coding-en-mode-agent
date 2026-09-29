package com.walsia.api_compta.integrationClient.dto.readDto;

public record ClasseCompteComptableReadDto(
        String id,
        int numero,
        String titre,
        String description,
        long nombreComptes
) {
}
