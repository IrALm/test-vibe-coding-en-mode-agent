package com.walsia.api_compta.integrationClient.service.interfaces;

import com.walsia.api_compta.integrationClient.dto.formDto.UtilisateurCreationForm;
import com.walsia.api_compta.integrationClient.dto.formDto.UtilisateurSearchForm;
import com.walsia.api_compta.integrationClient.dto.readDto.UtilisateurPageReadDto;
import com.walsia.api_compta.integrationClient.dto.readDto.UtilisateurReadDto;

/**
 * Gestion des utilisateurs d'une entreprise par son admin : ajout, recherche paginée,
 * activation/désactivation. Toujours scopé à l'entité de l'appelant (isolation tenant) -
 * un admin ne peut jamais voir ni agir sur les utilisateurs d'une autre entreprise.
 */
public interface UtilisateurGestionService {

    UtilisateurReadDto ajouterUtilisateur(String keycloakIdAppelant, UtilisateurCreationForm form);

    UtilisateurPageReadDto rechercherUtilisateurs(String keycloakIdAppelant, UtilisateurSearchForm form);

    UtilisateurReadDto activer(String keycloakIdAppelant, String utilisateurId);

    UtilisateurReadDto desactiver(String keycloakIdAppelant, String utilisateurId);
}
