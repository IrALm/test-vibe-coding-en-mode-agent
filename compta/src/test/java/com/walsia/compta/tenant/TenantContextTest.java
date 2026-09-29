package com.walsia.compta.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void setThenGet_returnsSameSnapshot() {
        TenantContext.TenantSnapshot snapshot = new TenantContext.TenantSnapshot(1L, "ecole-lapereaux", "ecole_lapereaux");

        TenantContext.set(snapshot);

        assertThat(TenantContext.get()).isEqualTo(snapshot);
    }

    @Test
    void clear_removesCurrentSnapshot() {
        TenantContext.set(new TenantContext.TenantSnapshot(1L, "ecole-lapereaux", "ecole_lapereaux"));

        TenantContext.clear();

        assertThat(TenantContext.get()).isNull();
    }

    @Test
    void get_returnsNullWhenNothingSet() {
        assertThat(TenantContext.get()).isNull();
    }
}
