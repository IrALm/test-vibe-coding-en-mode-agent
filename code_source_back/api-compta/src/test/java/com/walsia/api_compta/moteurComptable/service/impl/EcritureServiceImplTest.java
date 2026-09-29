package com.walsia.api_compta.moteurComptable.service.impl;

import com.walsia.api_compta.exception.ConflitException;
import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.referentiel.CompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.PlanComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.ReferentielComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.integrationClient.repository.CompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.PlanComptableRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.moteurComptable.dto.formDto.ContrePassationForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.EcritureCreationForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.EcritureModificationForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.LigneEcritureForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.RenvoyerBrouillonForm;
import com.walsia.api_compta.moteurComptable.dto.readDto.EcritureReadDto;
import com.walsia.api_compta.moteurComptable.entity.ecriture.Ecriture;
import com.walsia.api_compta.moteurComptable.entity.ecriture.LigneEcriture;
import com.walsia.api_compta.moteurComptable.entity.ecriture.StatutEcriture;
import com.walsia.api_compta.moteurComptable.entity.exercice.ExerciceComptable;
import com.walsia.api_compta.moteurComptable.entity.exercice.StatutExercice;
import com.walsia.api_compta.moteurComptable.entity.journal.Journal;
import com.walsia.api_compta.moteurComptable.repository.EcritureRepository;
import com.walsia.api_compta.moteurComptable.repository.ExerciceComptableRepository;
import com.walsia.api_compta.moteurComptable.repository.JournalRepository;
import com.walsia.api_compta.moteurComptable.specification.EcritureSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.CrudRepository;

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
class EcritureServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private EcritureRepository ecritureRepository;
    @Mock
    private ExerciceComptableRepository exerciceComptableRepository;
    @Mock
    private PlanComptableRepository planComptableRepository;
    @Mock
    private CompteComptableRepository compteComptableRepository;
    @Mock
    private JournalRepository journalRepository;
    @Mock
    private JournalProvisioningService journalProvisioningService;
    @Mock
    private EcritureSpecification ecritureSpecification;

    private EcritureServiceImpl service;

    private Entite entite;
    private PlanComptable planActif;
    private Journal journal;
    private ExerciceComptable exerciceOuvert;
    private Utilisateur appelant;
    private CompteComptable compteDebit;
    private CompteComptable compteCredit;

    @BeforeEach
    void setUp() {
        service = new EcritureServiceImpl(
                utilisateurRepository, ecritureRepository, exerciceComptableRepository, planComptableRepository,
                compteComptableRepository, journalRepository, journalProvisioningService, ecritureSpecification);

        ReferentielComptable referentiel = ReferentielComptable.builder().id("ref-1").build();
        entite = Entite.builder().id("entite-1").referentielComptable(referentiel).build();
        planActif = PlanComptable.builder().id("plan-1").referentielComptable(referentiel).actif(true).build();
        journal = Journal.builder().id("journal-1").entite(entite).dernierNumero(5).build();
        exerciceOuvert = ExerciceComptable.builder().id("ex-1").entite(entite).statut(StatutExercice.OUVERT)
                .dateDebut(LocalDate.of(2026, 1, 1)).dateFin(LocalDate.of(2026, 12, 31)).build();
        appelant = Utilisateur.builder().id("user-1").keycloakId("kc-user").entite(entite).nom("Dupont").prenom("Jean").build();
        compteDebit = CompteComptable.builder().id("compte-1").numero("411").libelle("Clients").planComptable(planActif).build();
        compteCredit = CompteComptable.builder().id("compte-2").numero("701").libelle("Ventes").planComptable(planActif).build();

        lenient().when(utilisateurRepository.findByKeycloakId("kc-user")).thenReturn(Optional.of(appelant));
        lenient().when(exerciceComptableRepository.findByEntite_IdAndStatut("entite-1", StatutExercice.OUVERT))
                .thenReturn(Optional.of(exerciceOuvert));
        lenient().when(planComptableRepository.findByReferentielComptable_IdAndActifTrue("ref-1"))
                .thenReturn(Optional.of(planActif));
        lenient().when(journalProvisioningService.journalGeneralDe(entite)).thenReturn(journal);
        lenient().when(compteComptableRepository.findById("compte-1")).thenReturn(Optional.of(compteDebit));
        lenient().when(compteComptableRepository.findById("compte-2")).thenReturn(Optional.of(compteCredit));
        lenient().when(ecritureRepository.save(any(Ecriture.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private EcritureCreationForm formValide() {
        return new EcritureCreationForm(LocalDate.of(2026, 6, 15), "FAC-001", "Vente au comptant", List.of(
                new LigneEcritureForm("compte-1", SensCompte.DEBIT, new BigDecimal("100.00"), null),
                new LigneEcritureForm("compte-2", SensCompte.CREDIT, new BigDecimal("100.00"), null)));
    }

    private Ecriture ecritureBrouillonEquilibree() {
        Ecriture ecriture = Ecriture.builder().id("ecr-1").entite(entite).exercice(exerciceOuvert).journal(journal)
                .date(LocalDate.of(2026, 6, 15)).libelle("Test").statut(StatutEcriture.BROUILLON).creePar(appelant).build();
        ecriture.getLignes().add(LigneEcriture.builder().ecriture(ecriture).compte(compteDebit)
                .sens(SensCompte.DEBIT).montant(new BigDecimal("100.00")).build());
        ecriture.getLignes().add(LigneEcriture.builder().ecriture(ecriture).compte(compteCredit)
                .sens(SensCompte.CREDIT).montant(new BigDecimal("100.00")).build());
        return ecriture;
    }

    @Test
    void creerEcriture_casNominal_statutBrouillonEtProvisionneLeJournal() {
        EcritureReadDto resultat = service.creerEcriture("kc-user", formValide());

        assertThat(resultat.statut()).isEqualTo(StatutEcriture.BROUILLON);
        assertThat(resultat.journalId()).isEqualTo("journal-1");
        assertThat(resultat.lignes()).hasSize(2);
        assertThat(resultat.equilibree()).isTrue();
    }

    @Test
    void creerEcriture_dateHorsBornesExercice_leveConflit() {
        EcritureCreationForm form = new EcritureCreationForm(LocalDate.of(2027, 1, 1), null, "Hors bornes", List.of(
                new LigneEcritureForm("compte-1", SensCompte.DEBIT, BigDecimal.TEN, null),
                new LigneEcritureForm("compte-2", SensCompte.CREDIT, BigDecimal.TEN, null)));

        assertThatThrownBy(() -> service.creerEcriture("kc-user", form)).isInstanceOf(ConflitException.class);
        verify(ecritureRepository, never()).save(any());
    }

    @Test
    void creerEcriture_aucunExerciceOuvert_leveRessourceIntrouvable() {
        when(exerciceComptableRepository.findByEntite_IdAndStatut("entite-1", StatutExercice.OUVERT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creerEcriture("kc-user", formValide()))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void creerEcriture_compteDuneAutreEntiteNonPartage_leveRessourceIntrouvable() {
        Entite autreEntite = Entite.builder().id("entite-2").build();
        CompteComptable compteAutreEntite = CompteComptable.builder()
                .id("compte-1").planComptable(planActif).entite(autreEntite).build();
        when(compteComptableRepository.findById("compte-1")).thenReturn(Optional.of(compteAutreEntite));

        assertThatThrownBy(() -> service.creerEcriture("kc-user", formValide()))
                .isInstanceOf(RessourceIntrouvableException.class);
        verify(ecritureRepository, never()).save(any());
    }

    @Test
    void creerEcriture_compteDesactive_leveConflit() {
        CompteComptable compteInactif = CompteComptable.builder()
                .id("compte-1").numero("411").libelle("Clients").planComptable(planActif).actif(false).build();
        when(compteComptableRepository.findById("compte-1")).thenReturn(Optional.of(compteInactif));

        assertThatThrownBy(() -> service.creerEcriture("kc-user", formValide()))
                .isInstanceOf(ConflitException.class);
        verify(ecritureRepository, never()).save(any());
    }

    @Test
    void obtenirStats_agregeLesComptesParStatutSurLExercice() {
        when(exerciceComptableRepository.findById("ex-1")).thenReturn(Optional.of(exerciceOuvert));
        when(ecritureRepository.countByExercice_IdAndStatut("ex-1", StatutEcriture.BROUILLON)).thenReturn(2L);
        when(ecritureRepository.countByExercice_IdAndStatut("ex-1", StatutEcriture.EN_ATTENTE)).thenReturn(1L);
        when(ecritureRepository.countByExercice_IdAndStatut("ex-1", StatutEcriture.VALIDEE)).thenReturn(5L);

        var stats = service.obtenirStats("kc-user", "ex-1");

        assertThat(stats.brouillon()).isEqualTo(2L);
        assertThat(stats.enAttente()).isEqualTo(1L);
        assertThat(stats.validees()).isEqualTo(5L);
    }

    @Test
    void modifierEcriture_ecritureValidee_leveConflit() {
        Ecriture ecriture = ecritureBrouillonEquilibree();
        ecriture.setStatut(StatutEcriture.VALIDEE);
        when(ecritureRepository.findById("ecr-1")).thenReturn(Optional.of(ecriture));

        EcritureModificationForm form = new EcritureModificationForm(LocalDate.of(2026, 6, 20), null, "Modif", List.of(
                new LigneEcritureForm("compte-1", SensCompte.DEBIT, BigDecimal.TEN, null),
                new LigneEcritureForm("compte-2", SensCompte.CREDIT, BigDecimal.TEN, null)));

        assertThatThrownBy(() -> service.modifierEcriture("kc-user", "ecr-1", form)).isInstanceOf(ConflitException.class);
    }

    @Test
    void supprimerEcriture_ecritureValidee_leveConflit() {
        Ecriture ecriture = ecritureBrouillonEquilibree();
        ecriture.setStatut(StatutEcriture.VALIDEE);
        when(ecritureRepository.findById("ecr-1")).thenReturn(Optional.of(ecriture));

        assertThatThrownBy(() -> service.supprimerEcriture("kc-user", "ecr-1")).isInstanceOf(ConflitException.class);
        verify((CrudRepository<Ecriture, String>) ecritureRepository, never()).delete(any());
    }

    @Test
    void supprimerEcriture_brouillon_supprimee() {
        Ecriture ecriture = ecritureBrouillonEquilibree();
        when(ecritureRepository.findById("ecr-1")).thenReturn(Optional.of(ecriture));

        service.supprimerEcriture("kc-user", "ecr-1");

        verify((CrudRepository<Ecriture, String>) ecritureRepository).delete(ecriture);
    }

    @Test
    void soumettre_debitDifferentDuCredit_leveConflit() {
        Ecriture ecriture = ecritureBrouillonEquilibree();
        ecriture.getLignes().get(1).setMontant(new BigDecimal("50.00"));
        when(ecritureRepository.findById("ecr-1")).thenReturn(Optional.of(ecriture));

        assertThatThrownBy(() -> service.soumettre("kc-user", "ecr-1")).isInstanceOf(ConflitException.class);
        assertThat(ecriture.getStatut()).isEqualTo(StatutEcriture.BROUILLON);
    }

    @Test
    void soumettre_equilibree_passeEnAttente() {
        Ecriture ecriture = ecritureBrouillonEquilibree();
        when(ecritureRepository.findById("ecr-1")).thenReturn(Optional.of(ecriture));

        EcritureReadDto resultat = service.soumettre("kc-user", "ecr-1");

        assertThat(resultat.statut()).isEqualTo(StatutEcriture.EN_ATTENTE);
    }

    @Test
    void valider_ecritureEncoreEnBrouillon_leveConflit() {
        Ecriture ecriture = ecritureBrouillonEquilibree();
        when(ecritureRepository.findById("ecr-1")).thenReturn(Optional.of(ecriture));

        assertThatThrownBy(() -> service.valider("kc-user", "ecr-1")).isInstanceOf(ConflitException.class);
    }

    @Test
    void valider_appelantDuneAutreEntite_leveRessourceIntrouvable() {
        Entite autreEntite = Entite.builder().id("entite-2").build();
        Ecriture ecriture = ecritureBrouillonEquilibree();
        ecriture.setEntite(autreEntite);
        ecriture.setStatut(StatutEcriture.EN_ATTENTE);
        when(ecritureRepository.findById("ecr-1")).thenReturn(Optional.of(ecriture));

        assertThatThrownBy(() -> service.valider("kc-user", "ecr-1")).isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void valider_assigneUnNumeroSequentielViaLeVerrouDuJournalEtAutoriseLAuto_validation() {
        Ecriture ecriture = ecritureBrouillonEquilibree();
        ecriture.setStatut(StatutEcriture.EN_ATTENTE);
        when(ecritureRepository.findById("ecr-1")).thenReturn(Optional.of(ecriture));
        when(journalRepository.findByIdForUpdate("journal-1")).thenReturn(Optional.of(journal));

        // ecriture.creePar == appelant : l'auto-validation (valider sa propre écriture) est autorisée par construction.
        EcritureReadDto resultat = service.valider("kc-user", "ecr-1");

        assertThat(resultat.statut()).isEqualTo(StatutEcriture.VALIDEE);
        assertThat(resultat.numero()).isEqualTo("000006");
        assertThat(journal.getDernierNumero()).isEqualTo(6);
        verify(journalRepository).findByIdForUpdate("journal-1");
    }

    @Test
    void valider_ecritureMiroir_basculeLoriginaleEnContrepasseeEtLaLie() {
        Ecriture original = ecritureBrouillonEquilibree();
        original.setId("ecr-original");
        original.setStatut(StatutEcriture.VALIDEE);
        original.setNumero("000003");
        when(ecritureRepository.findById("ecr-original")).thenReturn(Optional.of(original));

        Ecriture miroir = ecritureBrouillonEquilibree();
        miroir.setId("ecr-miroir");
        miroir.setStatut(StatutEcriture.EN_ATTENTE);
        miroir.setContrePassationDeId("ecr-original");
        when(ecritureRepository.findById("ecr-miroir")).thenReturn(Optional.of(miroir));
        when(journalRepository.findByIdForUpdate("journal-1")).thenReturn(Optional.of(journal));

        service.valider("kc-user", "ecr-miroir");

        assertThat(original.getStatut()).isEqualTo(StatutEcriture.CONTREPASSEE);
        assertThat(original.getEcritureMiroirId()).isEqualTo("ecr-miroir");
    }

    @Test
    void renvoyerEnBrouillon_depuisEnAttente_repasseEnBrouillonEtStockeLeMotif() {
        Ecriture ecriture = ecritureBrouillonEquilibree();
        ecriture.setStatut(StatutEcriture.EN_ATTENTE);
        when(ecritureRepository.findById("ecr-1")).thenReturn(Optional.of(ecriture));

        service.renvoyerEnBrouillon("kc-user", "ecr-1", new RenvoyerBrouillonForm("Compte erroné"));

        assertThat(ecriture.getStatut()).isEqualTo(StatutEcriture.BROUILLON);
        assertThat(ecriture.getMotifRejet()).isEqualTo("Compte erroné");
    }

    @Test
    void renvoyerEnBrouillon_depuisBrouillon_leveConflit() {
        Ecriture ecriture = ecritureBrouillonEquilibree();
        when(ecritureRepository.findById("ecr-1")).thenReturn(Optional.of(ecriture));

        assertThatThrownBy(() -> service.renvoyerEnBrouillon("kc-user", "ecr-1", new RenvoyerBrouillonForm(null)))
                .isInstanceOf(ConflitException.class);
    }

    @Test
    void contrePasser_sansDateFournie_repredLaDateDeLOriginale() {
        Ecriture original = ecritureBrouillonEquilibree();
        original.setStatut(StatutEcriture.VALIDEE);
        original.setNumero("000003");
        when(ecritureRepository.findById("ecr-1")).thenReturn(Optional.of(original));

        EcritureReadDto miroir = service.contrePasser("kc-user", "ecr-1", new ContrePassationForm(null));

        assertThat(miroir.statut()).isEqualTo(StatutEcriture.BROUILLON);
        assertThat(miroir.contrePassationDeId()).isEqualTo("ecr-1");
        assertThat(miroir.date()).isEqualTo(original.getDate());
        assertThat(miroir.lignes()).hasSize(2);
        assertThat(miroir.lignes().get(0).sens()).isEqualTo(SensCompte.CREDIT); // était DEBIT sur l'originale
        assertThat(miroir.lignes().get(1).sens()).isEqualTo(SensCompte.DEBIT); // était CREDIT sur l'originale
    }

    @Test
    void contrePasser_avecDateFournie_utiliseCetteDate() {
        Ecriture original = ecritureBrouillonEquilibree();
        original.setStatut(StatutEcriture.VALIDEE);
        original.setNumero("000003");
        when(ecritureRepository.findById("ecr-1")).thenReturn(Optional.of(original));

        EcritureReadDto miroir = service.contrePasser("kc-user", "ecr-1", new ContrePassationForm(LocalDate.of(2026, 7, 1)));

        assertThat(miroir.date()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    void contrePasser_dateHorsBornesDeLExerciceOuvert_leveConflit() {
        Ecriture original = ecritureBrouillonEquilibree();
        original.setStatut(StatutEcriture.VALIDEE);
        original.setNumero("000003");
        when(ecritureRepository.findById("ecr-1")).thenReturn(Optional.of(original));

        assertThatThrownBy(() ->
                service.contrePasser("kc-user", "ecr-1", new ContrePassationForm(LocalDate.of(2027, 1, 1))))
                .isInstanceOf(ConflitException.class);
    }

    @Test
    void contrePasser_ecritureNonValidee_leveConflit() {
        Ecriture ecriture = ecritureBrouillonEquilibree();
        when(ecritureRepository.findById("ecr-1")).thenReturn(Optional.of(ecriture));

        assertThatThrownBy(() -> service.contrePasser("kc-user", "ecr-1", new ContrePassationForm(null)))
                .isInstanceOf(ConflitException.class);
    }
}
