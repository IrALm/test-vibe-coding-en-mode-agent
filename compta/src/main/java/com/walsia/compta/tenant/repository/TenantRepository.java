package com.walsia.compta.tenant.repository;

import com.walsia.compta.tenant.domain.Tenant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByKeycloakRealm(String keycloakRealm);

    boolean existsByKeycloakRealmOrDbName(String keycloakRealm, String dbName);
}
