package com.walsia.api_compta.integrationClient.service.impl;

import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.dto.readDto.PlanComptableRecapReadDto;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.referentiel.CodeReferentiel;
import com.walsia.api_compta.integrationClient.entity.referentiel.PlanComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.ReferentielComptable;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.integrationClient.repository.ClasseCompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.CompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.PlanComptableRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanComptableConsultationServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private PlanComptableRepository planComptableRepository;
    @Mock
    private ClasseCompteComptableRepository classeCompteComptableRepository;
    @Mock
    private CompteComptableRepository compteComptableRepository;

    private PlanComptableConsultationServiceImpl service;

    private Entite entite;
    private PlanComptable planActif;

    @BeforeEach
    void setUp() {
        service = new PlanComptableConsultationServiceImpl(
                utilisateurRepository, planComptableRepository, classeCompteComptableRepository, compteComptableRepository);

        ReferentielComptable referentiel = ReferentielComptable.builder()
                .id("ref-1").code(CodeReferentiel.SYSCOHADA_NORMAL).libelle("SYSCOHADA - Système Normal").build();
        entite = Entite.builder().id("entite-1").referentielComptable(referentiel).build();
        planActif = PlanComptable.builder().id("plan-1").referentielComptable(referentiel).actif(true).build();

        Utilisateur appelant = Utilisateur.builder().id("user-1").keycloakId("kc-user").entite(entite).build();
        lenient().when(utilisateurRepository.findByKeycloakId("kc-user")).thenReturn(Optional.of(appelant));
    }

    @Test
    void obtenirRecap_calculeLesCompteursScopesAuPlanActifEtALEntiteDeLAppelant() {
        when(planComptableRepository.findByReferentielComptable_IdAndActifTrue("ref-1")).thenReturn(Optional.of(planActif));
        when(classeCompteComptableRepository.countByReferentielComptable_Id("ref-1")).thenReturn(9L);
        when(compteComptableRepository.countVisiblesParPlan("plan-1", "entite-1")).thenReturn(240L);

        PlanComptableRecapReadDto recap = service.obtenirRecap("kc-user");

        assertThat(recap.referentielComptableCode()).isEqualTo(CodeReferentiel.SYSCOHADA_NORMAL);
        assertThat(recap.referentielComptableLibelle()).isEqualTo("SYSCOHADA - Système Normal");
        assertThat(recap.nombreClasses()).isEqualTo(9L);
        assertThat(recap.nombreComptes()).isEqualTo(240L);
    }

    @Test
    void obtenirRecap_planComptableActifIntrouvable_leveRessourceIntrouvable() {
        when(planComptableRepository.findByReferentielComptable_IdAndActifTrue("ref-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenirRecap("kc-user"))
                .isInstanceOf(RessourceIntrouvableException.class);
    }
}
