package com.walsia.api_compta.moteurComptable.dto.readDto;

public record ClotureCheckReadDto(long nombreBrouillon, long nombreEnAttente, boolean cloturable) {
}
