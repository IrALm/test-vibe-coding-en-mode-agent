package com.walsia.api_compta.integrationClient.service.impl;

import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.dto.readDto.UtilisateurReadDto;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.integrationClient.service.interfaces.UtilisateurConsultationService;
import org.springframework.stereotype.Service;

@Service
public class UtilisateurConsultationServiceImpl implements UtilisateurConsultationService {

    private final UtilisateurRepository utilisateurRepository;

    public UtilisateurConsultationServiceImpl(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public UtilisateurReadDto obtenirProfilConnecte(String keycloakId) {
        Utilisateur utilisateur = utilisateurRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));

        return new UtilisateurReadDto(
                null, utilisateur.getNom(), utilisateur.getPostNom(), utilisateur.getPrenom(),
                utilisateur.getEmail(), utilisateur.getRole(),
                false, utilisateur.isEmailVerifie(), false, null, null);
    }
}
