ALTER TABLE utilisateur
    ADD COLUMN keycloak_id   VARCHAR(36)  NULL,
    ADD COLUMN email_verifie BOOLEAN      NOT NULL DEFAULT FALSE;

ALTER TABLE utilisateur
    ADD CONSTRAINT uk_utilisateur_keycloak_id UNIQUE (keycloak_id);
