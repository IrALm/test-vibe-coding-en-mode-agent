package com.walsia.api_compta.authentification.service.interfaces;

public interface AuthAccountService {

    /** Déclenche l'envoi d'un email de réinitialisation de mot de passe (no-op silencieux si l'email est inconnu). */
    void demanderReinitialisationMotDePasse(String email);

    /** Applique le nouveau mot de passe si le token est valide. */
    void reinitialiserMotDePasse(String token, String nouveauMotDePasse);

    /** Marque l'email comme vérifié si le token est valide. */
    void verifierEmail(String token);

    /** Renvoie un nouvel email de vérification. */
    void renvoyerEmailVerification(String email);

    /** Remplace le mot de passe temporaire par le mot de passe définitif choisi par l'utilisateur connecté. */
    void definirMotDePassePermanent(String keycloakId, String nouveauMotDePasse);
}
