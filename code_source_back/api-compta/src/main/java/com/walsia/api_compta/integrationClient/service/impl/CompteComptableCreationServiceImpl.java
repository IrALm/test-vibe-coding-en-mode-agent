package com.walsia.api_compta.integrationClient.service.impl;

import com.walsia.api_compta.exception.ConflitException;
import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.dto.formDto.CompteComptableCreationForm;
import com.walsia.api_compta.integrationClient.dto.readDto.CompteComptableReadDto;
import com.walsia.api_compta.integrationClient.dto.readDto.CompteOptionReadDto;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.referentiel.ClasseCompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.CompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.PlanComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte;
import com.walsia.api_compta.integrationClient.mapper.CompteComptableMapper;
import com.walsia.api_compta.integrationClient.repository.ClasseCompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.CompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.PlanComptableRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.integrationClient.service.interfaces.CompteComptableCreationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompteComptableCreationServiceImpl implements CompteComptableCreationService {

    private final UtilisateurRepository utilisateurRepository;
    private final PlanComptableRepository planComptableRepository;
    private final ClasseCompteComptableRepository classeCompteComptableRepository;
    private final CompteComptableRepository compteComptableRepository;
    private final CompteComptableMapper compteComptableMapper;

    public CompteComptableCreationServiceImpl(
            UtilisateurRepository utilisateurRepository,
            PlanComptableRepository planComptableRepository,
            ClasseCompteComptableRepository classeCompteComptableRepository,
            CompteComptableRepository compteComptableRepository,
            CompteComptableMapper compteComptableMapper) {
        this.utilisateurRepository = utilisateurRepository;
        this.planComptableRepository = planComptableRepository;
        this.classeCompteComptableRepository = classeCompteComptableRepository;
        this.compteComptableRepository = compteComptableRepository;
        this.compteComptableMapper = compteComptableMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompteOptionReadDto> listerComptesDeClasse(String keycloakIdAppelant, String classeId) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        String referentielId = entite.getReferentielComptable().getId();
        PlanComptable planActif = planActifDuReferentiel(referentielId);
        classeDuReferentiel(classeId, referentielId);

        return compteComptableRepository.findVisiblesParClasse(classeId, planActif.getId(), entite.getId()).stream()
                .map(compteComptableMapper::toOptionReadDto)
                .toList();
    }

    @Override
    @Transactional
    public CompteComptableReadDto creerCompte(String keycloakIdAppelant, CompteComptableCreationForm form) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        String referentielId = entite.getReferentielComptable().getId();
        PlanComptable planActif = planActifDuReferentiel(referentielId);
        ClasseCompteComptable classe = classeDuReferentiel(form.classeCompteComptableId(), referentielId);
        CompteComptable parent = parentValide(form.parentId(), planActif.getId(), classe.getId());

        if (compteComptableRepository.existsByPlanComptable_IdAndNumero(planActif.getId(), form.numero())) {
            throw new ConflitException(
                    "Un compte comptable existe déjà avec ce numéro dans ce plan comptable : " + form.numero());
        }

        // Un sous-compte hérite obligatoirement du sens de son parent : la valeur envoyée par le
        // client n'est jamais celle retenue, pour ne pas dépendre d'un verrouillage uniquement côté front.
        SensCompte sensNormal = parent != null ? parent.getSensNormal() : form.sensNormal();

        CompteComptable compte = CompteComptable.builder()
                .numero(form.numero())
                .libelle(form.libelle())
                .classeCompteComptable(classe)
                .sensNormal(sensNormal)
                .lettrable(form.lettrable())
                .actif(form.actif())
                .planComptable(planActif)
                .compteParent(parent)
                .entite(entite)
                .build();

        try {
            compte = compteComptableRepository.save(compte);
        } catch (DataIntegrityViolationException e) {
            // Filet de sécurité si deux requêtes concurrentes passent le existsBy... au même instant :
            // la contrainte unique (plan_comptable_id, numero) en base tranche en dernier recours.
            throw new ConflitException(
                    "Un compte comptable existe déjà avec ce numéro dans ce plan comptable : " + form.numero());
        }

        return compteComptableMapper.toReadDto(compte);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean numeroExiste(String keycloakIdAppelant, String numero) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        PlanComptable planActif = planActifDuReferentiel(entite.getReferentielComptable().getId());
        return compteComptableRepository.existsByPlanComptable_IdAndNumero(planActif.getId(), numero);
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

    /** Vérifie que la classe appartient au référentiel de l'entreprise appelante - 404 sinon, pour ne pas confirmer l'existence d'une classe d'un autre référentiel. */
    private ClasseCompteComptable classeDuReferentiel(String classeId, String referentielComptableId) {
        ClasseCompteComptable classe = classeCompteComptableRepository.findById(classeId)
                .orElseThrow(() -> new RessourceIntrouvableException("Classe comptable introuvable"));
        if (!classe.getReferentielComptable().getId().equals(referentielComptableId)) {
            throw new RessourceIntrouvableException("Classe comptable introuvable");
        }
        return classe;
    }

    /** parentId absent = compte racine de la classe (valide). S'il est fourni, doit exister, appartenir au même
     * plan et à la même classe que le compte en cours de création - 404 sinon (même logique de non-divulgation
     * que classeDuReferentiel). */
    private CompteComptable parentValide(String parentId, String planComptableId, String classeId) {
        if (parentId == null) {
            return null;
        }
        CompteComptable parent = compteComptableRepository.findById(parentId)
                .orElseThrow(() -> new RessourceIntrouvableException("Compte parent introuvable"));
        if (!parent.getPlanComptable().getId().equals(planComptableId)
                || !parent.getClasseCompteComptable().getId().equals(classeId)) {
            throw new RessourceIntrouvableException("Compte parent introuvable");
        }
        return parent;
    }
}
