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
import com.walsia.api_compta.moteurComptable.dto.formDto.ContrePassationForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.EcritureCreationForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.EcritureModificationForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.EcritureSearchForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.LigneEcritureForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.RenvoyerBrouillonForm;
import com.walsia.api_compta.moteurComptable.dto.readDto.EcriturePageReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.EcritureReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.EcritureStatsReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.LigneEcritureReadDto;
import com.walsia.api_compta.moteurComptable.entity.ecriture.Ecriture;
import com.walsia.api_compta.moteurComptable.entity.ecriture.LigneEcriture;
import com.walsia.api_compta.moteurComptable.entity.ecriture.StatutEcriture;
import com.walsia.api_compta.moteurComptable.entity.exercice.ExerciceComptable;
import com.walsia.api_compta.moteurComptable.entity.exercice.StatutExercice;
import com.walsia.api_compta.moteurComptable.entity.journal.Journal;
import com.walsia.api_compta.moteurComptable.repository.EcritureRepository;
import com.walsia.api_compta.moteurComptable.repository.ExerciceComptableRepository;
import com.walsia.api_compta.moteurComptable.repository.JournalRepository;
import com.walsia.api_compta.moteurComptable.service.interfaces.EcritureService;
import com.walsia.api_compta.moteurComptable.specification.EcritureSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EcritureServiceImpl implements EcritureService {

    private final UtilisateurRepository utilisateurRepository;
    private final EcritureRepository ecritureRepository;
    private final ExerciceComptableRepository exerciceComptableRepository;
    private final PlanComptableRepository planComptableRepository;
    private final CompteComptableRepository compteComptableRepository;
    private final JournalRepository journalRepository;
    private final JournalProvisioningService journalProvisioningService;
    private final EcritureSpecification ecritureSpecification;

    public EcritureServiceImpl(UtilisateurRepository utilisateurRepository,
                                EcritureRepository ecritureRepository,
                                ExerciceComptableRepository exerciceComptableRepository,
                                PlanComptableRepository planComptableRepository,
                                CompteComptableRepository compteComptableRepository,
                                JournalRepository journalRepository,
                                JournalProvisioningService journalProvisioningService,
                                EcritureSpecification ecritureSpecification) {
        this.utilisateurRepository = utilisateurRepository;
        this.ecritureRepository = ecritureRepository;
        this.exerciceComptableRepository = exerciceComptableRepository;
        this.planComptableRepository = planComptableRepository;
        this.compteComptableRepository = compteComptableRepository;
        this.journalRepository = journalRepository;
        this.journalProvisioningService = journalProvisioningService;
        this.ecritureSpecification = ecritureSpecification;
    }

    @Override
    @Transactional(readOnly = true)
    public EcriturePageReadDto rechercherEcritures(String keycloakIdAppelant, EcritureSearchForm form) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        Pageable pageable = PageRequest.of(form.page(), form.size(), resolveTri(form.sort()));
        Page<EcritureReadDto> page = ecritureRepository
                .findAll(ecritureSpecification.build(entite.getId(), form), pageable)
                .map(this::toReadDto);
        return EcriturePageReadDto.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public EcritureReadDto obtenirDetail(String keycloakIdAppelant, String ecritureId) {
        return toReadDto(ecritureDeLEntite(keycloakIdAppelant, ecritureId));
    }

    @Override
    @Transactional(readOnly = true)
    public EcritureStatsReadDto obtenirStats(String keycloakIdAppelant, String exerciceId) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        ExerciceComptable exercice = exerciceComptableRepository.findById(exerciceId)
                .orElseThrow(() -> new RessourceIntrouvableException("Exercice introuvable"));
        if (!exercice.getEntite().getId().equals(entite.getId())) {
            throw new RessourceIntrouvableException("Exercice introuvable");
        }
        long brouillon = ecritureRepository.countByExercice_IdAndStatut(exercice.getId(), StatutEcriture.BROUILLON);
        long enAttente = ecritureRepository.countByExercice_IdAndStatut(exercice.getId(), StatutEcriture.EN_ATTENTE);
        long validees = ecritureRepository.countByExercice_IdAndStatut(exercice.getId(), StatutEcriture.VALIDEE);
        return new EcritureStatsReadDto(brouillon, enAttente, validees);
    }

    @Override
    @Transactional
    public EcritureReadDto creerEcriture(String keycloakIdAppelant, EcritureCreationForm form) {
        Utilisateur appelant = utilisateurAppelant(keycloakIdAppelant);
        Entite entite = appelant.getEntite();
        ExerciceComptable exercice = exerciceOuvertDe(entite);
        validerDateDansExercice(form.date(), exercice);
        PlanComptable planActif = planActifDuReferentiel(entite.getReferentielComptable().getId());
        Journal journal = journalProvisioningService.journalGeneralDe(entite);

        Ecriture ecriture = Ecriture.builder()
                .date(form.date())
                .reference(form.reference())
                .libelle(form.libelle())
                .statut(StatutEcriture.BROUILLON)
                .journal(journal)
                .exercice(exercice)
                .entite(entite)
                .creePar(appelant)
                .creeLe(LocalDateTime.now())
                .build();

        remplirLignes(ecriture, form.lignes(), planActif, entite.getId());

        return toReadDto(ecritureRepository.save(ecriture));
    }

    @Override
    @Transactional
    public EcritureReadDto modifierEcriture(String keycloakIdAppelant, String ecritureId, EcritureModificationForm form) {
        Ecriture ecriture = ecritureDeLEntite(keycloakIdAppelant, ecritureId);
        if (ecriture.getStatut() != StatutEcriture.BROUILLON) {
            throw new ConflitException("Seule une écriture en brouillon peut être modifiée");
        }
        validerDateDansExercice(form.date(), ecriture.getExercice());
        Entite entite = ecriture.getEntite();
        PlanComptable planActif = planActifDuReferentiel(entite.getReferentielComptable().getId());

        ecriture.setDate(form.date());
        ecriture.setReference(form.reference());
        ecriture.setLibelle(form.libelle());
        ecriture.getLignes().clear();
        remplirLignes(ecriture, form.lignes(), planActif, entite.getId());

        return toReadDto(ecritureRepository.save(ecriture));
    }

    @Override
    @Transactional
    public void supprimerEcriture(String keycloakIdAppelant, String ecritureId) {
        Ecriture ecriture = ecritureDeLEntite(keycloakIdAppelant, ecritureId);
        if (ecriture.getStatut() != StatutEcriture.BROUILLON) {
            throw new ConflitException("Seule une écriture en brouillon peut être supprimée");
        }
        ecritureRepository.delete(ecriture);
    }

    @Override
    @Transactional
    public EcritureReadDto soumettre(String keycloakIdAppelant, String ecritureId) {
        Ecriture ecriture = ecritureDeLEntite(keycloakIdAppelant, ecritureId);
        if (ecriture.getStatut() != StatutEcriture.BROUILLON) {
            throw new ConflitException("Seule une écriture en brouillon peut être soumise à validation");
        }
        validerDateDansExercice(ecriture.getDate(), ecriture.getExercice());
        verifierEquilibre(ecriture);
        ecriture.setStatut(StatutEcriture.EN_ATTENTE);
        return toReadDto(ecritureRepository.save(ecriture));
    }

    @Override
    @Transactional
    public EcritureReadDto valider(String keycloakIdAppelant, String ecritureId) {
        Utilisateur appelant = utilisateurAppelant(keycloakIdAppelant);
        Ecriture ecriture = ecritureDe(appelant.getEntite(), ecritureId);
        if (ecriture.getStatut() != StatutEcriture.EN_ATTENTE) {
            throw new ConflitException("Cette écriture n'est pas en attente de validation");
        }
        // Revérification défensive : jamais fait confiance au statut "équilibrée" calculé lors de la soumission.
        verifierEquilibre(ecriture);

        Journal journal = journalRepository.findByIdForUpdate(ecriture.getJournal().getId())
                .orElseThrow(() -> new RessourceIntrouvableException("Journal introuvable"));
        int prochainNumero = journal.getDernierNumero() + 1;
        journal.setDernierNumero(prochainNumero);
        journalRepository.save(journal);

        ecriture.setNumero(String.format("%06d", prochainNumero));
        ecriture.setStatut(StatutEcriture.VALIDEE);
        ecriture.setValidePar(appelant);
        ecriture.setValideLe(LocalDateTime.now());

        // Si cette écriture est le miroir d'une contre-passation, l'originale ne devient CONTREPASSEE
        // qu'à cet instant précis - jamais dès la demande de contre-passation (cf. handoff Phase 1, §A).
        if (ecriture.getContrePassationDeId() != null) {
            Ecriture original = ecritureRepository.findById(ecriture.getContrePassationDeId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Écriture d'origine introuvable"));
            original.setStatut(StatutEcriture.CONTREPASSEE);
            original.setEcritureMiroirId(ecriture.getId());
            ecritureRepository.save(original);
        }

        return toReadDto(ecritureRepository.save(ecriture));
    }

    @Override
    @Transactional
    public EcritureReadDto renvoyerEnBrouillon(String keycloakIdAppelant, String ecritureId, RenvoyerBrouillonForm form) {
        Ecriture ecriture = ecritureDeLEntite(keycloakIdAppelant, ecritureId);
        if (ecriture.getStatut() != StatutEcriture.EN_ATTENTE) {
            throw new ConflitException("Seule une écriture en attente de validation peut être renvoyée en brouillon");
        }
        ecriture.setStatut(StatutEcriture.BROUILLON);
        ecriture.setMotifRejet(form.motif());
        return toReadDto(ecritureRepository.save(ecriture));
    }

    @Override
    @Transactional
    public EcritureReadDto contrePasser(String keycloakIdAppelant, String ecritureId, ContrePassationForm form) {
        Utilisateur appelant = utilisateurAppelant(keycloakIdAppelant);
        Entite entite = appelant.getEntite();
        Ecriture original = ecritureDe(entite, ecritureId);
        if (original.getStatut() != StatutEcriture.VALIDEE) {
            throw new ConflitException("Seule une écriture validée peut être contre-passée");
        }
        // L'exercice de l'originale peut être clôturé (raison fréquente de contre-passer) : le miroir
        // est toujours rattaché à l'exercice OUVERT courant, jamais à celui de l'écriture d'origine.
        ExerciceComptable exerciceCible = exerciceOuvertDe(entite);
        Journal journal = journalProvisioningService.journalGeneralDe(entite);

        // Date laissée à la main de l'utilisateur (pré-remplie côté front par la date de l'originale) :
        // un LocalDate.now() figé ne tombait pas forcément dans les bornes de l'exercice OUVERT courant
        // (ex. exercice 2027 déjà ouvert alors qu'on contre-passe "aujourd'hui" en 2026).
        LocalDate dateMiroir = form.date() != null ? form.date() : original.getDate();
        validerDateDansExercice(dateMiroir, exerciceCible);

        Ecriture miroir = Ecriture.builder()
                .date(dateMiroir)
                .reference(original.getReference())
                .libelle("Contre-passation de l'écriture " +
                        (original.getNumero() != null ? original.getNumero() : original.getLibelle()))
                .statut(StatutEcriture.BROUILLON)
                .journal(journal)
                .exercice(exerciceCible)
                .entite(entite)
                .creePar(appelant)
                .creeLe(LocalDateTime.now())
                .contrePassationDeId(original.getId())
                .build();

        for (LigneEcriture ligne : original.getLignes()) {
            miroir.getLignes().add(LigneEcriture.builder()
                    .ecriture(miroir)
                    .compte(ligne.getCompte())
                    .sens(ligne.getSens() == SensCompte.DEBIT ? SensCompte.CREDIT : SensCompte.DEBIT)
                    .montant(ligne.getMontant())
                    .libelle(ligne.getLibelle())
                    .build());
        }

        return toReadDto(ecritureRepository.save(miroir));
    }

    private void remplirLignes(Ecriture ecriture, List<LigneEcritureForm> formLignes, PlanComptable planActif, String entiteId) {
        if (formLignes.size() < 2) {
            throw new ConflitException("Une écriture doit comporter au moins 2 lignes");
        }
        for (LigneEcritureForm ligneForm : formLignes) {
            CompteComptable compte = compteValide(ligneForm.compteId(), planActif.getId(), entiteId);
            ecriture.getLignes().add(LigneEcriture.builder()
                    .ecriture(ecriture)
                    .compte(compte)
                    .sens(ligneForm.sens())
                    .montant(ligneForm.montant())
                    .libelle(ligneForm.libelle())
                    .build());
        }
    }

    private void verifierEquilibre(Ecriture ecriture) {
        if (totalParSens(ecriture, SensCompte.DEBIT).compareTo(totalParSens(ecriture, SensCompte.CREDIT)) != 0) {
            throw new ConflitException("L'écriture n'est pas équilibrée (total débit différent du total crédit)");
        }
    }

    private BigDecimal totalParSens(Ecriture ecriture, SensCompte sens) {
        return ecriture.getLignes().stream()
                .filter(l -> l.getSens() == sens)
                .map(LigneEcriture::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validerDateDansExercice(LocalDate date, ExerciceComptable exercice) {
        if (exercice.getStatut() != StatutExercice.OUVERT) {
            throw new ConflitException("L'exercice est clôturé");
        }
        if (date.isBefore(exercice.getDateDebut()) || date.isAfter(exercice.getDateFin())) {
            throw new ConflitException("La date de l'écriture est hors des bornes de l'exercice");
        }
    }

    private ExerciceComptable exerciceOuvertDe(Entite entite) {
        return exerciceComptableRepository.findByEntite_IdAndStatut(entite.getId(), StatutExercice.OUVERT)
                .orElseThrow(() -> new RessourceIntrouvableException("Aucun exercice ouvert"));
    }

    private PlanComptable planActifDuReferentiel(String referentielComptableId) {
        return planComptableRepository.findByReferentielComptable_IdAndActifTrue(referentielComptableId)
                .orElseThrow(() -> new RessourceIntrouvableException("Plan comptable actif introuvable"));
    }

    /** compteId doit exister, appartenir au plan actif et être visible par l'entreprise (standard ou
     *  spécifique à l'entreprise) - 404 sinon (même logique de non-divulgation que TiersServiceImpl).
     *  Doit aussi être actif - 409 sinon (le compte existe et est visible, contrairement au cas 404
     *  ci-dessus, donc pas de raison de masquer son existence). */
    private CompteComptable compteValide(String compteId, String planComptableId, String entiteId) {
        CompteComptable compte = compteComptableRepository.findById(compteId)
                .orElseThrow(() -> new RessourceIntrouvableException("Compte comptable introuvable"));
        boolean visible = compte.getEntite() == null || compte.getEntite().getId().equals(entiteId);
        if (!compte.getPlanComptable().getId().equals(planComptableId) || !visible) {
            throw new RessourceIntrouvableException("Compte comptable introuvable");
        }
        if (!compte.isActif()) {
            throw new ConflitException("Le compte " + compte.getNumero() + " est désactivé et ne peut plus recevoir d'écritures");
        }
        return compte;
    }

    private Sort resolveTri(String sort) {
        return "date,asc".equalsIgnoreCase(sort) ? Sort.by(Sort.Direction.ASC, "date") : Sort.by(Sort.Direction.DESC, "date");
    }

    private EcritureReadDto toReadDto(Ecriture ecriture) {
        List<LigneEcritureReadDto> lignes = ecriture.getLignes().stream()
                .map(l -> new LigneEcritureReadDto(l.getId(), l.getCompte().getId(), l.getCompte().getNumero(),
                        l.getCompte().getLibelle(), l.getSens(), l.getMontant(), l.getLibelle()))
                .toList();
        BigDecimal totalDebit = totalParSens(ecriture, SensCompte.DEBIT);
        BigDecimal totalCredit = totalParSens(ecriture, SensCompte.CREDIT);
        Utilisateur creePar = ecriture.getCreePar();
        Utilisateur validePar = ecriture.getValidePar();
        return new EcritureReadDto(
                ecriture.getId(), ecriture.getDate(), ecriture.getReference(), ecriture.getLibelle(),
                ecriture.getStatut(), ecriture.getNumero(),
                ecriture.getJournal().getId(), ecriture.getJournal().getLibelle(),
                ecriture.getExercice().getId(),
                lignes, totalDebit, totalCredit, totalDebit.compareTo(totalCredit) == 0,
                creePar.getId(), nomComplet(creePar), ecriture.getCreeLe(),
                validePar != null ? validePar.getId() : null,
                validePar != null ? nomComplet(validePar) : null,
                ecriture.getValideLe(), ecriture.getMotifRejet(),
                ecriture.getEcritureMiroirId(), ecriture.getContrePassationDeId());
    }

    private String nomComplet(Utilisateur utilisateur) {
        return utilisateur.getPrenom() + " " + utilisateur.getNom();
    }

    private Utilisateur utilisateurAppelant(String keycloakIdAppelant) {
        return utilisateurRepository.findByKeycloakId(keycloakIdAppelant)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));
    }

    private Entite entiteAppelant(String keycloakIdAppelant) {
        return utilisateurAppelant(keycloakIdAppelant).getEntite();
    }

    /** Charge l'écriture ciblée et vérifie son appartenance à l'entreprise de l'appelant - 404 sinon,
     *  pour ne pas confirmer l'existence d'une écriture d'une autre entreprise. */
    private Ecriture ecritureDeLEntite(String keycloakIdAppelant, String ecritureId) {
        return ecritureDe(entiteAppelant(keycloakIdAppelant), ecritureId);
    }

    private Ecriture ecritureDe(Entite entite, String ecritureId) {
        Ecriture ecriture = ecritureRepository.findById(ecritureId)
                .orElseThrow(() -> new RessourceIntrouvableException("Écriture introuvable"));
        if (!ecriture.getEntite().getId().equals(entite.getId())) {
            throw new RessourceIntrouvableException("Écriture introuvable");
        }
        return ecriture;
    }
}
