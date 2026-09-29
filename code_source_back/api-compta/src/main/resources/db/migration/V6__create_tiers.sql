CREATE TABLE tiers
(
    id                 VARCHAR(36) PRIMARY KEY,
    type               VARCHAR(20)  NOT NULL,
    raison_sociale     VARCHAR(255) NOT NULL,
    nom_contact        VARCHAR(255),
    email              VARCHAR(255),
    telephone          VARCHAR(255),
    adresse            VARCHAR(255),
    numero_fiscal      VARCHAR(255),
    actif              BOOLEAN      NOT NULL DEFAULT TRUE,
    entite_id          VARCHAR(36)  NOT NULL,
    compte_associe_id  VARCHAR(36),
    CONSTRAINT fk_tiers_entite FOREIGN KEY (entite_id)
        REFERENCES entite (id),
    CONSTRAINT fk_tiers_compte_associe FOREIGN KEY (compte_associe_id)
        REFERENCES compte_comptable (id)
);
