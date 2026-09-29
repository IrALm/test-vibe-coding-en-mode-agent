package com.walsia.api_compta.authentification.service.impl;

import com.walsia.api_compta.authentification.service.interfaces.KeycloakAdminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Implémentation basée sur RestClient + l'Admin REST API de Keycloak, authentifiée
 * via le service account dédié (client_credentials) - cf. KC_APP_CLIENT_SECRET.
 */
@Service
public class KeycloakAdminServiceImpl implements KeycloakAdminService {

    private final RestClient restClient;

    private final String realm;
    private final String adminClientId;
    private final String adminClientSecret;

    public KeycloakAdminServiceImpl(
            @Value("${keycloak.auth-server-url}") String authServerUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.admin-client-id}") String adminClientId,
            @Value("${keycloak.admin-client-secret}") String adminClientSecret) {
        this.restClient = RestClient.create(authServerUrl);
        this.realm = realm;
        this.adminClientId = adminClientId;
        this.adminClientSecret = adminClientSecret;
    }

    @Override
    public String creerUtilisateur(NouvelUtilisateurKeycloak nouvelUtilisateur) {
        // temporary=false : le grant ROPC utilisé par le login backend-mediated (BFF) ne
        // supporte aucune étape interactive - un requiredAction UPDATE_PASSWORD (attaché
        // automatiquement par Keycloak si temporary=true) ferait échouer l'échange
        // (invalid_grant). La contrainte "mot de passe à changer" est donc gérée côté
        // application (Utilisateur.motDePasseTemporaire), pas par Keycloak.
        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", nouvelUtilisateur.motDePasseTemporaire(),
                "temporary", false
        );
        Map<String, Object> body = Map.of(
                "username", nouvelUtilisateur.email(),
                "email", nouvelUtilisateur.email(),
                "firstName", nouvelUtilisateur.prenom(),
                "lastName", nouvelUtilisateur.nom(),
                "enabled", true,
                "emailVerified", false,
                "credentials", List.of(credential)
        );

        URI location = restClient.post()
                .uri("/admin/realms/{realm}/users", realm)
                .headers(h -> h.setBearerAuth(obtenirTokenAdmin()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity()
                .getHeaders()
                .getLocation();

        if (location == null) {
            throw new IllegalStateException("Keycloak n'a pas retourné l'emplacement du nouvel utilisateur créé");
        }
        String path = location.getPath();
        String keycloakId = path.substring(path.lastIndexOf('/') + 1);

        try {
            if (!nouvelUtilisateur.realmRoles().isEmpty()) {
                assignerRolesRealm(keycloakId, nouvelUtilisateur.realmRoles());
            }
        } catch (RuntimeException e) {
            // Compense : un utilisateur Keycloak sans son rôle n'est pas un état exploitable.
            supprimerUtilisateur(keycloakId);
            throw e;
        }
        return keycloakId;
    }

    @Override
    public void reinitialiserMotDePasse(String keycloakId, String nouveauMotDePasse) {
        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", nouveauMotDePasse,
                "temporary", false
        );
        restClient.put()
                .uri("/admin/realms/{realm}/users/{id}/reset-password", realm, keycloakId)
                .headers(h -> h.setBearerAuth(obtenirTokenAdmin()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(credential)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void marquerEmailVerifie(String keycloakId, boolean verifie) {
        restClient.put()
                .uri("/admin/realms/{realm}/users/{id}", realm, keycloakId)
                .headers(h -> h.setBearerAuth(obtenirTokenAdmin()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("emailVerified", verifie))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void supprimerUtilisateur(String keycloakId) {
        restClient.delete()
                .uri("/admin/realms/{realm}/users/{id}", realm, keycloakId)
                .headers(h -> h.setBearerAuth(obtenirTokenAdmin()))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void deconnecterToutesLesSessions(String keycloakId) {
        restClient.post()
                .uri("/admin/realms/{realm}/users/{id}/logout", realm, keycloakId)
                .headers(h -> h.setBearerAuth(obtenirTokenAdmin()))
                .retrieve()
                .toBodilessEntity();
    }

    private void assignerRolesRealm(String keycloakId, List<String> realmRoles) {
        List<Map<String, Object>> rolesDisponibles = restClient.get()
                .uri("/admin/realms/{realm}/roles", realm)
                .headers(h -> h.setBearerAuth(obtenirTokenAdmin()))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        List<Map<String, Object>> rolesARattacher = rolesDisponibles.stream()
                .filter(role -> realmRoles.contains(role.get("name")))
                .toList();

        restClient.post()
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, keycloakId)
                .headers(h -> h.setBearerAuth(obtenirTokenAdmin()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(rolesARattacher)
                .retrieve()
                .toBodilessEntity();
    }

    private String obtenirTokenAdmin() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", adminClientId);
        form.add("client_secret", adminClientSecret);

        Map<String, Object> response = restClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        return (String) response.get("access_token");
    }
}
