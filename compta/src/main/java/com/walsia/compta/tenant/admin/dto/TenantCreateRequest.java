package com.walsia.compta.tenant.admin.dto;

import com.walsia.compta.tenant.domain.ClientType;

/**
 * Requête de création d'un tenant. {@code dbPassword} est en clair en
 * entrée uniquement : il n'est jamais journalisé ni renvoyé (voir
 * {@link TenantResponse}).
 */
public record TenantCreateRequest(
        String name,
        ClientType clientType,
        String keycloakRealm,
        String dbName,
        String dbHost,
        Integer dbPort,
        String dbUsername,
        String dbPassword
) {

    @Override
    public String toString() {
        // Ne jamais journaliser le mot de passe en clair.
        return "TenantCreateRequest[name=%s, clientType=%s, keycloakRealm=%s, dbName=%s, dbHost=%s, dbPort=%s, dbUsername=%s, dbPassword=****]"
                .formatted(name, clientType, keycloakRealm, dbName, dbHost, dbPort, dbUsername);
    }
}
