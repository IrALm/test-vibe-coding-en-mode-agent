package com.walsia.api_compta.integrationClient.service.interfaces;

import com.walsia.api_compta.integrationClient.dto.formDto.CompteComptableCreationForm;
import com.walsia.api_compta.integrationClient.dto.readDto.CompteComptableReadDto;
import com.walsia.api_compta.integrationClient.dto.readDto.CompteOptionReadDto;

import java.util.List;

public interface CompteComptableCreationService {

    List<CompteOptionReadDto> listerComptesDeClasse(String keycloakIdAppelant, String classeId);

    CompteComptableReadDto creerCompte(String keycloakIdAppelant, CompteComptableCreationForm form);

    boolean numeroExiste(String keycloakIdAppelant, String numero);
}
