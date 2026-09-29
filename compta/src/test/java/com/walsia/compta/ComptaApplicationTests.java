package com.walsia.compta;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test de chargement du contexte Spring complet. La base admin
 * ({@code postgres_admin_db} en production) est fournie ici par un
 * conteneur PostgreSQL Testcontainers éphémère (démarré/détruit
 * automatiquement pour ce run, sans donnée persistée), plutôt que par le
 * service PostgreSQL de développement du {@code docker-compose.yml} — voir
 * HELP.md, section "Tests d'intégration". Docker doit être disponible sur la
 * machine exécutant les tests.
 */
@Testcontainers
@SpringBootTest
class ComptaApplicationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer ADMIN_DB = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

	@Test
	void contextLoads() {
	}

}
