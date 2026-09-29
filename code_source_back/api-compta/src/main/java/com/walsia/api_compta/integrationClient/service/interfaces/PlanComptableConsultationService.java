package com.walsia.api_compta.integrationClient.service.interfaces;

import com.walsia.api_compta.integrationClient.dto.readDto.PlanComptableRecapReadDto;

public interface PlanComptableConsultationService {

    PlanComptableRecapReadDto obtenirRecap(String keycloakIdAppelant);
}
