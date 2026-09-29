package com.walsia.compta.tenant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entité JPA persistée dans l'annuaire (base admin postgres_admin_db).
 * Représente un tenant de l'ERP OHADA (une base de données dédiée par
 * tenant, adressée via {@link #dbName} et un realm Keycloak dédié).
 */
@Entity
@Table(
        name = "tenants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tenants_keycloak_realm", columnNames = "keycloak_realm"),
                @UniqueConstraint(name = "uk_tenants_db_name", columnNames = "db_name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, length = 20)
    private ClientType clientType;

    @Column(name = "keycloak_realm", nullable = false, length = 100)
    private String keycloakRealm;

    @Column(name = "db_name", nullable = false, length = 100)
    private String dbName;

    @Column(name = "db_host", nullable = false)
    private String dbHost;

    @Column(name = "db_port", nullable = false)
    private Integer dbPort;

    @Column(name = "db_username", nullable = false, length = 100)
    private String dbUsername;

    /**
     * Mot de passe de connexion à la base tenant, chiffré (AES-256-GCM) via
     * {@link com.walsia.compta.crypto.CredentialEncryptor}. Ne jamais exposer
     * ce champ en dehors de la couche persistance/provisioning.
     */
    @Column(name = "db_password_encrypted", nullable = false, columnDefinition = "TEXT")
    private String dbPasswordEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = TenantStatus.PROVISIONING;
        }
    }
}
