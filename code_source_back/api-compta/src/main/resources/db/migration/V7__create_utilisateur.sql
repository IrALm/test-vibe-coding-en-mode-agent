CREATE TABLE utilisateur
(
    id         VARCHAR(36) PRIMARY KEY,
    nom        VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    actif      BOOLEAN      NOT NULL DEFAULT TRUE,
    entite_id  VARCHAR(36)  NOT NULL,
    CONSTRAINT uk_utilisateur_email UNIQUE (email),
    CONSTRAINT fk_utilisateur_entite FOREIGN KEY (entite_id)
        REFERENCES entite (id)
);
