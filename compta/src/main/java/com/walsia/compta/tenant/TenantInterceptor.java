package com.walsia.compta.tenant;

import com.walsia.compta.security.KeycloakIssuerSupport;
import com.walsia.compta.tenant.domain.Tenant;
import com.walsia.compta.tenant.domain.TenantStatus;
import com.walsia.compta.tenant.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Résout le tenant courant à partir du JWT déjà authentifié (aucun
 * re-parsing brut du token) et l'expose via {@link TenantContext} le temps
 * de la requête.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantRepository tenantRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentification requise");
            return false;
        }

        Jwt jwt = jwtAuthentication.getToken();
        String realm = KeycloakIssuerSupport.extractRealm(jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
        if (realm == null) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Realm introuvable dans le jeton");
            return false;
        }

        Tenant tenant = tenantRepository.findByKeycloakRealm(realm).orElse(null);
        if (tenant == null) {
            log.warn("Requête rejetée : aucun tenant connu pour le realm '{}'", realm);
            response.sendError(HttpStatus.FORBIDDEN.value(), "Tenant inconnu");
            return false;
        }
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            log.warn("Requête rejetée : tenant '{}' non actif (status={})", realm, tenant.getStatus());
            response.sendError(HttpStatus.FORBIDDEN.value(), "Tenant non actif");
            return false;
        }

        TenantContext.set(new TenantContext.TenantSnapshot(tenant.getId(), tenant.getKeycloakRealm(), tenant.getDbName()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Appel inconditionnel : même en cas d'exception, le contexte tenant
        // ne doit jamais fuiter vers la requête suivante traitée par ce thread.
        TenantContext.clear();
    }
}
