package com.walsia.api_compta.authentification.service.interfaces;

import com.walsia.api_compta.integrationClient.dto.readDto.UtilisateurReadDto;

/**
 * Login/logout backend-mediated (BFF) : le frontend ne voit jamais le JWT
 * Keycloak, seulement un token de session opaque posé dans un cookie httpOnly.
 * Les tokens Keycloak (access + refresh) sont chiffrés en base (UserSession).
 */
public interface AuthSessionService {

    record SessionConnectee(UtilisateurReadDto utilisateur, String tokenSessionEnClair) {}

    /** Authentifie contre Keycloak, persiste une nouvelle session et retourne le token opaque à mettre dans le cookie. */
    SessionConnectee connecter(String email, String motDePasse);

    /**
     * Résout un token de session en un access token JWT Keycloak valide (rafraîchi
     * si besoin). Lève AuthentificationEchoueeException si la session est invalide,
     * révoquée, expirée, ou si le compte a été désactivé depuis.
     */
    String resoudreSession(String tokenSessionEnClair);

    /** Révoque la session (Keycloak + local). Idempotent : ne lève pas si la session est déjà invalide/absente. */
    void deconnecter(String tokenSessionEnClair);
}
