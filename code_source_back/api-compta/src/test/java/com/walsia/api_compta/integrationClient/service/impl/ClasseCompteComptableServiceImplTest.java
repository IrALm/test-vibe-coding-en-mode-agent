package com.walsia.api_compta.integrationClient.service.impl;

import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.dto.formDto.CompteComptableSearchForm;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.referentiel.ClasseCompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.CompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.PlanComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.ReferentielComptable;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.integrationClient.mapper.ClasseCompteComptableMapper;
import com.walsia.api_compta.integrationClient.mapper.CompteComptableMapper;
import com.walsia.api_compta.integrationClient.repository.ClasseCompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.CompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.PlanComptableRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.integrationClient.specification.ClasseCompteComptableSpecification;
import com.walsia.api_compta.integrationClient.specification.CompteComptableSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClasseCompteComptableServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private PlanComptableRepository planComptableRepository;
    @Mock
    private ClasseCompteComptableRepository classeCompteComptableRepository;
    @Mock
    private CompteComptableRepository compteComptableRepository;
    @Mock
    private ClasseCompteComptableMapper classeCompteComptableMapper;
    @Mock
    private CompteComptableMapper compteComptableMapper;
    @Mock
    private ClasseCompteComptableSpecification classeCompteComptableSpecification;
    @Mock
    private CompteComptableSpecification compteComptableSpecification;

    private ClasseCompteComptableServiceImpl service;

    private ReferentielComptable referentiel;
    private PlanComptable planActif;

    @BeforeEach
    void setUp() {
        service = new ClasseCompteComptableServiceImpl(
                utilisateurRepository, planComptableRepository, classeCompteComptableRepository,
                compteComptableRepository, classeCompteComptableMapper, compteComptableMapper,
                classeCompteComptableSpecification, compteComptableSpecification);

        referentiel = ReferentielComptable.builder().id("ref-1").build();
        Entite entite = Entite.builder().id("entite-1").referentielComptable(referentiel).build();
        planActif = PlanComptable.builder().id("plan-1").referentielComptable(referentiel).actif(true).build();

        Utilisateur appelant = Utilisateur.builder().id("user-1").keycloakId("kc-user").entite(entite).build();
        lenient().when(utilisateurRepository.findByKeycloakId("kc-user")).thenReturn(Optional.of(appelant));
        lenient().when(planComptableRepository.findByReferentielComptable_IdAndActifTrue("ref-1"))
                .thenReturn(Optional.of(planActif));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listerClasses_construitLaSpecificationAvecLeReferentielPlanEtEntiteDeLAppelant() {
        Specification<ClasseCompteComptable> spec = mock(Specification.class);
        when(classeCompteComptableSpecification.build("ref-1", "plan-1", "entite-1", "tres")).thenReturn(spec);
        when(classeCompteComptableRepository.findAll(eq(spec), any(Sort.class))).thenReturn(List.of());

        service.listerClasses("kc-user", "tres");

        // Preuve d'isolation tenant : la specification est toujours construite avec le référentiel/plan/entité
        // de l'appelant résolus côté serveur, jamais un identifiant fourni par le client.
        verify(classeCompteComptableSpecification).build("ref-1", "plan-1", "entite-1", "tres");
    }

    @Test
    void listerClasses_planComptableActifIntrouvable_leveRessourceIntrouvable() {
        when(planComptableRepository.findByReferentielComptable_IdAndActifTrue("ref-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listerClasses("kc-user", null))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void rechercherComptes_classeDUnAutreReferentiel_leveRessourceIntrouvable() {
        ReferentielComptable autreReferentiel = ReferentielComptable.builder().id("ref-2").build();
        ClasseCompteComptable classeAutreReferentiel = ClasseCompteComptable.builder()
                .id("classe-1").referentielComptable(autreReferentiel).build();
        when(classeCompteComptableRepository.findById("classe-1")).thenReturn(Optional.of(classeAutreReferentiel));

        CompteComptableSearchForm form = new CompteComptableSearchForm(null, null, null, null);

        assertThatThrownBy(() -> service.rechercherComptes("kc-user", "classe-1", form))
                .isInstanceOf(RessourceIntrouvableException.class);

        verifyNoInteractions(compteComptableSpecification);
    }

    @Test
    @SuppressWarnings("unchecked")
    void rechercherComptes_construitLaSpecificationAvecClassePlanActifEtEntiteDeLAppelant() {
        ClasseCompteComptable classe = ClasseCompteComptable.builder()
                .id("classe-1").numero(4).titre("Tiers").referentielComptable(referentiel).build();
        when(classeCompteComptableRepository.findById("classe-1")).thenReturn(Optional.of(classe));

        CompteComptableSearchForm form = new CompteComptableSearchForm(null, null, null, null);
        Specification<CompteComptable> spec = mock(Specification.class);
        when(compteComptableSpecification.build("classe-1", "plan-1", "entite-1", form)).thenReturn(spec);
        when(compteComptableRepository.findAll(eq(spec), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.rechercherComptes("kc-user", "classe-1", form);

        // Preuve d'isolation tenant : la specification est toujours construite avec le plan comptable
        // actif et l'entité résolus côté serveur, jamais fournis par le client.
        verify(compteComptableSpecification).build("classe-1", "plan-1", "entite-1", form);
    }

    @Test
    @SuppressWarnings("unchecked")
    void rechercherComptesGlobal_construitLaSpecificationSansFrontiereDeClasse() {
        CompteComptableSearchForm form = new CompteComptableSearchForm("411", null, null, null);
        Specification<CompteComptable> spec = mock(Specification.class);
        when(compteComptableSpecification.build(null, "plan-1", "entite-1", form)).thenReturn(spec);
        when(compteComptableRepository.findAll(eq(spec), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.rechercherComptesGlobal("kc-user", form);

        // Pas de classeId : la recherche porte sur toutes les classes du plan actif de l'appelant.
        verify(compteComptableSpecification).build(null, "plan-1", "entite-1", form);
    }
}
