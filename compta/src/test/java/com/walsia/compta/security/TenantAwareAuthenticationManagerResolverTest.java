package com.walsia.compta.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.walsia.compta.tenant.domain.Tenant;
import com.walsia.compta.tenant.domain.TenantStatus;
import com.walsia.compta.tenant.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

/**
 * Vérifie en particulier le correctif du point BLOQUANT de sécurité : un JWT
 * portant un nom de realm connu de l'annuaire mais un issuer complet dont
 * l'hôte diverge de la base URL Keycloak de confiance
 * ({@code compta.keycloak.base-url}) doit être rejeté AVANT toute tentative
 * de résolution réseau (pas d'appel à {@code JwtDecoders.fromIssuerLocation}
 * avec la valeur fournie par l'attaquant). La structure même de
 * {@link TenantAwareAuthenticationManagerResolver#resolve} garantit ceci :
 * l'exception est levée de façon synchrone dans {@code resolve(...)}, avant
 * tout appel à la méthode privée qui construit le {@link AuthenticationManager}
 * (et donc avant tout appel à {@code JwtDecoders.fromIssuerLocation}).
 */
class TenantAwareAuthenticationManagerResolverTest {

    private static final String TRUSTED_BASE_URL = "http://localhost:8080";
    private static final String ADMIN_REALM = "admin";
    private static final String TENANT_REALM = "ecole-lapereaux";

    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final TenantAwareAuthenticationManagerResolver resolver =
            new TenantAwareAuthenticationManagerResolver(tenantRepository, TRUSTED_BASE_URL, ADMIN_REALM);

    @Test
    void resolve_rejectsSpoofedIssuer_withKnownTenantRealmNameButAttackerHost() {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .keycloakRealm(TENANT_REALM)
                .dbName("ecole_lapereaux")
                .status(TenantStatus.ACTIVE)
                .build();
        when(tenantRepository.findByKeycloakRealm(TENANT_REALM)).thenReturn(Optional.of(tenant));

        // Nom de realm réel et connu de l'annuaire, mais host d'issuer
        // usurpé : c'est exactement le scénario SSRF/spoofing décrit dans la
        // revue de sécurité.
        HttpServletRequest request = requestWithBearerToken(
                "https://attacker.example/realms/" + TENANT_REALM);

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(InvalidBearerTokenException.class);

        // Le realm a bien été consulté (pour identifier le realm candidat),
        // mais aucune tentative de résolution réseau (JwtDecoders) n'a pu
        // avoir lieu : l'exception est levée avant tout appel à la méthode
        // qui construit l'AuthenticationManager / le JwtDecoder.
        verify(tenantRepository).findByKeycloakRealm(TENANT_REALM);
    }

    @Test
    void resolve_rejectsSpoofedIssuer_withKnownAdminRealmNameButAttackerHost() {
        HttpServletRequest request = requestWithBearerToken(
                "https://attacker.example/realms/" + ADMIN_REALM);

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(InvalidBearerTokenException.class);

        // Le realm admin ne nécessite pas de recherche en base ; on vérifie
        // simplement qu'aucun realm tenant n'a été recherché par erreur.
        verify(tenantRepository, never()).findByKeycloakRealm(ADMIN_REALM);
    }

    @Test
    void resolve_rejectsUnknownRealm() {
        when(tenantRepository.findByKeycloakRealm("realm-inconnu")).thenReturn(Optional.empty());
        HttpServletRequest request = requestWithBearerToken(TRUSTED_BASE_URL + "/realms/realm-inconnu");

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void resolve_rejectsRequest_withoutBearerToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    /**
     * Cas limite : même hôte et même nom de realm, mais schéma HTTP
     * différent (http vs https) — doit être rejeté par l'égalité stricte de
     * chaîne, sans normalisation implicite entre schémas.
     */
    @Test
    void resolve_rejectsIssuer_withDifferentSchemeThanTrustedBaseUrl() {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .keycloakRealm(TENANT_REALM)
                .dbName("ecole_lapereaux")
                .status(TenantStatus.ACTIVE)
                .build();
        when(tenantRepository.findByKeycloakRealm(TENANT_REALM)).thenReturn(Optional.of(tenant));

        HttpServletRequest request = requestWithBearerToken("https://localhost:8080/realms/" + TENANT_REALM);

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    private HttpServletRequest requestWithBearerToken(String issuer) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + unsignedJwtWithIssuer(issuer));
        return request;
    }

    private String unsignedJwtWithIssuer(String issuer) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject("attacker")
                    .build();
            return new PlainJWT(claims).serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
