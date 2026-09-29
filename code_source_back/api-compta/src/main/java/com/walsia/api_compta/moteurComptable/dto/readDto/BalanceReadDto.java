package com.walsia.api_compta.moteurComptable.dto.readDto;

import java.math.BigDecimal;
import java.util.List;

public record BalanceReadDto(List<BalanceLigneReadDto> lignes, BigDecimal totalDebit, BigDecimal totalCredit) {
}
