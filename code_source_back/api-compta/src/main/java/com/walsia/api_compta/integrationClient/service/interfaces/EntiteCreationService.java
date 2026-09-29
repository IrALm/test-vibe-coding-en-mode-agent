package com.walsia.api_compta.integrationClient.service.interfaces;

import com.walsia.api_compta.integrationClient.dto.formDto.EntiteCreationForm;
import com.walsia.api_compta.integrationClient.dto.readDto.EntiteCreeeReadDto;

public interface EntiteCreationService {

    /**
     * Crée une Entite et son administrateur (chef d'entreprise) : compte Keycloak
     * avec mot de passe temporaire, compte local rattaché, envoi des emails
     * de bienvenue et de vérification.
     */
    EntiteCreeeReadDto creerEntiteEtAdmin(EntiteCreationForm form);
}
