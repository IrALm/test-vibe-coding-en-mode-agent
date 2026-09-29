package com.walsia.compta.tenant.admin;

import com.walsia.compta.crypto.CredentialEncryptor;
import com.walsia.compta.migration.TenantSchemaMigrationService;
import com.walsia.compta.tenant.TenantDataSourceRegistry;
import com.walsia.compta.tenant.admin.dto.TenantCreateRequest;
import com.walsia.compta.tenant.admin.dto.TenantResponse;
import com.walsia.compta.tenant.admin.dto.TenantUpdateRequest;
import com.walsia.compta.tenant.domain.Tenant;
import com.walsia.compta.tenant.domain.TenantStatus;
import com.walsia.compta.tenant.repository.TenantRepository;
import com.zaxxer.hikari.HikariDataSource;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Orchestre le provisioning d'un tenant :
 * <ol>
 *   <li>chiffre le mot de passe fourni et persiste le {@link Tenant} en
 *       statut {@code PROVISIONING}, dans une transaction JPA COMMITTÉE ;</li>
 *   <li>hors de toute transaction JPA, construit un pool Hikari avec les
 *       identifiants fournis et exécute la migration Flyway du schéma
 *       tenant ;</li>
 *   <li>en cas de succès, bascule le tenant en {@code ACTIVE} ; en cas
 *       d'échec (connexion ou migration), bascule en {@code FAILED} et
 *       propage une exception métier (mappée en HTTP 422).</li>
 * </ol>
 * Les étapes 1 et 3 utilisent une {@link TransactionTemplate} programmatique
 * (plutôt que {@code @Transactional} sur des méthodes privées) afin d'éviter
 * les pièges classiques d'auto-invocation des proxies Spring et de garantir
 * explicitement que la persistance du statut PROVISIONING est bien commise
 * avant que l'étape de connexion/migration (potentiellement longue et
 * faillible) ne démarre.
 */
@Slf4j
@Service
public class TenantProvisioningService {

