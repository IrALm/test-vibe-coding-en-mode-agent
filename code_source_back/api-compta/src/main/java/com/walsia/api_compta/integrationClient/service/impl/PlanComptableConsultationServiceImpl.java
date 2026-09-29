package com.walsia.api_compta.integrationClient.service.impl;

import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.dto.readDto.PlanComptableRecapReadDto;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.referentiel.PlanComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.ReferentielComptable;
import com.walsia.api_compta.integrationClient.repository.ClasseCompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.CompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.PlanComptableRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.integrationClient.service.interfaces.PlanComptableConsultationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanComptableConsultationServiceImpl implements PlanComptableConsultationService {

    private final UtilisateurRepository utilisateurRepository;
    private final PlanComptableRepository planComptableRepository;
    private final ClasseCompteComptableRepository classeCompteComptableRepository;
    private final CompteComptableRepository compteComptableRepository;

    public PlanComptableConsultationServiceImpl(
            UtilisateurRepository utilisateurRepository,
            PlanComptableRepository planComptableRepository,
            ClasseCompteComptableRepository classeCompteComptableRepository,
            CompteComptableRepository compteComptableRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.planComptableRepository = planComptableRepository;
        this.classeCompteComptableRepository = classeCompteComptableRepository;
        this.compteComptableRepository = compteComptableRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PlanComptableRecapReadDto obtenirRecap(String keycloakIdAppelant) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        ReferentielComptable referentiel = entite.getReferentielComptable();
        PlanComptable planActif = planActifDuReferentiel(referentiel.getId());

        long nombreClasses = classeCompteComptableRepository.countByReferentielComptable_Id(referentiel.getId());
        long nombreComptes = compteComptableRepository.countVisiblesParPlan(planActif.getId(), entite.getId());

        return new PlanComptableRecapReadDto(referentiel.getCode(), referentiel.getLibelle(), nombreClasses, nombreComptes);
    }

    private Entite entiteAppelant(String keycloakIdAppelant) {
        return utilisateurRepository.findByKeycloakId(keycloakIdAppelant)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"))
                .getEntite();
    }

    private PlanComptable planActifDuReferentiel(String referentielComptableId) {
        return planComptableRepository.findByReferentielComptable_IdAndActifTrue(referentielComptableId)
                .orElseThrow(() -> new RessourceIntrouvableException("Plan comptable actif introuvable"));
    }
}
