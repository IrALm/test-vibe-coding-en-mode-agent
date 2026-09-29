package com.walsia.api_compta.integrationClient.service.impl;

import com.walsia.api_compta.authentification.service.interfaces.KeycloakAdminService;
import com.walsia.api_compta.authentification.service.interfaces.UserTokenService;
import com.walsia.api_compta.exception.ConflitException;
import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.dto.formDto.UtilisateurCreationForm;
import com.walsia.api_compta.integrationClient.dto.formDto.UtilisateurSearchForm;
import com.walsia.api_compta.integrationClient.dto.readDto.UtilisateurReadDto;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Role;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.integrationClient.mapper.UtilisateurMapper;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.integrationClient.specification.UtilisateurSpecification;
import com.walsia.api_compta.mail.service.interfaces.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtilisateurGestionServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private KeycloakAdminService keycloakAdminService;
    @Mock
    private MailService mailService;
    @Mock
    private UserTokenService userTokenService;
    @Mock
    private UtilisateurMapper utilisateurMapper;
    @Mock
    private UtilisateurSpecification utilisateurSpecification;

    private UtilisateurGestionServiceImpl service;

    private Entite entite;

    @BeforeEach
    void setUp() {
        service = new UtilisateurGestionServiceImpl(
                utilisateurRepository, keycloakAdminService, mailService, userTokenService,
                utilisateurMapper, utilisateurSpecification);

        entite = Entite.builder().id("entite-1").build();
        Utilisateur appelant = Utilisateur.builder()
                .id("admin-1").keycloakId("kc-admin").role(Role.ADMIN).actif(true).entite(entite).build();

        lenient().when(utilisateurRepository.findByKeycloakId("kc-admin")).thenReturn(Optional.of(appelant));
    }

    @Test
    @SuppressWarnings("unchecked")
    void rechercherUtilisateurs_construitLaSpecificationAvecLEntiteDeLAppelant() {
        UtilisateurSearchForm form = new UtilisateurSearchForm(null, null, null, null, null, null, null, null);
        Specification<Utilisateur> spec = mock(Specification.class);
        when(utilisateurSpecification.build(form, "entite-1")).thenReturn(spec);
        when(utilisateurRepository.findAll(eq(spec), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.rechercherUtilisateurs("kc-admin", form);

        // Preuve d'isolation tenant : la specification est toujours construite avec l'entité
        // de l'appelant résolue côté serveur, jamais un entiteId fourni par le client
        // (UtilisateurSearchForm n'a d'ailleurs pas de champ entiteId).
        verify(utilisateurSpecification).build(form, "entite-1");
    }

    @Test
    void desactiver_dernierAdminActif_estRefuse() {
        Utilisateur cible = Utilisateur.builder().id("user-1").role(Role.ADMIN).actif(true).entite(entite).build();
        when(utilisateurRepository.findById("user-1")).thenReturn(Optional.of(cible));
        when(utilisateurRepository.countByEntite_IdAndRoleAndActif("entite-1", Role.ADMIN, true)).thenReturn(1L);

        assertThatThrownBy(() -> service.desactiver("kc-admin", "user-1"))
                .isInstanceOf(ConflitException.class);

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void desactiver_adminAvecAutreAdminActif_fonctionne() {
        Utilisateur cible = Utilisateur.builder().id("user-1").role(Role.ADMIN).actif(true).entite(entite).build();
        when(utilisateurRepository.findById("user-1")).thenReturn(Optional.of(cible));
        when(utilisateurRepository.countByEntite_IdAndRoleAndActif("entite-1", Role.ADMIN, true)).thenReturn(2L);
        when(utilisateurMapper.toReadDto(cible)).thenReturn(
                new UtilisateurReadDto("user-1", "Doe", null, "Jane", "jane@doe.com", Role.ADMIN, false, true, false, "entite-1", null));

        service.desactiver("kc-admin", "user-1");

        assertThat(cible.isActif()).isFalse();
        verify(utilisateurRepository).save(cible);
    }

    @Test
    void desactiver_utilisateurDuneAutreEntite_leveRessourceIntrouvable() {
        Entite autreEntite = Entite.builder().id("entite-2").build();
        Utilisateur cible = Utilisateur.builder().id("user-1").role(Role.COMPTABLE).actif(true).entite(autreEntite).build();
        when(utilisateurRepository.findById("user-1")).thenReturn(Optional.of(cible));

        assertThatThrownBy(() -> service.desactiver("kc-admin", "user-1"))
                .isInstanceOf(RessourceIntrouvableException.class);

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void activer_utilisateurDuneAutreEntite_leveRessourceIntrouvable() {
        Entite autreEntite = Entite.builder().id("entite-2").build();
        Utilisateur cible = Utilisateur.builder().id("user-1").role(Role.COMPTABLE).actif(false).entite(autreEntite).build();
        when(utilisateurRepository.findById("user-1")).thenReturn(Optional.of(cible));

        assertThatThrownBy(() -> service.activer("kc-admin", "user-1"))
                .isInstanceOf(RessourceIntrouvableException.class);

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void ajouterUtilisateur_emailDejaExistant_leveConflitEtNAppellePasKeycloak() {
        UtilisateurCreationForm form = new UtilisateurCreationForm("Doe", null, "Jane", "jane@doe.com", Role.COMPTABLE);
        when(utilisateurRepository.existsByEmail("jane@doe.com")).thenReturn(true);

        assertThatThrownBy(() -> service.ajouterUtilisateur("kc-admin", form))
                .isInstanceOf(ConflitException.class);

        verifyNoInteractions(keycloakAdminService);
    }
}
