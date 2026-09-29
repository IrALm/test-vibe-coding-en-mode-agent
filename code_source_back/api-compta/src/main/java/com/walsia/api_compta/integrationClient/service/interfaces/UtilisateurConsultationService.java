package com.walsia.api_compta.integrationClient.service.interfaces;

import com.walsia.api_compta.integrationClient.dto.readDto.UtilisateurReadDto;

/** Consultation du profil de l'utilisateur connecté (vue self-scoped, résolue depuis le JWT). */
public interface UtilisateurConsultationService {

    /** Retourne un UtilisateurReadDto partiel (nom, email, role, emailVerifie), sans id ni entiteId. */
    UtilisateurReadDto obtenirProfilConnecte(String keycloakId);
}
