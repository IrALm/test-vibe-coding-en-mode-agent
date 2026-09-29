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
import com.walsia.api_compta.integrationClient.entity.referentiel.ReferentielComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.integrationClient.mapper.CompteComptableMapper;
import com.walsia.api_compta.integrationClient.repository.ClasseCompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.CompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.PlanComptableRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompteComptableCreationServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private PlanComptableRepository planComptableRepository;
    @Mock
    private ClasseCompteComptableRepository classeCompteComptableRepository;
    @Mock
    private CompteComptableRepository compteComptableRepository;
    @Mock
    private CompteComptableMapper compteComptableMapper;

    private CompteComptableCreationServiceImpl service;

    private ReferentielComptable referentiel;
    private Entite entite;
    private PlanComptable planActif;
    private ClasseCompteComptable classe;

    @BeforeEach
    void setUp() {
        service = new CompteComptableCreationServiceImpl(
                utilisateurRepository, planComptableRepository, classeCompteComptableRepository,
                compteComptableRepository, compteComptableMapper);

        referentiel = ReferentielComptable.builder().id("ref-1").build();
        entite = Entite.builder().id("entite-1").referentielComptable(referentiel).build();
        planActif = PlanComptable.builder().id("plan-1").referentielComptable(referentiel).actif(true).build();
        classe = ClasseCompteComptable.builder().id("classe-1").numero(4).titre("Tiers").referentielComptable(referentiel).build();

        Utilisateur appelant = Utilisateur.builder().id("user-1").keycloakId("kc-user").entite(entite).build();
        lenient().when(utilisateurRepository.findByKeycloakId("kc-user")).thenReturn(Optional.of(appelant));
        lenient().when(planComptableRepository.findByReferentielComptable_IdAndActifTrue("ref-1"))
                .thenReturn(Optional.of(planActif));
        lenient().when(classeCompteComptableRepository.findById("classe-1")).thenReturn(Optional.of(classe));
    }

    private CompteComptableCreationForm formValide() {
        return new CompteComptableCreationForm("4111", "Clients divers", "classe-1", null, SensCompte.DEBIT, null, null);
    }

    @Test
    void creerCompte_cas_nominal_sauvegardeLeCompteRattacheAuPlanActifEtALEntiteAppelante() {
        CompteComptableCreationForm form = formValide();
        when(compteComptableRepository.existsByPlanComptable_IdAndNumero("plan-1", "4111")).thenReturn(false);
        when(compteComptableRepository.save(any(CompteComptable.class))).thenAnswer(inv -> inv.getArgument(0));
        when(compteComptableMapper.toReadDto(any(CompteComptable.class)))
                .thenReturn(new CompteComptableReadDto("compte-1", "4111", "Clients divers", SensCompte.DEBIT, false, true));

        CompteComptableReadDto resultat = service.creerCompte("kc-user", form);

        assertThat(resultat.numero()).isEqualTo("4111");

        org.mockito.ArgumentCaptor<CompteComptable> captor = org.mockito.ArgumentCaptor.forClass(CompteComptable.class);
        verify(compteComptableRepository).save(captor.capture());
        CompteComptable sauvegarde = captor.getValue();
        assertThat(sauvegarde.getPlanComptable().getId()).isEqualTo("plan-1");
        assertThat(sauvegarde.getEntite().getId()).isEqualTo("entite-1");
        assertThat(sauvegarde.getClasseCompteComptable().getId()).isEqualTo("classe-1");
        assertThat(sauvegarde.getCompteParent()).isNull();
        assertThat(sauvegarde.isLettrable()).isFalse();
        assertThat(sauvegarde.isActif()).isTrue();
    }

    @Test
    void creerCompte_numeroDejaExistantDansLePlan_leveConflit() {
        when(compteComptableRepository.existsByPlanComptable_IdAndNumero("plan-1", "4111")).thenReturn(true);

        assertThatThrownBy(() -> service.creerCompte("kc-user", formValide()))
                .isInstanceOf(ConflitException.class);

        verify(compteComptableRepository, never()).save(any());
    }

    @Test
    void creerCompte_violationConcurrenteEnBase_estConvertieEnConflit() {
        when(compteComptableRepository.existsByPlanComptable_IdAndNumero("plan-1", "4111")).thenReturn(false);
        when(compteComptableRepository.save(any(CompteComptable.class)))
                .thenThrow(new DataIntegrityViolationException("uk_compte_plan_numero"));

        assertThatThrownBy(() -> service.creerCompte("kc-user", formValide()))
                .isInstanceOf(ConflitException.class);
    }

    @Test
    void creerCompte_classeDUnAutreReferentiel_leveRessourceIntrouvable() {
        ReferentielComptable autreReferentiel = ReferentielComptable.builder().id("ref-2").build();
        ClasseCompteComptable classeAutreReferentiel = ClasseCompteComptable.builder()
                .id("classe-1").referentielComptable(autreReferentiel).build();
        when(classeCompteComptableRepository.findById("classe-1")).thenReturn(Optional.of(classeAutreReferentiel));

        assertThatThrownBy(() -> service.creerCompte("kc-user", formValide()))
                .isInstanceOf(RessourceIntrouvableException.class);

        verify(compteComptableRepository, never()).save(any());
    }

    @Test
    void creerCompte_classeIntrouvable_leveRessourceIntrouvable() {
        when(classeCompteComptableRepository.findById("classe-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creerCompte("kc-user", formValide()))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void creerCompte_planComptableActifIntrouvable_leveRessourceIntrouvable() {
        when(planComptableRepository.findByReferentielComptable_IdAndActifTrue("ref-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creerCompte("kc-user", formValide()))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void creerCompte_parentDUneAutreClasse_leveRessourceIntrouvable() {
        ClasseCompteComptable autreClasse = ClasseCompteComptable.builder().id("classe-2").referentielComptable(referentiel).build();
        CompteComptable parentAutreClasse = CompteComptable.builder()
                .id("parent-1").planComptable(planActif).classeCompteComptable(autreClasse).build();
        when(compteComptableRepository.findById("parent-1")).thenReturn(Optional.of(parentAutreClasse));

        CompteComptableCreationForm form = new CompteComptableCreationForm(
                "4111", "Clients divers", "classe-1", "parent-1", SensCompte.DEBIT, null, null);

        assertThatThrownBy(() -> service.creerCompte("kc-user", form))
                .isInstanceOf(RessourceIntrouvableException.class);

        verify(compteComptableRepository, never()).save(any());
    }

    @Test
    void creerCompte_parentDUnAutrePlan_leveRessourceIntrouvable() {
        PlanComptable autrePlan = PlanComptable.builder().id("plan-2").referentielComptable(referentiel).build();
        CompteComptable parentAutrePlan = CompteComptable.builder()
                .id("parent-1").planComptable(autrePlan).classeCompteComptable(classe).build();
        when(compteComptableRepository.findById("parent-1")).thenReturn(Optional.of(parentAutrePlan));

        CompteComptableCreationForm form = new CompteComptableCreationForm(
                "4111", "Clients divers", "classe-1", "parent-1", SensCompte.DEBIT, null, null);

        assertThatThrownBy(() -> service.creerCompte("kc-user", form))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void creerCompte_parentValide_estRattacheAuCompteCree() {
        CompteComptable parent = CompteComptable.builder()
                .id("parent-1").planComptable(planActif).classeCompteComptable(classe).numero("411")
                .sensNormal(SensCompte.DEBIT).build();
        when(compteComptableRepository.findById("parent-1")).thenReturn(Optional.of(parent));
        when(compteComptableRepository.existsByPlanComptable_IdAndNumero("plan-1", "4111")).thenReturn(false);
        when(compteComptableRepository.save(any(CompteComptable.class))).thenAnswer(inv -> inv.getArgument(0));
        when(compteComptableMapper.toReadDto(any(CompteComptable.class)))
                .thenReturn(new CompteComptableReadDto("compte-1", "4111", "Clients divers", SensCompte.DEBIT, false, true));

        CompteComptableCreationForm form = new CompteComptableCreationForm(
                "4111", "Clients divers", "classe-1", "parent-1", SensCompte.DEBIT, null, null);

        service.creerCompte("kc-user", form);

        org.mockito.ArgumentCaptor<CompteComptable> captor = org.mockito.ArgumentCaptor.forClass(CompteComptable.class);
        verify(compteComptableRepository).save(captor.capture());
        assertThat(captor.getValue().getCompteParent()).isEqualTo(parent);
    }

    @Test
    void creerCompte_sousCompte_heriteToujoursDuSensDuParentMemeSiLeFormulaireEnvoieAutreChose() {
        CompteComptable parent = CompteComptable.builder()
                .id("parent-1").planComptable(planActif).classeCompteComptable(classe).numero("411")
                .sensNormal(SensCompte.CREDIT).build();
        when(compteComptableRepository.findById("parent-1")).thenReturn(Optional.of(parent));
        when(compteComptableRepository.existsByPlanComptable_IdAndNumero("plan-1", "4111")).thenReturn(false);
        when(compteComptableRepository.save(any(CompteComptable.class))).thenAnswer(inv -> inv.getArgument(0));
        when(compteComptableMapper.toReadDto(any(CompteComptable.class)))
                .thenReturn(new CompteComptableReadDto("compte-1", "4111", "Clients divers", SensCompte.CREDIT, false, true));

        // Le client tente d'envoyer DEBIT alors que le parent est CREDIT : le serveur doit ignorer cette valeur.
        CompteComptableCreationForm form = new CompteComptableCreationForm(
                "4111", "Clients divers", "classe-1", "parent-1", SensCompte.DEBIT, null, null);

        service.creerCompte("kc-user", form);

        org.mockito.ArgumentCaptor<CompteComptable> captor = org.mockito.ArgumentCaptor.forClass(CompteComptable.class);
        verify(compteComptableRepository).save(captor.capture());
        assertThat(captor.getValue().getSensNormal()).isEqualTo(SensCompte.CREDIT);
    }

    @Test
    void numeroExiste_delegueAuRepositoryAvecLePlanActifDeLAppelant() {
        when(compteComptableRepository.existsByPlanComptable_IdAndNumero("plan-1", "4111")).thenReturn(true);

        boolean resultat = service.numeroExiste("kc-user", "4111");

        assertThat(resultat).isTrue();
        verify(compteComptableRepository).existsByPlanComptable_IdAndNumero("plan-1", "4111");
    }

    @Test
    void listerComptesDeClasse_retourneLesOptionsDuRepositoryScopeesAuPlanEtALEntite() {
        CompteComptable compte = CompteComptable.builder().id("c1").numero("411").libelle("Clients").build();
        when(compteComptableRepository.findVisiblesParClasse("classe-1", "plan-1", "entite-1"))
                .thenReturn(List.of(compte));
        when(compteComptableMapper.toOptionReadDto(compte)).thenReturn(new CompteOptionReadDto("c1", "411", "Clients", SensCompte.DEBIT));

        List<CompteOptionReadDto> resultat = service.listerComptesDeClasse("kc-user", "classe-1");

        assertThat(resultat).containsExactly(new CompteOptionReadDto("c1", "411", "Clients", SensCompte.DEBIT));
    }
}
