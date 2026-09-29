package com.walsia.api_compta.moteurComptable.dto.readDto;

import java.math.BigDecimal;

public record BalanceLigneReadDto(
        String compteId,
        String compteNumero,
        String compteLibelle,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal soldeDebiteur,
        BigDecimal soldeCrediteur
) {
}
