package com.walsia.api_compta.integrationClient.service.interfaces;

import com.walsia.api_compta.integrationClient.dto.formDto.TiersAssociationCompteForm;
import com.walsia.api_compta.integrationClient.dto.formDto.TiersCreationForm;
import com.walsia.api_compta.integrationClient.dto.formDto.TiersModificationForm;
import com.walsia.api_compta.integrationClient.dto.formDto.TiersSearchForm;
import com.walsia.api_compta.integrationClient.dto.readDto.TiersPageReadDto;
import com.walsia.api_compta.integrationClient.dto.readDto.TiersReadDto;
import com.walsia.api_compta.integrationClient.dto.readDto.TiersRecapReadDto;

public interface TiersService {

    TiersPageReadDto rechercherTiers(String keycloakIdAppelant, TiersSearchForm form);

    TiersRecapReadDto obtenirRecap(String keycloakIdAppelant);

    TiersReadDto obtenirDetail(String keycloakIdAppelant, String tiersId);

    TiersReadDto creerTiers(String keycloakIdAppelant, TiersCreationForm form);

    TiersReadDto modifierTiers(String keycloakIdAppelant, String tiersId, TiersModificationForm form);

    TiersReadDto associerCompte(String keycloakIdAppelant, String tiersId, TiersAssociationCompteForm form);

    TiersReadDto desactiverTiers(String keycloakIdAppelant, String tiersId);
}
