package com.walsia.api_compta.moteurComptable.service.impl;

import com.walsia.api_compta.exception.ConflitException;
import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.referentiel.CompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.PlanComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.integrationClient.repository.CompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.PlanComptableRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.moteurComptable.dto.formDto.ExerciceCreationForm;
import com.walsia.api_compta.moteurComptable.dto.readDto.ClotureCheckReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.ExerciceReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.ResultatReadDto;
import com.walsia.api_compta.moteurComptable.entity.ecriture.Ecriture;
import com.walsia.api_compta.moteurComptable.entity.ecriture.LigneEcriture;
import com.walsia.api_compta.moteurComptable.entity.ecriture.StatutEcriture;
import com.walsia.api_compta.moteurComptable.entity.exercice.ExerciceComptable;
import com.walsia.api_compta.moteurComptable.entity.exercice.StatutExercice;
import com.walsia.api_compta.moteurComptable.entity.journal.Journal;
import com.walsia.api_compta.moteurComptable.mapper.ExerciceMapper;
import com.walsia.api_compta.moteurComptable.repository.EcritureRepository;
import com.walsia.api_compta.moteurComptable.repository.ExerciceComptableRepository;
import com.walsia.api_compta.moteurComptable.repository.JournalRepository;
import com.walsia.api_compta.moteurComptable.repository.LigneEcritureRepository;
import com.walsia.api_compta.moteurComptable.service.interfaces.ExerciceComptableService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ExerciceComptableServiceImpl implements ExerciceComptableService {

    private static final long DUREE_MAX_JOURS_PREMIER_EXERCICE = 731; // ~24 mois
    private static final long DUREE_MAX_JOURS_EXERCICE = 366; // ~12 mois (marge année bissextile)

    /** Numéros SYSCOHADA du compte de résultat, seedés en V15 (classe 1, capitaux propres). */
    private static final String COMPTE_RESULTAT_BENEFICE = "111";
    private static final String COMPTE_RESULTAT_PERTE = "119";

    private final UtilisateurRepository utilisateurRepository;
    private final ExerciceComptableRepository exerciceComptableRepository;
    private final EcritureRepository ecritureRepository;
    private final LigneEcritureRepository ligneEcritureRepository;
    private final PlanComptableRepository planComptableRepository;
    private final CompteComptableRepository compteComptableRepository;
    private final JournalRepository journalRepository;
    private final JournalProvisioningService journalProvisioningService;
    private final ExerciceMapper exerciceMapper;

    public ExerciceComptableServiceImpl(UtilisateurRepository utilisateurRepository,
                                         ExerciceComptableRepository exerciceComptableRepository,
                                         EcritureRepository ecritureRepository,
                                         LigneEcritureRepository ligneEcritureRepository,
                                         PlanComptableRepository planComptableRepository,
                                         CompteComptableRepository compteComptableRepository,
                                         JournalRepository journalRepository,
                                         JournalProvisioningService journalProvisioningService,
                                         ExerciceMapper exerciceMapper) {
        this.utilisateurRepository = utilisateurRepository;
        this.exerciceComptableRepository = exerciceComptableRepository;
        this.ecritureRepository = ecritureRepository;
        this.ligneEcritureRepository = ligneEcritureRepository;
        this.planComptableRepository = planComptableRepository;
        this.compteComptableRepository = compteComptableRepository;
        this.journalRepository = journalRepository;
        this.journalProvisioningService = journalProvisioningService;
        this.exerciceMapper = exerciceMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExerciceReadDto> listerExercices(String keycloakIdAppelant) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        return exerciceComptableRepository.findByEntite_IdOrderByDateDebutDesc(entite.getId())
                .stream().map(exerciceMapper::toReadDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExerciceReadDto obtenirExerciceOuvert(String keycloakIdAppelant) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        return exerciceComptableRepository.findByEntite_IdAndStatut(entite.getId(), StatutExercice.OUVERT)
                .map(exerciceMapper::toReadDto)
                .orElseThrow(() -> new RessourceIntrouvableException("Aucun exercice ouvert"));
    }

    @Override
    @Transactional(readOnly = true)
    public ResultatReadDto obtenirResultat(String keycloakIdAppelant, String exerciceId) {
        ExerciceComptable exercice = exerciceDeLEntite(keycloakIdAppelant, exerciceId);
        BigDecimal resultat = ligneEcritureRepository.calculerResultat(exercice.getId());
        return new ResultatReadDto(exercice.getId(), resultat);
    }

    @Override
    @Transactional
    public ExerciceReadDto creerExercice(String keycloakIdAppelant, ExerciceCreationForm form) {
        Entite entite = entiteAppelant(keycloakIdAppelant);

        if (!form.dateFin().isAfter(form.dateDebut())) {
            throw new ConflitException("La date de fin doit être postérieure à la date de début");
        }

        List<ExerciceComptable> exercicesExistants = exerciceComptableRepository
                .findByEntite_IdOrderByDateDebutDesc(entite.getId());
        boolean chevauche = exercicesExistants.stream().anyMatch(e ->
                !form.dateDebut().isAfter(e.getDateFin()) && !form.dateFin().isBefore(e.getDateDebut()));
        if (chevauche) {
            throw new ConflitException("Les dates chevauchent un exercice existant");
        }

        long dureeJours = ChronoUnit.DAYS.between(form.dateDebut(), form.dateFin()) + 1;
        long dureeMax = exercicesExistants.isEmpty() ? DUREE_MAX_JOURS_PREMIER_EXERCICE : DUREE_MAX_JOURS_EXERCICE;
        if (dureeJours > dureeMax) {
            throw new ConflitException("La durée de l'exercice dépasse le maximum autorisé");
        }

        if (exerciceComptableRepository.existsByEntite_IdAndStatut(entite.getId(), StatutExercice.OUVERT)) {
            throw new ConflitException("Un exercice est déjà ouvert pour cette entreprise");
        }

        ExerciceComptable exercice = ExerciceComptable.builder()
                .dateDebut(form.dateDebut())
                .dateFin(form.dateFin())
                .statut(StatutExercice.OUVERT)
                .entite(entite)
                .build();

        try {
            exercice = exerciceComptableRepository.save(exercice);
        } catch (DataIntegrityViolationException e) {
            // Filet de sécurité : course concurrente sur l'index unique partiel (un seul OUVERT par entité).
            throw new ConflitException("Un exercice est déjà ouvert pour cette entreprise");
        }

        return exerciceMapper.toReadDto(exercice);
    }

    @Override
    @Transactional(readOnly = true)
    public ClotureCheckReadDto verifierCloture(String keycloakIdAppelant, String exerciceId) {
        ExerciceComptable exercice = exerciceDeLEntite(keycloakIdAppelant, exerciceId);
        return calculerClotureCheck(exercice);
    }

    @Override
    @Transactional
    public ExerciceReadDto cloturerExercice(String keycloakIdAppelant, String exerciceId) {
        Utilisateur appelant = utilisateurAppelant(keycloakIdAppelant);
        ExerciceComptable exercice = exerciceDe(appelant.getEntite(), exerciceId);
        if (exercice.getStatut() != StatutExercice.OUVERT) {
            throw new ConflitException("Cet exercice n'est pas ouvert");
        }
        // Revérifié côté serveur : jamais fait confiance à un appel précédent à verifierCloture.
        ClotureCheckReadDto check = calculerClotureCheck(exercice);
        if (!check.cloturable()) {
            throw new ConflitException("Des écritures restent en brouillon ou en attente de validation sur cet exercice");
        }

        genererEcritureClotureSiNecessaire(exercice, appelant);

        exercice.setStatut(StatutExercice.CLOS);
        return exerciceMapper.toReadDto(exerciceComptableRepository.save(exercice));
    }

    /**
     * Solde les comptes de gestion (classes 6/7/8) mouvementés sur l'exercice vers le compte de
     * résultat (111 bénéfice / 119 perte, seedés en V15) : chaque compte de gestion reçoit une
     * ligne de sens opposé à son solde net pour être ramené à zéro, et le compte de résultat
     * reçoit la ligne de contrepartie. Sans ça, le résultat de l'exercice n'atterrit jamais sur
     * le bilan et la balance de l'exercice suivant ne peut jamais s'équilibrer (§1.6 critère 5/6
     * du cahier des charges). Ne génère rien si le résultat est exactement nul.
     */
    private void genererEcritureClotureSiNecessaire(ExerciceComptable exercice, Utilisateur appelant) {
        List<Object[]> soldesGestion = ligneEcritureRepository.calculerSoldesGestion(exercice.getId());
        if (soldesGestion.isEmpty()) {
            return;
        }
        BigDecimal resultat = ligneEcritureRepository.calculerResultat(exercice.getId());
        if (resultat.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        Entite entite = exercice.getEntite();
        PlanComptable planActif = planComptableRepository
                .findByReferentielComptable_IdAndActifTrue(entite.getReferentielComptable().getId())
                .orElseThrow(() -> new RessourceIntrouvableException("Plan comptable actif introuvable"));
        String numeroCompteResultat = resultat.signum() > 0 ? COMPTE_RESULTAT_BENEFICE : COMPTE_RESULTAT_PERTE;
        CompteComptable compteResultat = compteComptableRepository
                .findByPlanComptable_IdAndNumero(planActif.getId(), numeroCompteResultat)
                .orElseThrow(() -> new ConflitException(
                        "Compte de résultat " + numeroCompteResultat + " introuvable dans le plan comptable — impossible de clôturer"));

        Journal journal = journalProvisioningService.journalGeneralDe(entite);
        Journal journalVerrouille = journalRepository.findByIdForUpdate(journal.getId())
                .orElseThrow(() -> new RessourceIntrouvableException("Journal introuvable"));
        int numero = journalVerrouille.getDernierNumero() + 1;
        journalVerrouille.setDernierNumero(numero);
        journalRepository.save(journalVerrouille);

        Ecriture ecritureCloture = Ecriture.builder()
                .date(exercice.getDateFin())
                .libelle("Clôture de l'exercice — solde des comptes de gestion")
                .statut(StatutEcriture.VALIDEE)
                .numero(String.format("%06d", numero))
                .journal(journal)
                .exercice(exercice)
                .entite(entite)
                .creePar(appelant)
                .creeLe(LocalDateTime.now())
                .validePar(appelant)
                .valideLe(LocalDateTime.now())
                .genereParCloture(true)
                .build();

        for (Object[] soldeGestion : soldesGestion) {
            String compteId = (String) soldeGestion[0];
            BigDecimal soldeNet = (BigDecimal) soldeGestion[1];
            CompteComptable compte = compteComptableRepository.findById(compteId)
                    .orElseThrow(() -> new RessourceIntrouvableException("Compte comptable introuvable"));
            ecritureCloture.getLignes().add(LigneEcriture.builder()
                    .ecriture(ecritureCloture)
                    .compte(compte)
                    .sens(soldeNet.signum() > 0 ? SensCompte.DEBIT : SensCompte.CREDIT)
                    .montant(soldeNet.abs())
                    .libelle("Solde de clôture")
                    .build());
        }
        ecritureCloture.getLignes().add(LigneEcriture.builder()
                .ecriture(ecritureCloture)
                .compte(compteResultat)
                .sens(resultat.signum() > 0 ? SensCompte.CREDIT : SensCompte.DEBIT)
                .montant(resultat.abs())
                .libelle("Résultat de l'exercice")
                .build());

        ecritureRepository.save(ecritureCloture);
    }

    private ClotureCheckReadDto calculerClotureCheck(ExerciceComptable exercice) {
        long brouillon = ecritureRepository.countByExercice_IdAndStatut(exercice.getId(), StatutEcriture.BROUILLON);
        long enAttente = ecritureRepository.countByExercice_IdAndStatut(exercice.getId(), StatutEcriture.EN_ATTENTE);
        return new ClotureCheckReadDto(brouillon, enAttente, brouillon == 0 && enAttente == 0);
    }

    private Utilisateur utilisateurAppelant(String keycloakIdAppelant) {
        return utilisateurRepository.findByKeycloakId(keycloakIdAppelant)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));
    }

    private Entite entiteAppelant(String keycloakIdAppelant) {
        return utilisateurAppelant(keycloakIdAppelant).getEntite();
    }

    /** Charge l'exercice ciblé et vérifie son appartenance à l'entreprise de l'appelant - 404 sinon,
     *  pour ne pas confirmer l'existence d'un exercice d'une autre entreprise. */
    private ExerciceComptable exerciceDeLEntite(String keycloakIdAppelant, String exerciceId) {
        return exerciceDe(entiteAppelant(keycloakIdAppelant), exerciceId);
    }

    private ExerciceComptable exerciceDe(Entite entite, String exerciceId) {
        ExerciceComptable exercice = exerciceComptableRepository.findById(exerciceId)
                .orElseThrow(() -> new RessourceIntrouvableException("Exercice introuvable"));
        if (!exercice.getEntite().getId().equals(entite.getId())) {
            throw new RessourceIntrouvableException("Exercice introuvable");
        }
        return exercice;
    }
}
