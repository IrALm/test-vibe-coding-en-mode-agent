package com.walsia.compta.tenant;

import com.walsia.compta.crypto.CredentialEncryptor;
import com.walsia.compta.tenant.domain.Tenant;
import com.walsia.compta.tenant.domain.TenantStatus;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Registre des pools de connexions Hikari vers les bases tenant, indexés par
 * realm Keycloak. Les pools sont créés paresseusement, à la demande.
 * <p>
 * Ne journalise jamais les identifiants de connexion (mot de passe en clair
 * ou clé de chiffrement).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantDataSourceRegistry {

    private final CredentialEncryptor credentialEncryptor;

    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();

    /**
     * Retourne le pool de connexions mis en cache pour ce tenant, en le
     * créant paresseusement si nécessaire. Refuse d'en créer un pour un
     * tenant dont le statut n'est pas {@link TenantStatus#ACTIVE}.
     */
    public DataSource getOrCreate(Tenant tenant) {
        requireActive(tenant);
        return pools.computeIfAbsent(tenant.getKeycloakRealm(), realm -> buildPool(tenant));
    }

    /**
     * Crée un pool de connexions "transitoire" (non mis en cache, sans
     * contrôle de statut), utilisé par le provisioning pour tester la
     * connexion et exécuter la migration Flyway avant que le tenant ne soit
     * ACTIVE. L'appelant est responsable de fermer ce pool après usage.
     */
    public HikariDataSource createTransientPool(Tenant tenant) {
        return buildPool(tenant);
    }

    /**
     * Ferme et retire le pool en cache d'un tenant (rotation de credentials
     * via PATCH, ou nettoyage). Si le tenant est ACTIVE, un nouveau pool est
     * immédiatement reconstruit avec les identifiants à jour ; sinon, seule
     * la fermeture est effectuée (le pool sera recréé plus tard, lors du
     * prochain provisioning/retry).
     */
    public void rotate(Tenant tenant) {
        close(tenant.getKeycloakRealm());
        if (tenant.getStatus() == TenantStatus.ACTIVE) {
            pools.put(tenant.getKeycloakRealm(), buildPool(tenant));
        }
    }

    public void close(String realm) {
        HikariDataSource removed = pools.remove(realm);
        if (removed != null) {
            removed.close();
        }
    }

    private void requireActive(Tenant tenant) {
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Impossible de créer un pool de connexions pour le tenant realm='" + tenant.getKeycloakRealm()
                            + "' : statut=" + tenant.getStatus() + " (attendu ACTIVE)");
        }
    }

    private HikariDataSource buildPool(Tenant tenant) {
        log.info("Création du pool de connexions pour le tenant realm='{}' (db='{}')",
                tenant.getKeycloakRealm(), tenant.getDbName());
        String password = credentialEncryptor.decrypt(tenant.getDbPasswordEncrypted());
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://%s:%d/%s".formatted(tenant.getDbHost(), tenant.getDbPort(), tenant.getDbName()));
        config.setUsername(tenant.getDbUsername());
        config.setPassword(password);
        config.setPoolName("tenant-" + tenant.getKeycloakRealm());
        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }
}
