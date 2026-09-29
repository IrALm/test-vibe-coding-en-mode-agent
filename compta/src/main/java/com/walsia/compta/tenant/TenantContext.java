package com.walsia.compta.tenant;

/**
 * Contexte tenant courant, propagé via un {@link ThreadLocal} le temps du
 * traitement d'une requête HTTP.
 * <p>
 * IMPORTANT (point critique de sécurité) : {@link #clear()} doit
 * systématiquement être appelé en fin de requête (y compris en cas
 * d'exception), sous peine de fuite d'un tenant vers une requête suivante
 * traitée par le même thread.
 */
public final class TenantContext {

    private static final ThreadLocal<TenantSnapshot> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    /**
     * Instantané minimal du tenant résolu pour la requête courante.
     */
    public record TenantSnapshot(Long tenantId, String realm, String dbName) {
    }

    public static void set(TenantSnapshot snapshot) {
        CURRENT.set(snapshot);
    }

    public static TenantSnapshot get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
