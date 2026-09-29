package com.walsia.api_compta.integrationClient.service.impl;

import com.walsia.api_compta.authentification.service.impl.AuthSessionServiceImpl;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Role;
import com.walsia.api_compta.authentification.entity.UserSession;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.exception.AuthentificationEchoueeException;
import com.walsia.api_compta.authentification.repository.UserSessionRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.authentification.service.interfaces.AuthSessionService;
import com.walsia.api_compta.authentification.service.interfaces.KeycloakAuthService;
import com.walsia.api_compta.authentification.service.interfaces.KeycloakAuthService.TokensKeycloak;
import com.walsia.api_compta.authentification.service.interfaces.TokenCipherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private KeycloakAuthService keycloakAuthService;
    @Mock
    private TokenCipherService tokenCipherService;

    private AuthSessionServiceImpl authSessionService;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        authSessionService = new AuthSessionServiceImpl(
                utilisateurRepository, userSessionRepository, keycloakAuthService, tokenCipherService);

        utilisateur = Utilisateur.builder()
                .id("user-1")
                .nom("Doe")
                .email("jane@doe.com")
                .role(Role.ADMIN)
                .actif(true)
                .keycloakId("kc-1")
                .emailVerifie(true)
                .build();

        lenient().when(tokenCipherService.chiffrer(anyString())).thenAnswer(inv -> "chiffre:" + inv.getArgument(0));
        lenient().when(tokenCipherService.dechiffrer(anyString())).thenAnswer(inv -> ((String) inv.getArgument(0)).replace("chiffre:", ""));
    }

    @Test
    void connecter_succes_persisteSessionEtRetourneToken() {
        when(keycloakAuthService.connecterAvecMotDePasse("jane@doe.com", "secret"))
                .thenReturn(new TokensKeycloak("access-jwt", 300, "refresh-jwt", 1800));
        when(utilisateurRepository.findByEmail("jane@doe.com")).thenReturn(Optional.of(utilisateur));

        AuthSessionService.SessionConnectee resultat = authSessionService.connecter("jane@doe.com", "secret");

        assertThat(resultat.utilisateur().emailVerifie()).isTrue();
        assertThat(resultat.utilisateur().motDePasseTemporaire()).isFalse();
        assertThat(resultat.utilisateur().id()).isNull();
        assertThat(resultat.utilisateur().entiteId()).isNull();
        assertThat(resultat.tokenSessionEnClair()).isNotBlank();

        ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionRepository).save(captor.capture());
        UserSession session = captor.getValue();
        assertThat(session.getUtilisateur()).isEqualTo(utilisateur);
        assertThat(session.getAccessTokenChiffre()).isEqualTo("chiffre:access-jwt");
        assertThat(session.getRefreshTokenChiffre()).isEqualTo("chiffre:refresh-jwt");
        assertThat(session.getRevokedAt()).isNull();
    }

    @Test
    void connecter_motDePasseInvalide_neConsultePasLeRepositoryLocal() {
        when(keycloakAuthService.connecterAvecMotDePasse("jane@doe.com", "mauvais"))
                .thenThrow(new AuthentificationEchoueeException("Identifiants invalides"));

        assertThatThrownBy(() -> authSessionService.connecter("jane@doe.com", "mauvais"))
                .isInstanceOf(AuthentificationEchoueeException.class);

        verifyNoInteractions(utilisateurRepository, userSessionRepository);
    }

    @Test
    void connecter_compteDesactive_rejeteApresValidationKeycloak() {
        utilisateur.setActif(false);
        when(keycloakAuthService.connecterAvecMotDePasse("jane@doe.com", "secret"))
                .thenReturn(new TokensKeycloak("access-jwt", 300, "refresh-jwt", 1800));
        when(utilisateurRepository.findByEmail("jane@doe.com")).thenReturn(Optional.of(utilisateur));

        assertThatThrownBy(() -> authSessionService.connecter("jane@doe.com", "secret"))
                .isInstanceOf(AuthentificationEchoueeException.class)
                .hasMessage("Compte désactivé");

        verifyNoInteractions(userSessionRepository);
    }

    @Test
    void resoudreSession_accessTokenEncoreValide_neRafraichitPas() {
        UserSession session = sessionValide();
        session.setAccessTokenExpiresAt(LocalDateTime.now().plusMinutes(4));
        when(userSessionRepository.findBySessionTokenHashAndRevokedAtIsNullForUpdate(anyString()))
                .thenReturn(Optional.of(session));

        String accessToken = authSessionService.resoudreSession("token-en-clair");

        assertThat(accessToken).isEqualTo("access-jwt");
        verifyNoInteractions(keycloakAuthService);
        verify(userSessionRepository, never()).save(any());
    }

    @Test
    void resoudreSession_accessTokenExpireMaisRefreshValide_rafraichitEtMetAJourLaSession() {
        UserSession session = sessionValide();
        session.setAccessTokenExpiresAt(LocalDateTime.now().minusSeconds(5));
        when(userSessionRepository.findBySessionTokenHashAndRevokedAtIsNullForUpdate(anyString()))
                .thenReturn(Optional.of(session));
        when(keycloakAuthService.rafraichirJetons("refresh-jwt"))
                .thenReturn(new TokensKeycloak("nouvel-access", 300, "nouvel-refresh", 1800));

        String accessToken = authSessionService.resoudreSession("token-en-clair");

        assertThat(accessToken).isEqualTo("nouvel-access");
        assertThat(session.getAccessTokenChiffre()).isEqualTo("chiffre:nouvel-access");
        assertThat(session.getRefreshTokenChiffre()).isEqualTo("chiffre:nouvel-refresh");
        verify(userSessionRepository).save(session);
    }

    @Test
    void resoudreSession_sessionIntrouvable_leveException() {
        when(userSessionRepository.findBySessionTokenHashAndRevokedAtIsNullForUpdate(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authSessionService.resoudreSession("token-inconnu"))
                .isInstanceOf(AuthentificationEchoueeException.class);
    }

    @Test
    void resoudreSession_refreshTokenExpire_revoqueLaSessionEtLeveException() {
        UserSession session = sessionValide();
        session.setRefreshTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(userSessionRepository.findBySessionTokenHashAndRevokedAtIsNullForUpdate(anyString()))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> authSessionService.resoudreSession("token-en-clair"))
                .isInstanceOf(AuthentificationEchoueeException.class)
                .hasMessage("Session expirée");

        assertThat(session.getRevokedAt()).isNotNull();
        verify(userSessionRepository).save(session);
    }

    @Test
    void resoudreSession_refreshEchoueCotéKeycloak_revoqueLaSession() {
        UserSession session = sessionValide();
        session.setAccessTokenExpiresAt(LocalDateTime.now().minusSeconds(5));
        when(userSessionRepository.findBySessionTokenHashAndRevokedAtIsNullForUpdate(anyString()))
                .thenReturn(Optional.of(session));
        when(keycloakAuthService.rafraichirJetons("refresh-jwt"))
                .thenThrow(new AuthentificationEchoueeException("Session expirée"));

        assertThatThrownBy(() -> authSessionService.resoudreSession("token-en-clair"))
                .isInstanceOf(AuthentificationEchoueeException.class);

        assertThat(session.getRevokedAt()).isNotNull();
    }

    @Test
    void deconnecter_sessionAbsente_neFaitRien() {
        when(userSessionRepository.findBySessionTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.empty());

        authSessionService.deconnecter("token-inconnu");

        verifyNoInteractions(keycloakAuthService);
        verify(userSessionRepository, never()).save(any());
    }

    @Test
    void deconnecter_sessionPresente_revoqueEtAppelleKeycloak() {
        UserSession session = sessionValide();
        when(userSessionRepository.findBySessionTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.of(session));

        authSessionService.deconnecter("token-en-clair");

        verify(keycloakAuthService).deconnecter("refresh-jwt");
        assertThat(session.getRevokedAt()).isNotNull();
        verify(userSessionRepository).save(session);
    }

    private UserSession sessionValide() {
        return UserSession.builder()
                .id("session-1")
                .utilisateur(utilisateur)
                .sessionTokenHash("hash")
                .accessTokenChiffre("chiffre:access-jwt")
                .accessTokenExpiresAt(LocalDateTime.now().plusMinutes(5))
                .refreshTokenChiffre("chiffre:refresh-jwt")
                .refreshTokenExpiresAt(LocalDateTime.now().plusHours(1))
                .createdAt(LocalDateTime.now())
                .build();
    }
}
