package com.walsia.compta.tenant.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.walsia.compta.crypto.CredentialEncryptor;
import com.walsia.compta.migration.TenantSchemaMigrationService;
import com.walsia.compta.tenant.TenantDataSourceRegistry;
import com.walsia.compta.tenant.admin.dto.TenantCreateRequest;
import com.walsia.compta.tenant.admin.dto.TenantResponse;
import com.walsia.compta.tenant.admin.dto.TenantUpdateRequest;
import com.walsia.compta.tenant.domain.ClientType;
import com.walsia.compta.tenant.domain.Tenant;
import com.walsia.compta.tenant.domain.TenantStatus;
import com.walsia.compta.tenant.repository.TenantRepository;
import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

class TenantProvisioningServiceTest {

    private TenantRepository tenantRepository;
    private CredentialEncryptor credentialEncryptor;
    private TenantDataSourceRegistry dataSourceRegistry;
    private TenantSchemaMigrationService migrationService;
    private PlatformTransactionManager transactionManager;

    private TenantProvisioningService service;

    private final Map<Long, Tenant> store = new HashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantRepository.class);
        credentialEncryptor = mock(CredentialEncryptor.class);
        dataSourceRegistry = mock(TenantDataSourceRegistry.class);
        migrationService = mock(TenantSchemaMigrationService.class);
        transactionManager = mock(PlatformTransactionManager.class);

        // TransactionTemplate exécute directement le callback : on simule une
        // transaction "transparente" (commit/rollback ne font rien ici).
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));

        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            if (tenant.getId() == null) {
                tenant.setId(idSequence.getAndIncrement());
            }
            store.put(tenant.getId(), tenant);
            return tenant;
        });
        when(tenantRepository.findById(anyLong())).thenAnswer(invocation ->
                Optional.ofNullable(store.get(invocation.getArgument(0, Long.class))));

        when(credentialEncryptor.encrypt(any())).thenReturn("encrypted-value");

        service = new TenantProvisioningService(
                tenantRepository, credentialEncryptor, dataSourceRegistry, migrationService, transactionManager, "admin");
    }

    private TenantCreateRequest sampleRequest() {
        return new TenantCreateRequest(
                "École LAPEREAUX", ClientType.ECOLE, "ecole-lapereaux", "ecole_lapereaux",
                "localhost", 5432, "ecole_lapereaux_owner", "s3cret");
    }

    @Test
    void create_activatesTenant_whenMigrationSucceeds() {
        when(tenantRepository.existsByKeycloakRealmOrDbName(any(), any())).thenReturn(false);
        HikariDataSource pool = mock(HikariDataSource.class);
        when(dataSourceRegistry.createTransientPool(any(Tenant.class))).thenReturn(pool);
        // migrationService.migrate(...) : ne fait rien (succès) par défaut avec un mock void.

        TenantResponse response = service.create(sampleRequest());

        assertThat(response.status()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(response.keycloakRealm()).isEqualTo("ecole-lapereaux");
        assertThat(store.get(response.id()).getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void create_marksTenantFailed_andThrows_whenMigrationFails() {
        when(tenantRepository.existsByKeycloakRealmOrDbName(any(), any())).thenReturn(false);
        HikariDataSource pool = mock(HikariDataSource.class);
        when(dataSourceRegistry.createTransientPool(any(Tenant.class))).thenReturn(pool);
        doThrow(new RuntimeException("connexion impossible"))
                .when(migrationService).migrate(any(Tenant.class), any());

        assertThatThrownBy(() -> service.create(sampleRequest()))
                .isInstanceOf(TenantProvisioningException.class);

        assertThat(store).hasSize(1);
        Tenant persisted = store.values().iterator().next();
        assertThat(persisted.getStatus()).isEqualTo(TenantStatus.FAILED);
    }

    @Test
    void create_rejectsDuplicateRealmOrDbName() {
        when(tenantRepository.existsByKeycloakRealmOrDbName(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.create(sampleRequest()))
                .isInstanceOf(TenantAlreadyExistsException.class);

        assertThat(store).isEmpty();
    }

    @Test
    void retryMigration_reactivatesFailedTenant_whenMigrationNowSucceeds() {
        Tenant failedTenant = Tenant.builder()
                .id(42L)
                .name("École LAPEREAUX")
                .clientType(ClientType.ECOLE)
                .keycloakRealm("ecole-lapereaux")
                .dbName("ecole_lapereaux")
                .dbHost("localhost")
                .dbPort(5432)
                .dbUsername("ecole_lapereaux_owner")
                .dbPasswordEncrypted("encrypted-value")
                .status(TenantStatus.FAILED)
                .build();
        store.put(42L, failedTenant);
        HikariDataSource pool = mock(HikariDataSource.class);
        when(dataSourceRegistry.createTransientPool(any(Tenant.class))).thenReturn(pool);

        TenantResponse response = service.retryMigration(42L);

        assertThat(response.status()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void retryMigration_rejectsWhenTenantNotFailed() {
        Tenant activeTenant = Tenant.builder()
                .id(7L)
                .keycloakRealm("ecole-lapereaux")
                .dbName("ecole_lapereaux")
                .status(TenantStatus.ACTIVE)
                .build();
        store.put(7L, activeTenant);

        assertThatThrownBy(() -> service.retryMigration(7L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void create_rejectsKeycloakRealmEqualToAdminRealm() {
        TenantCreateRequest request = new TenantCreateRequest(
                "École LAPEREAUX", ClientType.ECOLE, "admin", "ecole_lapereaux",
                "localhost", 5432, "ecole_lapereaux_owner", "s3cret");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("admin");
        assertThat(store).isEmpty();
    }

    @Test
    void create_rejectsInvalidKeycloakRealmFormat() {
        TenantCreateRequest request = new TenantCreateRequest(
                "École LAPEREAUX", ClientType.ECOLE, "Ecole LAPEREAUX!", "ecole_lapereaux",
                "localhost", 5432, "ecole_lapereaux_owner", "s3cret");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keycloakRealm");
        assertThat(store).isEmpty();
    }

    @Test
    void create_rejectsInvalidDbNameFormat() {
        TenantCreateRequest request = new TenantCreateRequest(
                "École LAPEREAUX", ClientType.ECOLE, "ecole-lapereaux", "1_invalid db",
                "localhost", 5432, "ecole_lapereaux_owner", "s3cret");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dbName");
        assertThat(store).isEmpty();
    }

    @Test
    void create_rejectsDbPortAboveUpperBound() {
        TenantCreateRequest request = new TenantCreateRequest(
                "École LAPEREAUX", ClientType.ECOLE, "ecole-lapereaux", "ecole_lapereaux",
                "localhost", 70000, "ecole_lapereaux_owner", "s3cret");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dbPort");
        assertThat(store).isEmpty();
    }

    @Test
    void update_rejectsBlankName() {
        Tenant tenant = Tenant.builder()
                .id(9L)
                .name("École LAPEREAUX")
                .keycloakRealm("ecole-lapereaux")
                .dbName("ecole_lapereaux")
                .status(TenantStatus.ACTIVE)
                .build();
        store.put(9L, tenant);

        assertThatThrownBy(() -> service.update(9L, new TenantUpdateRequest("   ", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void create_marksTenantFailed_evenWhenFailedStatusTransitionAlsoFails() {
        when(tenantRepository.existsByKeycloakRealmOrDbName(any(), any())).thenReturn(false);
        HikariDataSource pool = mock(HikariDataSource.class);
        when(dataSourceRegistry.createTransientPool(any(Tenant.class))).thenReturn(pool);
        doThrow(new RuntimeException("connexion impossible"))
                .when(migrationService).migrate(any(Tenant.class), any());

        // Simule une base admin momentanément indisponible lors de la
        // tentative de bascule du statut vers FAILED : la deuxième invocation
        // de save() (celle qui persiste PROVISIONING -> FAILED) échoue aussi.
        when(tenantRepository.save(any(Tenant.class)))
                .thenAnswer(invocation -> {
                    Tenant tenant = invocation.getArgument(0);
                    if (tenant.getId() == null) {
                        tenant.setId(idSequence.getAndIncrement());
                        store.put(tenant.getId(), tenant);
                        return tenant;
                    }
                    throw new RuntimeException("base admin indisponible");
                });

        // L'exception métier d'origine (échec de migration) doit être
        // propagée, pas masquée par l'échec de la transition vers FAILED.
        assertThatThrownBy(() -> service.create(sampleRequest()))
                .isInstanceOf(TenantProvisioningException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .cause().hasMessageContaining("connexion impossible");
    }
}
