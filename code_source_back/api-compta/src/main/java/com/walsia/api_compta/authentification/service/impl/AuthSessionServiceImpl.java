package com.walsia.api_compta.authentification.service.impl;

import com.walsia.api_compta.authentification.entity.UserSession;
import com.walsia.api_compta.integrationClient.dto.readDto.UtilisateurReadDto;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.exception.AuthentificationEchoueeException;
import com.walsia.api_compta.authentification.repository.UserSessionRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.authentification.service.interfaces.AuthSessionService;
import com.walsia.api_compta.authentification.service.interfaces.KeycloakAuthService;
import com.walsia.api_compta.authentification.service.interfaces.KeycloakAuthService.TokensKeycloak;
import com.walsia.api_compta.authentification.service.interfaces.TokenCipherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthSessionServiceImpl implements AuthSessionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /** Marge appliquée avant l'expiration réelle de l'access token pour déclencher un refresh préventif. */
    private static final long MARGE_EXPIRATION_SECONDES = 10;

    private final UtilisateurRepository utilisateurRepository;
    private final UserSessionRepository userSessionRepository;
    private final KeycloakAuthService keycloakAuthService;
    private final TokenCipherService tokenCipherService;

    public AuthSessionServiceImpl(
            UtilisateurRepository utilisateurRepository,
            UserSessionRepository userSessionRepository,
            KeycloakAuthService keycloakAuthService,
            TokenCipherService tokenCipherService) {
        this.utilisateurRepository = utilisateurRepository;
        this.userSessionRepository = userSessionRepository;
        this.keycloakAuthService = keycloakAuthService;
        this.tokenCipherService = tokenCipherService;
    }

    @Override
    @Transactional
    public SessionConnectee connecter(String email, String motDePasse) {
        TokensKeycloak tokens = keycloakAuthService.connecterAvecMotDePasse(email, motDePasse);

        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new AuthentificationEchoueeException("Identifiants invalides"));
        if (!utilisateur.isActif()) {
            throw new AuthentificationEchoueeException("Compte désactivé");
        }

        String tokenEnClair = genererTokenAleatoire();
        LocalDateTime maintenant = LocalDateTime.now();
        UserSession session = UserSession.builder()
                .utilisateur(utilisateur)
                .sessionTokenHash(hacher(tokenEnClair))
                .accessTokenChiffre(tokenCipherService.chiffrer(tokens.accessToken()))
                .accessTokenExpiresAt(maintenant.plusSeconds(tokens.accessTokenExpiresInSecondes()))
                .refreshTokenChiffre(tokenCipherService.chiffrer(tokens.refreshToken()))
                .refreshTokenExpiresAt(maintenant.plusSeconds(tokens.refreshTokenExpiresInSecondes()))
                .createdAt(maintenant)
                .build();
        userSessionRepository.save(session);

        // DTO volontairement partiel : le login n'a besoin de communiquer au frontend que
        // ce qui pilote la redirection post-connexion (portes email/mot de passe), pas le
        // profil complet - cf. UtilisateurConsultationService pour la vue complète via /me.
        UtilisateurReadDto infoConnexion = new UtilisateurReadDto(
                null, null, null, null, null, null, false,
                utilisateur.isEmailVerifie(), utilisateur.isMotDePasseTemporaire(), null, null);
        return new SessionConnectee(infoConnexion, tokenEnClair);
    }

    @Override
    @Transactional
    public String resoudreSession(String tokenSessionEnClair) {
        UserSession session = userSessionRepository
                .findBySessionTokenHashAndRevokedAtIsNullForUpdate(hacher(tokenSessionEnClair))
                .orElseThrow(() -> new AuthentificationEchoueeException("Session invalide"));

        LocalDateTime maintenant = LocalDateTime.now();
        if (session.getRefreshTokenExpiresAt().isBefore(maintenant)) {
            session.setRevokedAt(maintenant);
            userSessionRepository.save(session);
            throw new AuthentificationEchoueeException("Session expirée");
        }

        if (session.getAccessTokenExpiresAt().minusSeconds(MARGE_EXPIRATION_SECONDES).isAfter(maintenant)) {
            return tokenCipherService.dechiffrer(session.getAccessTokenChiffre());
        }

        if (!session.getUtilisateur().isActif()) {
            session.setRevokedAt(maintenant);
            userSessionRepository.save(session);
            throw new AuthentificationEchoueeException("Compte désactivé");
        }

        try {
            TokensKeycloak nouveauxTokens = keycloakAuthService.rafraichirJetons(
                    tokenCipherService.dechiffrer(session.getRefreshTokenChiffre()));

            session.setAccessTokenChiffre(tokenCipherService.chiffrer(nouveauxTokens.accessToken()));
            session.setAccessTokenExpiresAt(maintenant.plusSeconds(nouveauxTokens.accessTokenExpiresInSecondes()));
            session.setRefreshTokenChiffre(tokenCipherService.chiffrer(nouveauxTokens.refreshToken()));
            session.setRefreshTokenExpiresAt(maintenant.plusSeconds(nouveauxTokens.refreshTokenExpiresInSecondes()));
            userSessionRepository.save(session);

            return nouveauxTokens.accessToken();
        } catch (AuthentificationEchoueeException e) {
            session.setRevokedAt(maintenant);
            userSessionRepository.save(session);
            throw e;
        }
    }

    @Override
    @Transactional
    public void deconnecter(String tokenSessionEnClair) {
        Optional<UserSession> sessionOpt = userSessionRepository.findBySessionTokenHashAndRevokedAtIsNull(hacher(tokenSessionEnClair));
        if (sessionOpt.isEmpty()) {
            return;
        }

        UserSession session = sessionOpt.get();
        keycloakAuthService.deconnecter(tokenCipherService.dechiffrer(session.getRefreshTokenChiffre()));
        session.setRevokedAt(LocalDateTime.now());
        userSessionRepository.save(session);
    }

    private String genererTokenAleatoire() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hacher(String valeur) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(valeur.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
