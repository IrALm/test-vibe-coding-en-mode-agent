package com.walsia.compta.migration;

import com.walsia.compta.tenant.domain.Tenant;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Exécute la migration Flyway "standalone" (indépendante du Flyway
 * auto-configuré par Spring Boot pour l'annuaire) sur le pool d'un tenant
 * donné. Réutilisée à la fois par {@link TenantFlywayMigrator} (démarrage)
 * et par le provisioning (création d'un nouveau tenant).
 */
@Slf4j
@Service
public class TenantSchemaMigrationService {

    private final String tenantMigrationLocations;

    public TenantSchemaMigrationService(
            @Value("${compta.tenant.flyway.locations}") String tenantMigrationLocations) {
        this.tenantMigrationLocations = tenantMigrationLocations;
    }

    public void migrate(Tenant tenant, DataSource dataSource) {
        log.info("Migration Flyway du schéma tenant realm='{}' db='{}'", tenant.getKeycloakRealm(), tenant.getDbName());
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(tenantMigrationLocations)
                .load();
        flyway.migrate();
    }
}
