CREATE TABLE plan_comptable
(
    id                        VARCHAR(36) PRIMARY KEY,
    referentiel_comptable_id  VARCHAR(36) NOT NULL,
    date_mise_en_place        DATE,
    actif                     BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_plan_comptable_referentiel_comptable FOREIGN KEY (referentiel_comptable_id)
        REFERENCES referentiel_comptable (id)
);
