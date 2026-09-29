CREATE TABLE ecriture
(
    id                     VARCHAR(36)  PRIMARY KEY,
    date_ecriture          DATE         NOT NULL,
    reference              VARCHAR(255),
    libelle                VARCHAR(255) NOT NULL,
    statut                 VARCHAR(15)  NOT NULL,
    numero                 VARCHAR(20),
    journal_id             VARCHAR(36)  NOT NULL,
    exercice_id            VARCHAR(36)  NOT NULL,
    entite_id              VARCHAR(36)  NOT NULL,
    cree_par_id            VARCHAR(36)  NOT NULL,
    cree_le                TIMESTAMP    NOT NULL,
    valide_par_id          VARCHAR(36),
    valide_le              TIMESTAMP,
    motif_rejet            VARCHAR(500),
    ecriture_miroir_id     VARCHAR(36),
    contre_passation_de_id VARCHAR(36),
    CONSTRAINT fk_ecriture_journal FOREIGN KEY (journal_id)
        REFERENCES journal (id),
    CONSTRAINT fk_ecriture_exercice FOREIGN KEY (exercice_id)
        REFERENCES exercice_comptable (id),
    CONSTRAINT fk_ecriture_entite FOREIGN KEY (entite_id)
        REFERENCES entite (id),
    CONSTRAINT fk_ecriture_cree_par FOREIGN KEY (cree_par_id)
        REFERENCES utilisateur (id),
    CONSTRAINT fk_ecriture_valide_par FOREIGN KEY (valide_par_id)
        REFERENCES utilisateur (id),
    CONSTRAINT fk_ecriture_miroir FOREIGN KEY (ecriture_miroir_id)
        REFERENCES ecriture (id),
    CONSTRAINT fk_ecriture_contre_passation_de FOREIGN KEY (contre_passation_de_id)
        REFERENCES ecriture (id),
    CONSTRAINT uk_ecriture_journal_numero UNIQUE (journal_id, numero)
);

CREATE INDEX idx_ecriture_exercice ON ecriture (exercice_id);
CREATE INDEX idx_ecriture_statut ON ecriture (statut);
