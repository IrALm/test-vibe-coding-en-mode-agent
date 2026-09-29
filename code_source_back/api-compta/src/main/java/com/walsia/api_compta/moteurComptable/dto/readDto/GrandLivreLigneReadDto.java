package com.walsia.api_compta.moteurComptable.dto.readDto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GrandLivreLigneReadDto(
        LocalDate date,
        String ecritureId,
        String numero,
        String reference,
        String libelle,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal soldeProgressif
) {
}
