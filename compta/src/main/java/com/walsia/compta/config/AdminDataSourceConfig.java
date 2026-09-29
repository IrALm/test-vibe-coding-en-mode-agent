package com.walsia.compta.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * DataSource principale de l'application : l'annuaire des tenants
 * (postgres_admin_db), liée aux propriétés {@code spring.datasource.*}.
 * <p>
 * Marquée {@code @Primary} car un second bean {@code DataSource} existe dans
 * le contexte ({@link com.walsia.compta.tenant.TenantAwareDataSource}) :
 * sans cette annotation, toute injection par type sans qualifier
 * (JPA, actuator...) deviendrait ambiguë.
 * <p>
 * L'{@code EntityManagerFactory} de l'annuaire est celui auto-configuré par
 * Spring Boot à partir de cette DataSource primaire (aucune configuration
 * JPA manuelle supplémentaire n'est nécessaire dans ce lot : les entités et
 * repositories de l'annuaire résident déjà dans l'arborescence de paquets
 * scannée par défaut par {@code @SpringBootApplication}).
 */
@Configuration
public class AdminDataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties adminDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    public DataSource adminDataSource(DataSourceProperties adminDataSourceProperties) {
        return adminDataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }
}
