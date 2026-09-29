package com.walsia.api_compta.moteurComptable.dto.readDto;

import java.math.BigDecimal;
import java.util.List;

public record GrandLivreReadDto(
        String compteId,
        String compteNumero,
        String compteLibelle,
        BigDecimal soldeOuverture,
        List<GrandLivreLigneReadDto> lignes,
        BigDecimal soldeCloture
) {
}
