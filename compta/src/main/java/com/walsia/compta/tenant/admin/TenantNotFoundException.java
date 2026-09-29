package com.walsia.compta.tenant.admin;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(Long id) {
        super("Tenant introuvable : id=" + id);
    }
}
