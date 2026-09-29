package com.walsia.compta.tenant;

import com.walsia.compta.tenant.domain.Tenant;
import com.walsia.compta.tenant.repository.TenantRepository;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

/**
 * DataSource de routage qui aiguille chaque connexion vers la base du
 * tenant courant (résolu via {@link TenantContext}).
 * <p>
 * Infrastructure préparée pour un futur lot qui exploitera les données
 * tenant via JPA/JDBC (aucune entité métier n'existe encore côté tenant
 * dans ce lot) : non consommée par la codebase à ce stade, mais fournie
 * conformément à la conception du socle multi-tenant.
 * <p>
 * Note d'implémentation : les tenants étant provisionnés dynamiquement à
 * l'exécution (API admin), {@link #determineTargetDataSource()} est
 * volontairement surchargée pour déléguer à {@link TenantDataSourceRegistry}
 * plutôt que de s'appuyer sur la table statique {@code targetDataSources} de
 * {@link AbstractRoutingDataSource} (qui nécessiterait un redémarrage pour
 * prendre en compte un nouveau tenant).
 */
@Component
@RequiredArgsConstructor
public class TenantAwareDataSource extends AbstractRoutingDataSource {

    private final TenantDataSourceRegistry registry;
    private final TenantRepository tenantRepository;

    @PostConstruct
    void init() {
        setTargetDataSources(Map.of());
        afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        TenantContext.TenantSnapshot snapshot = TenantContext.get();
        return snapshot != null ? snapshot.realm() : null;
    }

    @Override
    protected DataSource determineTargetDataSource() {
        TenantContext.TenantSnapshot snapshot = TenantContext.get();
        if (snapshot == null) {
            throw new IllegalStateException("Aucun tenant courant : TenantContext non initialisé");
        }
        Tenant tenant = tenantRepository.findByKeycloakRealm(snapshot.realm())
                .orElseThrow(() -> new IllegalStateException("Tenant inconnu pour le realm '" + snapshot.realm() + "'"));
        return registry.getOrCreate(tenant);
    }
}
