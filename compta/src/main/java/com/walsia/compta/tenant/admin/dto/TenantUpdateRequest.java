package com.walsia.compta.tenant.admin.dto;

/**
 * Requête de mise à jour d'un tenant : uniquement le nom et/ou la rotation
 * du mot de passe de connexion à la base tenant. {@code keycloakRealm} et
 * {@code dbName} ne sont volontairement pas modifiables dans ce lot.
 */
public record TenantUpdateRequest(
        String name,
        String dbPassword
) {

    @Override
    public String toString() {
        return "TenantUpdateRequest[name=%s, dbPassword=%s]"
                .formatted(name, dbPassword != null ? "****" : null);
    }
}
