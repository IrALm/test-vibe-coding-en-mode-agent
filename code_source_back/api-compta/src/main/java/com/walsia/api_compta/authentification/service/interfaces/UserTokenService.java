package com.walsia.api_compta.authentification.service.interfaces;

import com.walsia.api_compta.authentification.entity.UserTokenType;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;

/**
 * Génération et validation des tokens à usage unique (vérification email, reset password).
 * Seul le hash du token est persisté ; le token en clair n'est jamais stocké.
 */
public interface UserTokenService {

    /** Génère un token, persiste son hash avec expiration, et retourne le token en clair (à mettre dans le lien envoyé par mail). */
    String genererToken(Utilisateur utilisateur, UserTokenType type);

    /** Valide le token (existant, non expiré, non utilisé), le marque consommé et retourne l'utilisateur associé. */
    Utilisateur consommerToken(String tokenEnClair, UserTokenType type);
}
