CREATE TABLE classe_compte_comptable
(
    id                        VARCHAR(36) PRIMARY KEY,
    numero                    INTEGER      NOT NULL,
    titre                     VARCHAR(255) NOT NULL,
    description               VARCHAR(255),
    referentiel_comptable_id  VARCHAR(36)  NOT NULL,
    CONSTRAINT fk_classe_compte_referentiel_comptable FOREIGN KEY (referentiel_comptable_id)
        REFERENCES referentiel_comptable (id),
    CONSTRAINT uk_classe_compte_referentiel_numero UNIQUE (referentiel_comptable_id, numero)
);
