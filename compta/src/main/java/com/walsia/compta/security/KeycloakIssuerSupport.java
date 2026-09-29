package com.walsia.compta.security;

/**
 * Extraction du nom de realm Keycloak à partir d'un issuer (claim {@code iss}
 * ou issuer-location), au format {@code {base-url}/realms/{realm}}.
 */
public final class KeycloakIssuerSupport {

    private static final String REALMS_SEGMENT = "/realms/";

    private KeycloakIssuerSupport() {
    }

    public static String extractRealm(String issuer) {
        if (issuer == null) {
            return null;
        }
        int idx = issuer.lastIndexOf(REALMS_SEGMENT);
        if (idx < 0) {
            return null;
        }
        String tail = issuer.substring(idx + REALMS_SEGMENT.length());
        int slash = tail.indexOf('/');
        String realm = slash >= 0 ? tail.substring(0, slash) : tail;
        return realm.isBlank() ? null : realm;
    }
}