    /**
     * Format attendu pour les identifiants techniques {@code keycloakRealm}
     * et {@code dbName} : ils sont utilisés tels quels comme nom de pool
     * Hikari et comme segment d'URL JDBC, d'où une validation stricte.
     */
    private static final Pattern TECHNICAL_IDENTIFIER_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]{2,63}$");
    private static final int MAX_DB_PORT = 65535;

    private final TenantRepository tenantRepository;
    private final CredentialEncryptor credentialEncryptor;
    private final TenantDataSourceRegistry dataSourceRegistry;
    private final TenantSchemaMigrationService migrationService;
    private final TransactionTemplate transactionTemplate;
    private final String adminRealm;

    public TenantProvisioningService(
            TenantRepository tenantRepository,
            CredentialEncryptor credentialEncryptor,
            TenantDataSourceRegistry dataSourceRegistry,
            TenantSchemaMigrationService migrationService,
            PlatformTransactionManager transactionManager,
            @Value("${compta.keycloak.admin-realm}") String adminRealm) {
        this.tenantRepository = tenantRepository;
        this.credentialEncryptor = credentialEncryptor;
        this.dataSourceRegistry = dataSourceRegistry;
        this.migrationService = migrationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.adminRealm = adminRealm;
    }

    public TenantResponse create(TenantCreateRequest request) {
        Tenant tenant = persistPending(request);
        activateOrFail(tenant);
        return TenantResponse.from(reload(tenant.getId()));
    }

    public TenantResponse retryMigration(Long tenantId) {
        Tenant tenant = reload(tenantId);
        if (tenant.getStatus() != TenantStatus.FAILED) {
            throw new IllegalStateException(
                    "Seul un tenant en statut FAILED peut être re-migré (statut actuel : " + tenant.getStatus() + ")");
        }
        activateOrFail(tenant);
        return TenantResponse.from(reload(tenantId));
    }

    public TenantResponse update(Long tenantId, TenantUpdateRequest request) {
        validate(request);
        boolean rotatePassword = request.dbPassword() != null && !request.dbPassword().isBlank();
        Tenant updated = transactionTemplate.execute(status -> {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new TenantNotFoundException(tenantId));
            if (request.name() != null) {
                tenant.setName(request.name());
            }
            if (rotatePassword) {
                tenant.setDbPasswordEncrypted(credentialEncryptor.encrypt(request.dbPassword()));
            }
            return tenantRepository.save(tenant);
        });
        if (rotatePassword) {
            dataSourceRegistry.rotate(updated);
        }
        return TenantResponse.from(updated);
    }

    private Tenant persistPending(TenantCreateRequest request) {
        validate(request);
        return transactionTemplate.execute(status -> {
            if (tenantRepository.existsByKeycloakRealmOrDbName(request.keycloakRealm(), request.dbName())) {
                throw new TenantAlreadyExistsException(request.keycloakRealm(), request.dbName());
            }
            Tenant tenant = Tenant.builder()
                    .name(request.name())
                    .clientType(request.clientType())
                    .keycloakRealm(request.keycloakRealm())
                    .dbName(request.dbName())
                    .dbHost(request.dbHost())
                    .dbPort(request.dbPort())
                    .dbUsername(request.dbUsername())
                    .dbPasswordEncrypted(credentialEncryptor.encrypt(request.dbPassword()))
                    .status(TenantStatus.PROVISIONING)
                    .build();
            return tenantRepository.save(tenant);
        });
    }

    /**
     * Étape 2/3 : construit un pool transitoire, migre, puis bascule le
     * statut du tenant (ACTIVE ou FAILED), entièrement hors transaction JPA
     * pendant la phase de connexion/migration.
     */
    private void activateOrFail(Tenant tenant) {
        HikariDataSource transientPool = null;
        try {
            transientPool = dataSourceRegistry.createTransientPool(tenant);
            migrationService.migrate(tenant, transientPool);
            updateStatus(tenant.getId(), TenantStatus.ACTIVE);
            log.info("Tenant '{}' (realm={}) provisionné avec succès", tenant.getName(), tenant.getKeycloakRealm());
        } catch (Exception e) {
            log.error("Échec du provisioning du tenant realm='{}' : {}", tenant.getKeycloakRealm(), e.getMessage());
            try {
                updateStatus(tenant.getId(), TenantStatus.FAILED);
            } catch (Exception statusUpdateException) {
                // La cause originale (échec de connexion/migration) ne doit
                // jamais être perdue même si la transition vers FAILED échoue
                // elle-même (ex. base admin momentanément indisponible) :
                // sans ce log, le tenant resterait bloqué en PROVISIONING
                // sans aucune trace exploitable de la cause racine.
                log.error(
                        "Échec de la transition vers FAILED pour le tenant realm='{}' (id={}) après l'échec de "
                                + "provisioning ci-dessus ; le tenant peut rester bloqué en statut {} en base",
                        tenant.getKeycloakRealm(), tenant.getId(), tenant.getStatus(), statusUpdateException);
            }
            throw new TenantProvisioningException(
                    "Échec de connexion ou de migration pour le tenant '" + tenant.getName() + "'. "
                            + "Vérifiez que la base et le rôle PostgreSQL ont bien été créés au préalable "
                            + "(voir scripts/bootstrap_postgres.sql).",
                    e);
        } finally {
            if (transientPool != null) {
                transientPool.close();
            }
        }
    }

    private void updateStatus(Long tenantId, TenantStatus status) {
        transactionTemplate.executeWithoutResult(txStatus -> {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new TenantNotFoundException(tenantId));
            tenant.setStatus(status);
            tenantRepository.save(tenant);
        });
    }

    private Tenant reload(Long tenantId) {
        return tenantRepository.findById(tenantId).orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    private void validate(TenantCreateRequest request) {
        requireNonBlank(request.name(), "name");
        requireNonBlank(request.keycloakRealm(), "keycloakRealm");
        requireNonBlank(request.dbName(), "dbName");
        requireNonBlank(request.dbHost(), "dbHost");
        requireNonBlank(request.dbUsername(), "dbUsername");
        requireNonBlank(request.dbPassword(), "dbPassword");
        if (request.clientType() == null) {
            throw new IllegalArgumentException("clientType est requis");
        }
        if (request.dbPort() == null || request.dbPort() <= 0 || request.dbPort() > MAX_DB_PORT) {
            throw new IllegalArgumentException("dbPort doit être un entier compris entre 1 et " + MAX_DB_PORT);
        }
        requireValidTechnicalIdentifier(request.keycloakRealm(), "keycloakRealm");
        requireValidTechnicalIdentifier(request.dbName(), "dbName");
        requireNotAdminRealm(request.keycloakRealm());
    }

    /**
     * Applique à la mise à jour la même rigueur de validation qu'à la
     * création : un champ fourni (non {@code null}) ne peut pas être
     * composé uniquement d'espaces.
     */
    private void validate(TenantUpdateRequest request) {
        if (request.name() != null && request.name().isBlank()) {
            throw new IllegalArgumentException("name ne peut pas être vide");
        }
        if (request.dbPassword() != null && request.dbPassword().isBlank()) {
            throw new IllegalArgumentException("dbPassword ne peut pas être vide s'il est fourni (rotation)");
        }
    }

    private void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " est requis");
        }
    }

    /**
     * {@code keycloakRealm} et {@code dbName} sont utilisés tels quels comme
     * identifiants techniques (nom de pool Hikari, segment d'URL JDBC) : ils
     * doivent respecter un format strict pour éviter toute injection ou
     * confusion (ex. segments d'URL contenant des caractères spéciaux).
     */
    private void requireValidTechnicalIdentifier(String value, String field) {
        if (!TECHNICAL_IDENTIFIER_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    field + " doit respecter le format " + TECHNICAL_IDENTIFIER_PATTERN.pattern());
        }
    }

    /**
     * Un tenant ne peut pas être créé avec {@code keycloakRealm} égal au
     * realm admin configuré ({@code compta.keycloak.admin-realm}) : ce realm
     * est traité comme le realm admin par
     * {@link com.walsia.compta.security.TenantAwareAuthenticationManagerResolver}
     * et {@link com.walsia.compta.security.AdminAccessAuthorizationManager} ;
     * un tenant portant ce nom de realm serait donc silencieusement
     * inéligible à toute authentification en tant que tenant.
     */
    private void requireNotAdminRealm(String keycloakRealm) {
        if (adminRealm.equals(keycloakRealm)) {
            throw new IllegalArgumentException(
                    "keycloakRealm ne peut pas être égal au realm admin configuré ('" + adminRealm + "')");
        }
    }
}
