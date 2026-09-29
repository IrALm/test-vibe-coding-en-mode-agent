CREATE TABLE compte_comptable
(
    id                          VARCHAR(36) PRIMARY KEY,
    numero                      VARCHAR(10)  NOT NULL,
    libelle                     VARCHAR(255) NOT NULL,
    classe_compte_comptable_id  VARCHAR(36)  NOT NULL,
    sens_normal                 VARCHAR(10)  NOT NULL,
    lettrable                   BOOLEAN      NOT NULL DEFAULT FALSE,
    actif                       BOOLEAN      NOT NULL DEFAULT TRUE,
    plan_comptable_id           VARCHAR(36)  NOT NULL,
    compte_parent_id            VARCHAR(36),
    entite_id                   VARCHAR(36),
    CONSTRAINT fk_compte_classe_compte_comptable FOREIGN KEY (classe_compte_comptable_id)
        REFERENCES classe_compte_comptable (id),
    CONSTRAINT fk_compte_plan_comptable FOREIGN KEY (plan_comptable_id)
        REFERENCES plan_comptable (id),
    CONSTRAINT fk_compte_compte_parent FOREIGN KEY (compte_parent_id)
        REFERENCES compte_comptable (id),
    CONSTRAINT fk_compte_entite FOREIGN KEY (entite_id)
        REFERENCES entite (id),
    CONSTRAINT uk_compte_plan_numero UNIQUE (plan_comptable_id, numero)
);
