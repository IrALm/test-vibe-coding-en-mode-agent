package com.walsia.compta.tenant.admin;

/**
 * Échec du provisioning d'un tenant (connexion à la base tenant impossible
 * ou échec de la migration Flyway). Le tenant est laissé en statut FAILED
 * en base pour permettre un nouveau provisioning via
 * {@code POST /admin/tenants/{id}/retry-migration}.
 */
public class TenantProvisioningException extends RuntimeException {

    public TenantProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
