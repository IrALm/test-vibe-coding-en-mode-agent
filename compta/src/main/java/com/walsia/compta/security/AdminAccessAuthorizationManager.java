package com.walsia.compta.security;

import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

/**
 * Double contrôle d'accès pour {@code /admin/**} : le rôle
 * {@code SUPER_ADMIN} ne suffit PAS à lui seul, l'issuer du jeton doit en
 * plus correspondre strictement au realm admin configuré
 * ({@code compta.keycloak.admin-realm}). Sans ce second contrôle, un realm
 * tenant mal configuré (attribuant par erreur un rôle nommé SUPER_ADMIN)
 * pourrait usurper les droits d'administration.
 */
@Component
public class AdminAccessAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final String SUPER_ADMIN_AUTHORITY = "ROLE_SUPER_ADMIN";

    private final String expectedAdminIssuer;

    public AdminAccessAuthorizationManager(
            @Value("${compta.keycloak.base-url}") String keycloakBaseUrl,
            @Value("${compta.keycloak.admin-realm}") String adminRealm) {
        this.expectedAdminIssuer = keycloakBaseUrl.replaceAll("/+$", "") + "/realms/" + adminRealm;
    }

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth) || !auth.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        boolean hasSuperAdminRole = jwtAuth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(SUPER_ADMIN_AUTHORITY::equals);

        String issuer = jwtAuth.getToken().getIssuer() != null ? jwtAuth.getToken().getIssuer().toString() : null;
        boolean issuerMatchesAdminRealm = expectedAdminIssuer.equals(issuer);

        return new AuthorizationDecision(hasSuperAdminRole && issuerMatchesAdminRealm);
    }
}
