package com.walsia.api_compta.authentification.service.impl;

import com.walsia.api_compta.exception.AuthentificationEchoueeException;
import com.walsia.api_compta.authentification.service.interfaces.KeycloakAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * Implémentation basée sur RestClient + le endpoint token/logout OIDC de Keycloak,
 * authentifiée via le client confidentiel dédié api-compta-bff (Resource Owner
 * Password Credentials) - cf. KC_BFF_CLIENT_SECRET.
 */
@Service
public class KeycloakAuthServiceImpl implements KeycloakAuthService {

    private final RestClient restClient;

    private final String realm;
    private final String bffClientId;
    private final String bffClientSecret;

    public KeycloakAuthServiceImpl(
            @Value("${keycloak.auth-server-url}") String authServerUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.bff-client-id}") String bffClientId,
            @Value("${keycloak.bff-client-secret}") String bffClientSecret) {
        this.restClient = RestClient.create(authServerUrl);
        this.realm = realm;
        this.bffClientId = bffClientId;
        this.bffClientSecret = bffClientSecret;
    }

    @Override
    public TokensKeycloak connecterAvecMotDePasse(String email, String motDePasse) {
        MultiValueMap<String, String> form = formDeBase();
        form.add("grant_type", "password");
        form.add("username", email);
        form.add("password", motDePasse);
        return demanderJetons(form, "Identifiants invalides");
    }

    @Override
    public TokensKeycloak rafraichirJetons(String refreshToken) {
        MultiValueMap<String, String> form = formDeBase();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        return demanderJetons(form, "Session expirée");
    }

    @Override
    public void deconnecter(String refreshToken) {
        MultiValueMap<String, String> form = formDeBase();
        form.add("refresh_token", refreshToken);
        try {
            restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/logout", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            // Best-effort : une session déjà expirée/révoquée côté Keycloak ne doit pas empêcher le logout local.
        }
    }

    private TokensKeycloak demanderJetons(MultiValueMap<String, String> form, String messageEchec) {
        Map<String, Object> reponse;
        try {
            reponse = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException e) {
            throw new AuthentificationEchoueeException(messageEchec);
        }

        return new TokensKeycloak(
                (String) reponse.get("access_token"),
                ((Number) reponse.get("expires_in")).longValue(),
                (String) reponse.get("refresh_token"),
                ((Number) reponse.get("refresh_expires_in")).longValue()
        );
    }

    private MultiValueMap<String, String> formDeBase() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", bffClientId);
        form.add("client_secret", bffClientSecret);
        return form;
    }
}
