package com.walsia.compta.tenant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.walsia.compta.crypto.CredentialEncryptor;
import com.walsia.compta.tenant.domain.Tenant;
import com.walsia.compta.tenant.domain.TenantStatus;
import org.junit.jupiter.api.Test;

class TenantDataSourceRegistryTest {

    private final CredentialEncryptor credentialEncryptor = mock(CredentialEncryptor.class);
    private final TenantDataSourceRegistry registry = new TenantDataSourceRegistry(credentialEncryptor);

    @Test
    void getOrCreate_refusesToBuildPool_whenTenantNotActive() {
        Tenant tenant = Tenant.builder()
                .keycloakRealm("ecole-lapereaux")
                .dbName("ecole_lapereaux")
                .status(TenantStatus.PROVISIONING)
                .build();

        assertThatThrownBy(() -> registry.getOrCreate(tenant))
                .isInstanceOf(IllegalStateException.class);
    }
}
