package com.walsia.api_compta.moteurComptable.service.interfaces;

import com.walsia.api_compta.moteurComptable.dto.readDto.BalanceReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.GrandLivreReadDto;

public interface GrandLivreBalanceService {

    GrandLivreReadDto obtenirGrandLivre(String keycloakIdAppelant, String exerciceId, String compteId);

    BalanceReadDto obtenirBalance(String keycloakIdAppelant, String exerciceId);
}
