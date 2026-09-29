CREATE TABLE entite
(
    id                     VARCHAR(36) PRIMARY KEY,
    raison_sociale         VARCHAR(255) NOT NULL,
    type_entite            VARCHAR(20)  NOT NULL,
    pays                   VARCHAR(255),
    devise                 VARCHAR(3),
    numero_identification  VARCHAR(255),
    date_creation          DATE,
    actif                  BOOLEAN      NOT NULL DEFAULT TRUE,
    referentiel_comptable_id VARCHAR(36) NOT NULL,
    CONSTRAINT fk_entite_referentiel_comptable FOREIGN KEY (referentiel_comptable_id)
        REFERENCES referentiel_comptable (id)
);
