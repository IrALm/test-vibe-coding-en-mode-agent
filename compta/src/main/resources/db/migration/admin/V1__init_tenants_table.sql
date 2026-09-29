-- Annuaire des tenants (base postgres_admin_db).
CREATE TABLE public.tenants (
    id                     BIGSERIAL PRIMARY KEY,
    name                   VARCHAR(255) NOT NULL,
    client_type            VARCHAR(20)  NOT NULL
        CONSTRAINT ck_tenants_client_type CHECK (client_type IN ('ECOLE', 'HOPITAL', 'ONG', 'PME')),
    keycloak_realm         VARCHAR(100) NOT NULL,
    db_name                VARCHAR(100) NOT NULL,
    db_host                VARCHAR(255) NOT NULL,
    db_port                INTEGER      NOT NULL,
    db_username            VARCHAR(100) NOT NULL,
    db_password_encrypted  TEXT         NOT NULL,
    status                 VARCHAR(20)  NOT NULL DEFAULT 'PROVISIONING'
        CONSTRAINT ck_tenants_status CHECK (status IN ('PROVISIONING', 'ACTIVE', 'FAILED')),
    created_at             TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT uk_tenants_keycloak_realm UNIQUE (keycloak_realm),
    CONSTRAINT uk_tenants_db_name UNIQUE (db_name)
);
