CREATE TABLE exercice_comptable
(
    id         VARCHAR(36) PRIMARY KEY,
    date_debut DATE        NOT NULL,
    date_fin   DATE        NOT NULL,
    statut     VARCHAR(10) NOT NULL,
    entite_id  VARCHAR(36) NOT NULL,
    CONSTRAINT fk_exercice_entite FOREIGN KEY (entite_id)
        REFERENCES entite (id)
);

-- Un seul exercice OUVERT par entité, imposé en base (pas seulement côté service).
CREATE UNIQUE INDEX uk_exercice_entite_ouvert ON exercice_comptable (entite_id) WHERE statut = 'OUVERT';
