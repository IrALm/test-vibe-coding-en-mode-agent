CREATE TABLE referentiel_comptable
(
    id          VARCHAR(36) PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL,
    libelle     VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    version     VARCHAR(255),
    CONSTRAINT uk_referentiel_comptable_code UNIQUE (code)
);
