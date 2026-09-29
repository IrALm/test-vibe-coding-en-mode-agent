package com.walsia.api_compta.integrationClient.service.impl;

import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.dto.formDto.TiersAssociationCompteForm;
import com.walsia.api_compta.integrationClient.dto.formDto.TiersCreationForm;
import com.walsia.api_compta.integrationClient.dto.formDto.TiersModificationForm;
import com.walsia.api_compta.integrationClient.dto.formDto.TiersSearchForm;
import com.walsia.api_compta.integrationClient.dto.readDto.TiersPageReadDto;
import com.walsia.api_compta.integrationClient.dto.readDto.TiersReadDto;
import com.walsia.api_compta.integrationClient.dto.readDto.TiersRecapReadDto;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.referentiel.CompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.PlanComptable;
import com.walsia.api_compta.integrationClient.entity.tiers.Tiers;
import com.walsia.api_compta.integrationClient.entity.tiers.TiersSortEnum;
import com.walsia.api_compta.integrationClient.entity.tiers.TypeTiers;
import com.walsia.api_compta.integrationClient.mapper.TiersMapper;
import com.walsia.api_compta.integrationClient.repository.CompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.PlanComptableRepository;
import com.walsia.api_compta.integrationClient.repository.TiersRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.integrationClient.service.interfaces.TiersService;
import com.walsia.api_compta.integrationClient.specification.TiersSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TiersServiceImpl implements TiersService {

    private final UtilisateurRepository utilisateurRepository;
    private final TiersRepository tiersRepository;
    private final PlanComptableRepository planComptableRepository;
    private final CompteComptableRepository compteComptableRepository;
    private final TiersMapper tiersMapper;
    private final TiersSpecification tiersSpecification;

    public TiersServiceImpl(
            UtilisateurRepository utilisateurRepository,
            TiersRepository tiersRepository,
            PlanComptableRepository planComptableRepository,
            CompteComptableRepository compteComptableRepository,
            TiersMapper tiersMapper,
            TiersSpecification tiersSpecification) {
        this.utilisateurRepository = utilisateurRepository;
        this.tiersRepository = tiersRepository;
        this.planComptableRepository = planComptableRepository;
        this.compteComptableRepository = compteComptableRepository;
        this.tiersMapper = tiersMapper;
        this.tiersSpecification = tiersSpecification;
    }

    @Override
    @Transactional(readOnly = true)
    public TiersPageReadDto rechercherTiers(String keycloakIdAppelant, TiersSearchForm form) {
        Entite entite = entiteAppelant(keycloakIdAppelant);

        Pageable pageable = PageRequest.of(form.page(), form.size(), TiersSortEnum.resolve(form.sort()));
        Page<TiersReadDto> page = tiersRepository
                .findAll(tiersSpecification.build(entite.getId(), form), pageable)
                .map(tiersMapper::toReadDto);

        return TiersPageReadDto.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public TiersRecapReadDto obtenirRecap(String keycloakIdAppelant) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        String entiteId = entite.getId();

        long total = tiersRepository.countByEntite_Id(entiteId);
        Map<TypeTiers, Long> parType = new LinkedHashMap<>();
        for (TypeTiers type : TypeTiers.values()) {
            parType.put(type, tiersRepository.countByEntite_IdAndType(entiteId, type));
        }
        long sansCompteAssocie = tiersRepository.countByEntite_IdAndCompteAssocieIsNull(entiteId);

        return new TiersRecapReadDto(total, parType, sansCompteAssocie);
    }

    @Override
    @Transactional(readOnly = true)
    public TiersReadDto obtenirDetail(String keycloakIdAppelant, String tiersId) {
        return tiersMapper.toReadDto(tiersDeLEntite(keycloakIdAppelant, tiersId));
    }

    @Override
    @Transactional
    public TiersReadDto creerTiers(String keycloakIdAppelant, TiersCreationForm form) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        PlanComptable planActif = planActifDuReferentiel(entite.getReferentielComptable().getId());
        CompteComptable compteAssocie = compteAssocieValide(form.compteAssocieId(), planActif.getId(), entite.getId());

        Tiers tiers = Tiers.builder()
                .type(form.type())
                .raisonSociale(form.raisonSociale())
                .nomContact(form.nomContact())
                .email(form.email())
                .telephone(form.telephone())
                .adresse(form.adresse())
                .numeroFiscal(form.numeroFiscal())
                .intitulePoste(form.intitulePoste())
                .actif(form.actif())
                .entite(entite)
                .compteAssocie(compteAssocie)
                .build();

        return tiersMapper.toReadDto(tiersRepository.save(tiers));
    }

    @Override
    @Transactional
    public TiersReadDto modifierTiers(String keycloakIdAppelant, String tiersId, TiersModificationForm form) {
        Tiers tiers = tiersDeLEntite(keycloakIdAppelant, tiersId);

        if (form.type() != null) {
            tiers.setType(form.type());
        }
        if (form.raisonSociale() != null) {
            tiers.setRaisonSociale(form.raisonSociale());
        }
        if (form.nomContact() != null) {
            tiers.setNomContact(form.nomContact());
        }
        if (form.email() != null) {
            tiers.setEmail(form.email());
        }
        if (form.telephone() != null) {
            tiers.setTelephone(form.telephone());
        }
        if (form.adresse() != null) {
            tiers.setAdresse(form.adresse());
        }
        if (form.numeroFiscal() != null) {
            tiers.setNumeroFiscal(form.numeroFiscal());
        }
        if (form.intitulePoste() != null) {
            tiers.setIntitulePoste(form.intitulePoste());
        }
        if (form.actif() != null) {
            tiers.setActif(form.actif());
        }

        return tiersMapper.toReadDto(tiersRepository.save(tiers));
    }

    @Override
    @Transactional
    public TiersReadDto associerCompte(String keycloakIdAppelant, String tiersId, TiersAssociationCompteForm form) {
        Tiers tiers = tiersDeLEntite(keycloakIdAppelant, tiersId);
        Entite entite = tiers.getEntite();
        PlanComptable planActif = planActifDuReferentiel(entite.getReferentielComptable().getId());

        CompteComptable compteAssocie = compteAssocieValide(form.compteAssocieId(), planActif.getId(), entite.getId());
        tiers.setCompteAssocie(compteAssocie);

        return tiersMapper.toReadDto(tiersRepository.save(tiers));
    }

    @Override
    @Transactional
    public TiersReadDto desactiverTiers(String keycloakIdAppelant, String tiersId) {
        Tiers tiers = tiersDeLEntite(keycloakIdAppelant, tiersId);
        tiers.setActif(false);
        return tiersMapper.toReadDto(tiersRepository.save(tiers));
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

    /** Charge le tiers ciblé et vérifie son appartenance à l'entreprise de l'appelant - 404 sinon, pour ne pas confirmer l'existence d'un tiers d'une autre entreprise. */
    private Tiers tiersDeLEntite(String keycloakIdAppelant, String tiersId) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        Tiers tiers = tiersRepository.findById(tiersId)
                .orElseThrow(() -> new RessourceIntrouvableException("Tiers introuvable"));
        if (!tiers.getEntite().getId().equals(entite.getId())) {
            throw new RessourceIntrouvableException("Tiers introuvable");
        }
        return tiers;
    }

    /** compteAssocieId absent = pas de compte associé (valide). S'il est fourni, doit exister, appartenir au plan actif
     * et être visible par l'entreprise (standard ou spécifique à l'entreprise) - 404 sinon (même logique de non-divulgation
     * que CompteComptableCreationServiceImpl). */
    private CompteComptable compteAssocieValide(String compteAssocieId, String planComptableId, String entiteId) {
        if (compteAssocieId == null) {
            return null;
        }
        CompteComptable compte = compteComptableRepository.findById(compteAssocieId)
                .orElseThrow(() -> new RessourceIntrouvableException("Compte associé introuvable"));
        boolean visible = compte.getEntite() == null || compte.getEntite().getId().equals(entiteId);
        if (!compte.getPlanComptable().getId().equals(planComptableId) || !visible) {
            throw new RessourceIntrouvableException("Compte associé introuvable");
        }
        return compte;
    }
}
