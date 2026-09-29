package com.walsia.compta.tenant.admin;

public class TenantAlreadyExistsException extends RuntimeException {

    public TenantAlreadyExistsException(String keycloakRealm, String dbName) {
        super("Un tenant existe déjà avec keycloakRealm='" + keycloakRealm + "' ou dbName='" + dbName + "'");
    }
}
