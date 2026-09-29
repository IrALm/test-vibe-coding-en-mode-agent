package com.walsia.api_compta.integrationClient.service.impl;

import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.dto.readDto.EntiteReadDto;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.integrationClient.mapper.EntiteMapper;
import com.walsia.api_compta.integrationClient.repository.ClasseCompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.CompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.integrationClient.service.interfaces.EntiteConsultationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntiteConsultationServiceImpl implements EntiteConsultationService {

    private final UtilisateurRepository utilisateurRepository;
    private final EntiteMapper entiteMapper;
    private final ClasseCompteComptableRepository classeCompteComptableRepository;
    private final CompteComptableRepository compteComptableRepository;

    public EntiteConsultationServiceImpl(
            UtilisateurRepository utilisateurRepository,
            EntiteMapper entiteMapper,
            ClasseCompteComptableRepository classeCompteComptableRepository,
            CompteComptableRepository compteComptableRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.entiteMapper = entiteMapper;
        this.classeCompteComptableRepository = classeCompteComptableRepository;
        this.compteComptableRepository = compteComptableRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public EntiteReadDto obtenirEntiteConnectee(String keycloakId) {
        Utilisateur utilisateur = utilisateurRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));
        return avecCompteurs(utilisateur.getEntite());
    }

    /** Complète l'EntiteReadDto avec les compteurs du référentiel (classes/comptes), non portés par le mapper. */
    private EntiteReadDto avecCompteurs(Entite entite) {
        EntiteReadDto base = entiteMapper.toReadDto(entite);
        String referentielId = entite.getReferentielComptable().getId();
        long nombreClasses = classeCompteComptableRepository.countByReferentielComptable_Id(referentielId);
        long nombreComptes = compteComptableRepository.countByClasseCompteComptable_ReferentielComptable_Id(referentielId);
        return new EntiteReadDto(
                base.id(), base.raisonSociale(), base.typeEntite(), base.pays(), base.devise(),
                base.numeroIdentification(), base.dateCreation(), base.actif(),
                base.referentielComptableCode(), base.referentielComptableLibelle(),
                nombreClasses, nombreComptes);
    }
}
