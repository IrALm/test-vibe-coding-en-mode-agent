package com.walsia.api_compta.moteurComptable.service.impl;

import com.walsia.api_compta.exception.ConflitException;
import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.referentiel.CompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.PlanComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.ReferentielComptable;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.integrationClient.repository.CompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.PlanComptableRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.moteurComptable.dto.formDto.ExerciceCreationForm;
import com.walsia.api_compta.moteurComptable.dto.readDto.ClotureCheckReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.ExerciceReadDto;
import com.walsia.api_compta.moteurComptable.entity.ecriture.Ecriture;
import com.walsia.api_compta.moteurComptable.entity.ecriture.StatutEcriture;
import com.walsia.api_compta.moteurComptable.entity.exercice.ExerciceComptable;
import com.walsia.api_compta.moteurComptable.entity.exercice.StatutExercice;
import com.walsia.api_compta.moteurComptable.entity.journal.Journal;
import com.walsia.api_compta.moteurComptable.mapper.ExerciceMapper;
import com.walsia.api_compta.moteurComptable.repository.EcritureRepository;
import com.walsia.api_compta.moteurComptable.repository.ExerciceComptableRepository;
import com.walsia.api_compta.moteurComptable.repository.JournalRepository;
import com.walsia.api_compta.moteurComptable.repository.LigneEcritureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciceComptableServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ExerciceComptableRepository exerciceComptableRepository;
    @Mock
    private EcritureRepository ecritureRepository;
    @Mock
    private LigneEcritureRepository ligneEcritureRepository;
    @Mock
    private PlanComptableRepository planComptableRepository;
    @Mock
    private CompteComptableRepository compteComptableRepository;
    @Mock
    private JournalRepository journalRepository;
    @Mock
    private JournalProvisioningService journalProvisioningService;
    @Mock
    private ExerciceMapper exerciceMapper;

    private ExerciceComptableServiceImpl service;
    private Entite entite;
    private Utilisateur appelant;

    @BeforeEach
    void setUp() {
        service = new ExerciceComptableServiceImpl(
                utilisateurRepository, exerciceComptableRepository, ecritureRepository, ligneEcritureRepository,
                planComptableRepository, compteComptableRepository, journalRepository, journalProvisioningService,
                exerciceMapper);

        ReferentielComptable referentiel = ReferentielComptable.builder().id("ref-1").build();
        entite = Entite.builder().id("entite-1").referentielComptable(referentiel).build();
        appelant = Utilisateur.builder().id("user-1").keycloakId("kc-user").entite(entite).build();
        lenient().when(utilisateurRepository.findByKeycloakId("kc-user")).thenReturn(Optional.of(appelant));
        lenient().when(exerciceComptableRepository.save(any(ExerciceComptable.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(exerciceMapper.toReadDto(any(ExerciceComptable.class))).thenAnswer(inv -> {
            ExerciceComptable e = inv.getArgument(0);
            return new ExerciceReadDto(e.getId(), e.getDateDebut(), e.getDateFin(), e.getStatut());
        });
    }

    @Test
    void creerExercice_premierExerciceDeLEntite_accepteJusqua24Mois() {
        lenient().when(exerciceComptableRepository.findByEntite_IdOrderByDateDebutDesc("entite-1"))
                .thenReturn(List.of());
        ExerciceCreationForm form = new ExerciceCreationForm(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 6, 30));

        ExerciceReadDto resultat = service.creerExercice("kc-user", form);

        assertThat(resultat.statut()).isEqualTo(StatutExercice.OUVERT);
    }

    @Test
    void creerExercice_exerciceSuivant_dureeExcessive_leveConflit() {
        ExerciceComptable premier = ExerciceComptable.builder()
                .id("ex-0").entite(entite).statut(StatutExercice.CLOS)
                .dateDebut(LocalDate.of(2024, 1, 1)).dateFin(LocalDate.of(2024, 12, 31)).build();
        when(exerciceComptableRepository.findByEntite_IdOrderByDateDebutDesc("entite-1")).thenReturn(List.of(premier));

        ExerciceCreationForm form = new ExerciceCreationForm(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 6, 30));

        assertThatThrownBy(() -> service.creerExercice("kc-user", form)).isInstanceOf(ConflitException.class);
        verify(exerciceComptableRepository, never()).save(any());
    }

    @Test
    void creerExercice_datesChevauchantUnExerciceExistant_leveConflit() {
        ExerciceComptable existant = ExerciceComptable.builder()
                .id("ex-0").entite(entite).statut(StatutExercice.CLOS)
                .dateDebut(LocalDate.of(2025, 1, 1)).dateFin(LocalDate.of(2025, 12, 31)).build();
        when(exerciceComptableRepository.findByEntite_IdOrderByDateDebutDesc("entite-1")).thenReturn(List.of(existant));

        ExerciceCreationForm form = new ExerciceCreationForm(LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31));

        assertThatThrownBy(() -> service.creerExercice("kc-user", form)).isInstanceOf(ConflitException.class);
        verify(exerciceComptableRepository, never()).save(any());
    }

    @Test
    void creerExercice_dejaUnExerciceOuvert_leveConflit() {
        when(exerciceComptableRepository.findByEntite_IdOrderByDateDebutDesc("entite-1")).thenReturn(List.of());
        when(exerciceComptableRepository.existsByEntite_IdAndStatut("entite-1", StatutExercice.OUVERT)).thenReturn(true);

        ExerciceCreationForm form = new ExerciceCreationForm(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThatThrownBy(() -> service.creerExercice("kc-user", form)).isInstanceOf(ConflitException.class);
        verify(exerciceComptableRepository, never()).save(any());
    }

    @Test
    void creerExercice_dateFinAvantDateDebut_leveConflit() {
        ExerciceCreationForm form = new ExerciceCreationForm(LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> service.creerExercice("kc-user", form)).isInstanceOf(ConflitException.class);
        verify(exerciceComptableRepository, never()).save(any());
    }

    @Test
    void cloturerExercice_ecrituresBrouillonRestantes_leveConflit() {
        ExerciceComptable exercice = ExerciceComptable.builder()
                .id("ex-1").entite(entite).statut(StatutExercice.OUVERT)
                .dateDebut(LocalDate.of(2026, 1, 1)).dateFin(LocalDate.of(2026, 12, 31)).build();
        when(exerciceComptableRepository.findById("ex-1")).thenReturn(Optional.of(exercice));
        when(ecritureRepository.countByExercice_IdAndStatut("ex-1", StatutEcriture.BROUILLON)).thenReturn(2L);
        when(ecritureRepository.countByExercice_IdAndStatut("ex-1", StatutEcriture.EN_ATTENTE)).thenReturn(0L);

        assertThatThrownBy(() -> service.cloturerExercice("kc-user", "ex-1")).isInstanceOf(ConflitException.class);
        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.OUVERT);
    }

    @Test
    void cloturerExercice_aucuneEcritureNonValidee_cloture() {
        ExerciceComptable exercice = ExerciceComptable.builder()
                .id("ex-1").entite(entite).statut(StatutExercice.OUVERT)
                .dateDebut(LocalDate.of(2026, 1, 1)).dateFin(LocalDate.of(2026, 12, 31)).build();
        when(exerciceComptableRepository.findById("ex-1")).thenReturn(Optional.of(exercice));
        when(ecritureRepository.countByExercice_IdAndStatut("ex-1", StatutEcriture.BROUILLON)).thenReturn(0L);
        when(ecritureRepository.countByExercice_IdAndStatut("ex-1", StatutEcriture.EN_ATTENTE)).thenReturn(0L);

        service.cloturerExercice("kc-user", "ex-1");

        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.CLOS);
    }

    @Test
    void cloturerExercice_resultatNonNul_genereUneEcritureDeClotureValideeVersLeCompteDeResultat() {
        ExerciceComptable exercice = ExerciceComptable.builder()
                .id("ex-1").entite(entite).statut(StatutExercice.OUVERT)
                .dateDebut(LocalDate.of(2026, 1, 1)).dateFin(LocalDate.of(2026, 12, 31)).build();
        when(exerciceComptableRepository.findById("ex-1")).thenReturn(Optional.of(exercice));
        when(ecritureRepository.countByExercice_IdAndStatut("ex-1", StatutEcriture.BROUILLON)).thenReturn(0L);
        when(ecritureRepository.countByExercice_IdAndStatut("ex-1", StatutEcriture.EN_ATTENTE)).thenReturn(0L);

        // Compte 605 (charge) en position nette débitrice de 123 => résultat = -123 (perte).
        when(ligneEcritureRepository.calculerSoldesGestion("ex-1"))
                .thenReturn(List.<Object[]>of(new Object[]{"compte-605", new BigDecimal("-123.00")}));
        when(ligneEcritureRepository.calculerResultat("ex-1")).thenReturn(new BigDecimal("-123.00"));

        PlanComptable planActif = PlanComptable.builder().id("plan-1").build();
        when(planComptableRepository.findByReferentielComptable_IdAndActifTrue("ref-1"))
                .thenReturn(Optional.of(planActif));
        CompteComptable compte605 = CompteComptable.builder().id("compte-605").numero("605").libelle("Autres achats").build();
        when(compteComptableRepository.findById("compte-605")).thenReturn(Optional.of(compte605));
        CompteComptable comptePerte = CompteComptable.builder().id("compte-119").numero("119").libelle("Résultat net — Perte").build();
        when(compteComptableRepository.findByPlanComptable_IdAndNumero("plan-1", "119"))
                .thenReturn(Optional.of(comptePerte));

        Journal journal = Journal.builder().id("journal-1").entite(entite).dernierNumero(5).build();
        when(journalProvisioningService.journalGeneralDe(entite)).thenReturn(journal);
        when(journalRepository.findByIdForUpdate("journal-1")).thenReturn(Optional.of(journal));

        service.cloturerExercice("kc-user", "ex-1");

        ArgumentCaptor<Ecriture> captor = ArgumentCaptor.forClass(Ecriture.class);
        verify(ecritureRepository).save(captor.capture());
        Ecriture ecritureCloture = captor.getValue();

        assertThat(ecritureCloture.isGenereParCloture()).isTrue();
        assertThat(ecritureCloture.getStatut()).isEqualTo(StatutEcriture.VALIDEE);
        assertThat(ecritureCloture.getNumero()).isEqualTo("000006");
        assertThat(ecritureCloture.getLignes()).hasSize(2);
        assertThat(ecritureCloture.getLignes())
                .anySatisfy(l -> {
                    assertThat(l.getCompte().getNumero()).isEqualTo("605");
                    assertThat(l.getSens().name()).isEqualTo("CREDIT");
                    assertThat(l.getMontant()).isEqualByComparingTo("123.00");
                })
                .anySatisfy(l -> {
                    assertThat(l.getCompte().getNumero()).isEqualTo("119");
                    assertThat(l.getSens().name()).isEqualTo("DEBIT");
                    assertThat(l.getMontant()).isEqualByComparingTo("123.00");
                });
        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.CLOS);
    }

    @Test
    void cloturerExercice_resultatNul_neGenereAucuneEcritureDeCloture() {
        ExerciceComptable exercice = ExerciceComptable.builder()
                .id("ex-1").entite(entite).statut(StatutExercice.OUVERT)
                .dateDebut(LocalDate.of(2026, 1, 1)).dateFin(LocalDate.of(2026, 12, 31)).build();
        when(exerciceComptableRepository.findById("ex-1")).thenReturn(Optional.of(exercice));
        when(ecritureRepository.countByExercice_IdAndStatut("ex-1", StatutEcriture.BROUILLON)).thenReturn(0L);
        when(ecritureRepository.countByExercice_IdAndStatut("ex-1", StatutEcriture.EN_ATTENTE)).thenReturn(0L);
        when(ligneEcritureRepository.calculerSoldesGestion("ex-1")).thenReturn(List.of());

        service.cloturerExercice("kc-user", "ex-1");

        verify(ecritureRepository, never()).save(any(Ecriture.class));
        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.CLOS);
    }

    @Test
    void cloturerExercice_compteDeResultatIntrouvableDansLePlan_leveConflit() {
        ExerciceComptable exercice = ExerciceComptable.builder()
                .id("ex-1").entite(entite).statut(StatutExercice.OUVERT)
                .dateDebut(LocalDate.of(2026, 1, 1)).dateFin(LocalDate.of(2026, 12, 31)).build();
        when(exerciceComptableRepository.findById("ex-1")).thenReturn(Optional.of(exercice));
        when(ecritureRepository.countByExercice_IdAndStatut("ex-1", StatutEcriture.BROUILLON)).thenReturn(0L);
        when(ecritureRepository.countByExercice_IdAndStatut("ex-1", StatutEcriture.EN_ATTENTE)).thenReturn(0L);
        when(ligneEcritureRepository.calculerSoldesGestion("ex-1"))
                .thenReturn(List.<Object[]>of(new Object[]{"compte-701", new BigDecimal("123.00")}));
        when(ligneEcritureRepository.calculerResultat("ex-1")).thenReturn(new BigDecimal("123.00"));
        PlanComptable planActif = PlanComptable.builder().id("plan-1").build();
        when(planComptableRepository.findByReferentielComptable_IdAndActifTrue("ref-1"))
                .thenReturn(Optional.of(planActif));
        when(compteComptableRepository.findByPlanComptable_IdAndNumero("plan-1", "111")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cloturerExercice("kc-user", "ex-1")).isInstanceOf(ConflitException.class);
        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.OUVERT);
        verify(exerciceComptableRepository, never()).save(any());
    }

    @Test
    void verifierCloture_retourneLesComptesExacts() {
        ExerciceComptable exercice = ExerciceComptable.builder()
                .id("ex-1").entite(entite).statut(StatutExercice.OUVERT)
                .dateDebut(LocalDate.of(2026, 1, 1)).dateFin(LocalDate.of(2026, 12, 31)).build();
        when(exerciceComptableRepository.findById("ex-1")).thenReturn(Optional.of(exercice));
        when(ecritureRepository.countByExercice_IdAndStatut("ex-1", StatutEcriture.BROUILLON)).thenReturn(3L);
        when(ecritureRepository.countByExercice_IdAndStatut("ex-1", StatutEcriture.EN_ATTENTE)).thenReturn(1L);

        ClotureCheckReadDto check = service.verifierCloture("kc-user", "ex-1");

        assertThat(check.nombreBrouillon()).isEqualTo(3L);
        assertThat(check.nombreEnAttente()).isEqualTo(1L);
        assertThat(check.cloturable()).isFalse();
    }

    @Test
    void obtenirResultat_delegueAuCalculSurLesEcrituresValideesDeLExercice() {
        ExerciceComptable exercice = ExerciceComptable.builder()
                .id("ex-1").entite(entite).statut(StatutExercice.OUVERT)
                .dateDebut(LocalDate.of(2026, 1, 1)).dateFin(LocalDate.of(2026, 12, 31)).build();
        when(exerciceComptableRepository.findById("ex-1")).thenReturn(Optional.of(exercice));
        when(ligneEcritureRepository.calculerResultat("ex-1")).thenReturn(new java.math.BigDecimal("1500.00"));

        var resultat = service.obtenirResultat("kc-user", "ex-1");

        assertThat(resultat.resultat()).isEqualByComparingTo("1500.00");
    }

    @Test
    void obtenirExerciceOuvert_aucunExerciceOuvert_leveRessourceIntrouvable() {
        when(exerciceComptableRepository.findByEntite_IdAndStatut("entite-1", StatutExercice.OUVERT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenirExerciceOuvert("kc-user"))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void obtenirExerciceOuvert_exerciceDuneAutreEntite_neVoitQueSonPropreExercice() {
        // findByEntite_IdAndStatut est scopé par construction sur l'entiteId de l'appelant : ce test
        // vérifie que le repository est bien interrogé avec l'entite de l'appelant, pas un id arbitraire.
        ExerciceComptable exercice = ExerciceComptable.builder()
                .id("ex-1").entite(entite).statut(StatutExercice.OUVERT)
                .dateDebut(LocalDate.of(2026, 1, 1)).dateFin(LocalDate.of(2026, 12, 31)).build();
        when(exerciceComptableRepository.findByEntite_IdAndStatut("entite-1", StatutExercice.OUVERT))
                .thenReturn(Optional.of(exercice));

        ExerciceReadDto resultat = service.obtenirExerciceOuvert("kc-user");

        assertThat(resultat.id()).isEqualTo("ex-1");
        verify(exerciceComptableRepository).findByEntite_IdAndStatut("entite-1", StatutExercice.OUVERT);
    }
}
