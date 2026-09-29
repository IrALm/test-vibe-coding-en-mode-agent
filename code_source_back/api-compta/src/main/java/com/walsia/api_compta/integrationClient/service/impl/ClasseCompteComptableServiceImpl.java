package com.walsia.api_compta.integrationClient.service.impl;

import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.dto.formDto.CompteComptableSearchForm;
import com.walsia.api_compta.integrationClient.dto.readDto.ClasseCompteComptableReadDto;
import com.walsia.api_compta.integrationClient.dto.readDto.CompteComptablePageReadDto;
import com.walsia.api_compta.integrationClient.dto.readDto.CompteComptableReadDto;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.referentiel.ClasseCompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.PlanComptable;
import com.walsia.api_compta.integrationClient.mapper.ClasseCompteComptableMapper;
import com.walsia.api_compta.integrationClient.mapper.CompteComptableMapper;
import com.walsia.api_compta.integrationClient.repository.ClasseCompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.CompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.PlanComptableRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.integrationClient.service.interfaces.ClasseCompteComptableService;
import com.walsia.api_compta.integrationClient.specification.ClasseCompteComptableSpecification;
import com.walsia.api_compta.integrationClient.specification.CompteComptableSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClasseCompteComptableServiceImpl implements ClasseCompteComptableService {

    private final UtilisateurRepository utilisateurRepository;
    private final PlanComptableRepository planComptableRepository;
    private final ClasseCompteComptableRepository classeCompteComptableRepository;
    private final CompteComptableRepository compteComptableRepository;
    private final ClasseCompteComptableMapper classeCompteComptableMapper;
    private final CompteComptableMapper compteComptableMapper;
    private final ClasseCompteComptableSpecification classeCompteComptableSpecification;
    private final CompteComptableSpecification compteComptableSpecification;

    public ClasseCompteComptableServiceImpl(
            UtilisateurRepository utilisateurRepository,
            PlanComptableRepository planComptableRepository,
            ClasseCompteComptableRepository classeCompteComptableRepository,
            CompteComptableRepository compteComptableRepository,
            ClasseCompteComptableMapper classeCompteComptableMapper,
            CompteComptableMapper compteComptableMapper,
            ClasseCompteComptableSpecification classeCompteComptableSpecification,
            CompteComptableSpecification compteComptableSpecification) {
        this.utilisateurRepository = utilisateurRepository;
        this.planComptableRepository = planComptableRepository;
        this.classeCompteComptableRepository = classeCompteComptableRepository;
        this.compteComptableRepository = compteComptableRepository;
        this.classeCompteComptableMapper = classeCompteComptableMapper;
        this.compteComptableMapper = compteComptableMapper;
        this.classeCompteComptableSpecification = classeCompteComptableSpecification;
        this.compteComptableSpecification = compteComptableSpecification;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClasseCompteComptableReadDto> listerClasses(String keycloakIdAppelant, String q) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        String referentielId = entite.getReferentielComptable().getId();
        PlanComptable planActif = planActifDuReferentiel(referentielId);

        List<ClasseCompteComptable> classes = classeCompteComptableRepository.findAll(
                classeCompteComptableSpecification.build(referentielId, planActif.getId(), entite.getId(), q),
                Sort.by(Sort.Direction.ASC, "numero"));

        return classes.stream()
                .map(classe -> avecNombreComptes(classe, planActif.getId(), entite.getId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompteComptablePageReadDto rechercherComptes(
            String keycloakIdAppelant, String classeId, CompteComptableSearchForm form) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        ClasseCompteComptable classe = classeDuReferentiel(classeId, entite.getReferentielComptable().getId());
        PlanComptable planActif = planActifDuReferentiel(entite.getReferentielComptable().getId());

        Pageable pageable = PageRequest.of(form.page(), form.size(), Sort.by(Sort.Direction.ASC, "numero"));
        Page<CompteComptableReadDto> page = compteComptableRepository
                .findAll(
                        compteComptableSpecification.build(classe.getId(), planActif.getId(), entite.getId(), form),
                        pageable)
                .map(compteComptableMapper::toReadDto);

        return CompteComptablePageReadDto.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public CompteComptablePageReadDto rechercherComptesGlobal(String keycloakIdAppelant, CompteComptableSearchForm form) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        PlanComptable planActif = planActifDuReferentiel(entite.getReferentielComptable().getId());

        Pageable pageable = PageRequest.of(form.page(), form.size(), Sort.by(Sort.Direction.ASC, "numero"));
        Page<CompteComptableReadDto> page = compteComptableRepository
                .findAll(
                        compteComptableSpecification.build(null, planActif.getId(), entite.getId(), form),
                        pageable)
                .map(compteComptableMapper::toReadDto);

        return CompteComptablePageReadDto.from(page);
    }

    private ClasseCompteComptableReadDto avecNombreComptes(ClasseCompteComptable classe, String planComptableId, String entiteId) {
        ClasseCompteComptableReadDto base = classeCompteComptableMapper.toReadDto(classe);
        long nombreComptes = compteComptableRepository.countVisiblesParClasse(classe.getId(), planComptableId, entiteId);
        return new ClasseCompteComptableReadDto(base.id(), base.numero(), base.titre(), base.description(), nombreComptes);
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
}
