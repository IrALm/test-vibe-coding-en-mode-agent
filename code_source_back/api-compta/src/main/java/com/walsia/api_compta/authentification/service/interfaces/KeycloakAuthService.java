package com.walsia.api_compta.authentification.service.interfaces;

/**
 * Authentification d'un utilisateur final via Keycloak, pour le login/logout
 * backend-mediated (BFF). Utilise le client confidentiel dédié api-compta-bff
 * (Resource Owner Password Credentials) - jamais KeycloakAdminService, réservé
 * au service-account d'administration.
 */
public interface KeycloakAuthService {

    record TokensKeycloak(
            String accessToken,
            long accessTokenExpiresInSecondes,
            String refreshToken,
            long refreshTokenExpiresInSecondes
    ) {}

    /** Échange email/mot de passe contre des tokens. Lève AuthentificationEchoueeException si invalide. */
    TokensKeycloak connecterAvecMotDePasse(String email, String motDePasse);

    /** Échange un refresh token contre de nouveaux tokens. Lève AuthentificationEchoueeException si le refresh token n'est plus valide. */
    TokensKeycloak rafraichirJetons(String refreshToken);

    /** Révoque le refresh token côté Keycloak (best-effort, ne lève pas si déjà invalide). */
    void deconnecter(String refreshToken);
}
