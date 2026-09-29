package com.walsia.compta.tenant.admin.dto;

import com.walsia.compta.tenant.domain.ClientType;
import com.walsia.compta.tenant.domain.Tenant;
import com.walsia.compta.tenant.domain.TenantStatus;
import java.time.Instant;

/**
 * Représentation exposée d'un tenant. Ne comporte volontairement AUCUN champ
 * lié aux identifiants de connexion (ni {@code dbPasswordEncrypted}, ni
 * {@code dbPassword}).
 */
public record TenantResponse(
        Long id,
        String name,
        ClientType clientType,
        String keycloakRealm,
        String dbName,
        TenantStatus status,
        Instant createdAt
) {

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getClientType(),
                tenant.getKeycloakRealm(),
                tenant.getDbName(),
                tenant.getStatus(),
                tenant.getCreatedAt()
        );
    }
}
