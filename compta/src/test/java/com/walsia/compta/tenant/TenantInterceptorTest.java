package com.walsia.compta.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.walsia.compta.tenant.domain.Tenant;
import com.walsia.compta.tenant.domain.TenantStatus;
import com.walsia.compta.tenant.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Vérifie en particulier le point critique de sécurité : afterCompletion
 * doit vider le TenantContext dans TOUS les cas, y compris en présence
 * d'une exception.
 */
class TenantInterceptorTest {

    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final TenantInterceptor interceptor = new TenantInterceptor(tenantRepository);

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void preHandle_setsTenantContext_forActiveKnownTenant() throws Exception {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .keycloakRealm("ecole-lapereaux")
                .dbName("ecole_lapereaux")
                .status(TenantStatus.ACTIVE)
                .build();
        when(tenantRepository.findByKeycloakRealm("ecole-lapereaux")).thenReturn(Optional.of(tenant));
        authenticateAs("http://localhost:8080/realms/ecole-lapereaux");

        boolean result = interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), new Object());

        assertThat(result).isTrue();
        assertThat(TenantContext.get()).isNotNull();
        assertThat(TenantContext.get().realm()).isEqualTo("ecole-lapereaux");
    }

    @Test
    void preHandle_rejects_whenTenantNotActive() throws Exception {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .keycloakRealm("ecole-lapereaux")
                .dbName("ecole_lapereaux")
                .status(TenantStatus.PROVISIONING)
                .build();
        when(tenantRepository.findByKeycloakRealm("ecole-lapereaux")).thenReturn(Optional.of(tenant));
        authenticateAs("http://localhost:8080/realms/ecole-lapereaux");

        boolean result = interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), new Object());

        assertThat(result).isFalse();
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    void afterCompletion_clearsTenantContext_evenWhenExceptionIsNotNull() {
        TenantContext.set(new TenantContext.TenantSnapshot(1L, "ecole-lapereaux", "ecole_lapereaux"));

        interceptor.afterCompletion(
                mock(HttpServletRequest.class),
                mock(HttpServletResponse.class),
                new Object(),
                new RuntimeException("boom"));

        assertThat(TenantContext.get()).isNull();
    }

    @Test
    void afterCompletion_clearsTenantContext_whenNoException() {
        TenantContext.set(new TenantContext.TenantSnapshot(1L, "ecole-lapereaux", "ecole_lapereaux"));

        interceptor.afterCompletion(mock(HttpServletRequest.class), mock(HttpServletResponse.class), new Object(), null);

        assertThat(TenantContext.get()).isNull();
    }

    private void authenticateAs(String issuer) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("iss", issuer)
                .subject("user")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
