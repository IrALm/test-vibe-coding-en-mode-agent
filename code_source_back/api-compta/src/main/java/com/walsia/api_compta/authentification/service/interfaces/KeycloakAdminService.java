package com.walsia.api_compta.authentification.service.interfaces;

import java.util.List;

/**
 * Orchestration des opérations d'administration Keycloak (realm erp-comptable)
 * nécessaires à la création et à la gestion des comptes utilisateurs.
 * Utilise le client de service dédié (api-compta-admin), pas le compte super-admin.
 */
public interface KeycloakAdminService {

    record NouvelUtilisateurKeycloak(
            String email,
            String prenom,
            String nom,
            String motDePasseTemporaire,
            List<String> realmRoles
    ) {}

    /** Crée l'utilisateur dans Keycloak avec un mot de passe temporaire et retourne son id Keycloak. */
    String creerUtilisateur(NouvelUtilisateurKeycloak nouvelUtilisateur);

    /** Définit un nouveau mot de passe (utilisé pour la réinitialisation "mot de passe oublié"). */
    void reinitialiserMotDePasse(String keycloakId, String nouveauMotDePasse);

    /** Marque l'email de l'utilisateur comme vérifié (ou non) dans Keycloak. */
    void marquerEmailVerifie(String keycloakId, boolean verifie);

    /** Supprime l'utilisateur Keycloak (compensation si la création locale échoue après coup). */
    void supprimerUtilisateur(String keycloakId);

    /** Révoque toutes les sessions/refresh tokens Keycloak actifs de l'utilisateur (ex. après réinitialisation de mot de passe). */
    void deconnecterToutesLesSessions(String keycloakId);
}
