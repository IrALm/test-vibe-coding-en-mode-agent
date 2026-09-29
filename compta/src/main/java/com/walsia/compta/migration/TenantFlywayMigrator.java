package com.walsia.compta.migration;

import com.walsia.compta.tenant.TenantDataSourceRegistry;
import com.walsia.compta.tenant.domain.Tenant;
import com.walsia.compta.tenant.domain.TenantStatus;
import com.walsia.compta.tenant.repository.TenantRepository;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Au démarrage de l'application, (ré)exécute les migrations Flyway sur
 * chaque tenant ACTIVE, de façon idempotente (Flyway ne rejoue pas les
 * migrations déjà appliquées). Les tenants PROVISIONING/FAILED ne sont pas
 * traités ici : ils sont gérés via l'endpoint admin
 * {@code POST /admin/tenants/{id}/retry-migration}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFlywayMigrator implements ApplicationRunner {

    private final TenantRepository tenantRepository;
    private final TenantDataSourceRegistry dataSourceRegistry;
    private final TenantSchemaMigrationService migrationService;

    @Override
    public void run(ApplicationArguments args) {
        for (Tenant tenant : tenantRepository.findAll()) {
            if (tenant.getStatus() != TenantStatus.ACTIVE) {
                continue;
            }
            try {
                DataSource dataSource = dataSourceRegistry.getOrCreate(tenant);
                migrationService.migrate(tenant, dataSource);
            } catch (Exception e) {
                log.error("Échec de la migration Flyway au démarrage pour le tenant realm='{}'",
                        tenant.getKeycloakRealm(), e);
            }
        }
    }
}
