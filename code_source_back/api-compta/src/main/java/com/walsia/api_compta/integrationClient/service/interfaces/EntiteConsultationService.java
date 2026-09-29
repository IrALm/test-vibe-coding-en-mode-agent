package com.walsia.api_compta.integrationClient.service.interfaces;

import com.walsia.api_compta.integrationClient.dto.readDto.EntiteReadDto;

/** Consultation de l'entité de l'utilisateur connecté (vue self-scoped, résolue depuis le JWT). */
public interface EntiteConsultationService {

    EntiteReadDto obtenirEntiteConnectee(String keycloakId);
}
