package com.walsia.api_compta.integrationClient.service.impl;

import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.dto.formDto.TiersAssociationCompteForm;
import com.walsia.api_compta.integrationClient.dto.formDto.TiersCreationForm;
import com.walsia.api_compta.integrationClient.dto.formDto.TiersModificationForm;
import com.walsia.api_compta.integrationClient.dto.formDto.TiersSearchForm;
import com.walsia.api_compta.integrationClient.dto.readDto.TiersReadDto;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.referentiel.CompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.PlanComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.ReferentielComptable;
import com.walsia.api_compta.integrationClient.entity.tiers.Tiers;
import com.walsia.api_compta.integrationClient.entity.tiers.TypeTiers;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.integrationClient.mapper.TiersMapper;
import com.walsia.api_compta.integrationClient.repository.CompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.PlanComptableRepository;
import com.walsia.api_compta.integrationClient.repository.TiersRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.integrationClient.specification.TiersSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TiersServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private TiersRepository tiersRepository;
    @Mock
    private PlanComptableRepository planComptableRepository;
    @Mock
    private CompteComptableRepository compteComptableRepository;
    @Mock
    private TiersMapper tiersMapper;
    @Mock
    private TiersSpecification tiersSpecification;

    private TiersServiceImpl service;

    private ReferentielComptable referentiel;
    private Entite entite;
    private PlanComptable planActif;

    @BeforeEach
    void setUp() {
        service = new TiersServiceImpl(
                utilisateurRepository, tiersRepository, planComptableRepository,
                compteComptableRepository, tiersMapper, tiersSpecification);

        referentiel = ReferentielComptable.builder().id("ref-1").build();
        entite = Entite.builder().id("entite-1").referentielComptable(referentiel).build();
        planActif = PlanComptable.builder().id("plan-1").referentielComptable(referentiel).actif(true).build();

        Utilisateur appelant = Utilisateur.builder().id("user-1").keycloakId("kc-user").entite(entite).build();
        lenient().when(utilisateurRepository.findByKeycloakId("kc-user")).thenReturn(Optional.of(appelant));
        lenient().when(planComptableRepository.findByReferentielComptable_IdAndActifTrue("ref-1"))
                .thenReturn(Optional.of(planActif));
        lenient().when(tiersRepository.save(any(Tiers.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(tiersMapper.toReadDto(any(Tiers.class))).thenAnswer(inv -> {
            Tiers t = inv.getArgument(0);
            return new TiersReadDto(t.getId(), t.getType(), t.getRaisonSociale(), t.getNomContact(), t.getEmail(),
                    t.getTelephone(), t.getAdresse(), t.getNumeroFiscal(), t.getIntitulePoste(), t.isActif(),
                    t.getCompteAssocie() != null ? t.getCompteAssocie().getId() : null,
                    t.getCompteAssocie() != null ? t.getCompteAssocie().getNumero() : null,
                    t.getCompteAssocie() != null ? t.getCompteAssocie().getLibelle() : null);
        });
    }

    private TiersCreationForm formValide() {
        return new TiersCreationForm(TypeTiers.CLIENT, "Client SARL", "Jean Dupont", "jean@client.cd",
                "+243900000000", "Kinshasa", "NIF-123", null, null, null);
    }

    @Test
    void creerTiers_cas_nominal_rattacheLeTiersALEntiteAppelante() {
        TiersReadDto resultat = service.creerTiers("kc-user", formValide());

        assertThat(resultat.raisonSociale()).isEqualTo("Client SARL");

        ArgumentCaptor<Tiers> captor = ArgumentCaptor.forClass(Tiers.class);
        verify(tiersRepository).save(captor.capture());
        Tiers sauvegarde = captor.getValue();
        assertThat(sauvegarde.getEntite().getId()).isEqualTo("entite-1");
        assertThat(sauvegarde.getType()).isEqualTo(TypeTiers.CLIENT);
        assertThat(sauvegarde.isActif()).isTrue();
        assertThat(sauvegarde.getCompteAssocie()).isNull();
    }

    @Test
    void creerTiers_avecCompteAssocieValide_estRattacheAuTiers() {
        CompteComptable compte = CompteComptable.builder()
                .id("compte-1").numero("411").libelle("Clients").planComptable(planActif).build();
        when(compteComptableRepository.findById("compte-1")).thenReturn(Optional.of(compte));

        TiersCreationForm form = new TiersCreationForm(TypeTiers.CLIENT, "Client SARL", null, null,
                null, null, null, null, null, "compte-1");

        service.creerTiers("kc-user", form);

        ArgumentCaptor<Tiers> captor = ArgumentCaptor.forClass(Tiers.class);
        verify(tiersRepository).save(captor.capture());
        assertThat(captor.getValue().getCompteAssocie()).isEqualTo(compte);
    }

    @Test
    void creerTiers_compteAssocieDUnAutrePlan_leveRessourceIntrouvable() {
        PlanComptable autrePlan = PlanComptable.builder().id("plan-2").referentielComptable(referentiel).build();
        CompteComptable compteAutrePlan = CompteComptable.builder().id("compte-1").planComptable(autrePlan).build();
        when(compteComptableRepository.findById("compte-1")).thenReturn(Optional.of(compteAutrePlan));

        TiersCreationForm form = new TiersCreationForm(TypeTiers.CLIENT, "Client SARL", null, null,
                null, null, null, null, null, "compte-1");

        assertThatThrownBy(() -> service.creerTiers("kc-user", form))
                .isInstanceOf(RessourceIntrouvableException.class);
        verify(tiersRepository, never()).save(any());
    }

    @Test
    void creerTiers_compteAssocieDUneAutreEntite_leveRessourceIntrouvable() {
        Entite autreEntite = Entite.builder().id("entite-2").build();
        CompteComptable compteAutreEntite = CompteComptable.builder()
                .id("compte-1").planComptable(planActif).entite(autreEntite).build();
        when(compteComptableRepository.findById("compte-1")).thenReturn(Optional.of(compteAutreEntite));

        TiersCreationForm form = new TiersCreationForm(TypeTiers.CLIENT, "Client SARL", null, null,
                null, null, null, null, null, "compte-1");

        assertThatThrownBy(() -> service.creerTiers("kc-user", form))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void obtenirDetail_tiersDuneAutreEntite_leveRessourceIntrouvable() {
        Entite autreEntite = Entite.builder().id("entite-2").build();
        Tiers tiersAutreEntite = Tiers.builder().id("tiers-1").entite(autreEntite).build();
        when(tiersRepository.findById("tiers-1")).thenReturn(Optional.of(tiersAutreEntite));

        assertThatThrownBy(() -> service.obtenirDetail("kc-user", "tiers-1"))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void obtenirDetail_tiersIntrouvable_leveRessourceIntrouvable() {
        when(tiersRepository.findById("tiers-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenirDetail("kc-user", "tiers-1"))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void modifierTiers_appliqueSeulementLesChampsNonNuls() {
        Tiers tiers = Tiers.builder()
                .id("tiers-1").entite(entite).type(TypeTiers.CLIENT).raisonSociale("Ancien nom")
                .nomContact("Ancien contact").actif(true).build();
        when(tiersRepository.findById("tiers-1")).thenReturn(Optional.of(tiers));

        TiersModificationForm form = new TiersModificationForm(null, "Nouveau nom", null, null, null, null, null, null, null);
        service.modifierTiers("kc-user", "tiers-1", form);

        assertThat(tiers.getRaisonSociale()).isEqualTo("Nouveau nom");
        assertThat(tiers.getNomContact()).isEqualTo("Ancien contact");
        assertThat(tiers.getType()).isEqualTo(TypeTiers.CLIENT);
    }

    @Test
    void associerCompte_compteAssocieIdNull_dissocieLeCompteExistant() {
        CompteComptable ancienCompte = CompteComptable.builder().id("compte-1").planComptable(planActif).build();
        Tiers tiers = Tiers.builder().id("tiers-1").entite(entite).compteAssocie(ancienCompte).build();
        when(tiersRepository.findById("tiers-1")).thenReturn(Optional.of(tiers));

        service.associerCompte("kc-user", "tiers-1", new TiersAssociationCompteForm(null));

        assertThat(tiers.getCompteAssocie()).isNull();
    }

    @Test
    void associerCompte_compteAssocieIdValide_associeLeNouveauCompte() {
        Tiers tiers = Tiers.builder().id("tiers-1").entite(entite).build();
        when(tiersRepository.findById("tiers-1")).thenReturn(Optional.of(tiers));
        CompteComptable compte = CompteComptable.builder().id("compte-1").planComptable(planActif).build();
        when(compteComptableRepository.findById("compte-1")).thenReturn(Optional.of(compte));

        service.associerCompte("kc-user", "tiers-1", new TiersAssociationCompteForm("compte-1"));

        assertThat(tiers.getCompteAssocie()).isEqualTo(compte);
    }

    @Test
    void desactiverTiers_mestLeTiersInactif() {
        Tiers tiers = Tiers.builder().id("tiers-1").entite(entite).actif(true).build();
        when(tiersRepository.findById("tiers-1")).thenReturn(Optional.of(tiers));

        service.desactiverTiers("kc-user", "tiers-1");

        assertThat(tiers.isActif()).isFalse();
    }

    @Test
    void obtenirRecap_agregeLeTotalParTypeEtSansCompteAssocie() {
        when(tiersRepository.countByEntite_Id("entite-1")).thenReturn(10L);
        for (TypeTiers type : TypeTiers.values()) {
            when(tiersRepository.countByEntite_IdAndType("entite-1", type)).thenReturn(2L);
        }
        when(tiersRepository.countByEntite_IdAndCompteAssocieIsNull("entite-1")).thenReturn(3L);

        var recap = service.obtenirRecap("kc-user");

        assertThat(recap.total()).isEqualTo(10L);
        assertThat(recap.sansCompteAssocie()).isEqualTo(3L);
        assertThat(recap.parType()).hasSize(TypeTiers.values().length);
        assertThat(recap.parType().get(TypeTiers.SALARIE)).isEqualTo(2L);
    }

    @Test
    void rechercherTiers_delegueAuRepositoryAvecIsolationTenant() {
        Specification<Tiers> specification = mock(Specification.class);
        TiersSearchForm form = new TiersSearchForm(null, null, null, null, null, null);
        when(tiersSpecification.build("entite-1", form)).thenReturn(specification);
        when(tiersRepository.findAll(eq(specification), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<Tiers>(java.util.List.of()));

        service.rechercherTiers("kc-user", form);

        verify(tiersSpecification).build("entite-1", form);
    }
}
