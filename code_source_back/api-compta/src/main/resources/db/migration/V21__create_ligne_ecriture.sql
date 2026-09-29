CREATE TABLE ligne_ecriture
(
    id          VARCHAR(36)   PRIMARY KEY,
    ecriture_id VARCHAR(36)   NOT NULL,
    compte_id   VARCHAR(36)   NOT NULL,
    sens        VARCHAR(10)   NOT NULL,
    montant     NUMERIC(19,2) NOT NULL,
    libelle     VARCHAR(255),
    CONSTRAINT fk_ligne_ecriture FOREIGN KEY (ecriture_id)
        REFERENCES ecriture (id),
    CONSTRAINT fk_ligne_compte FOREIGN KEY (compte_id)
        REFERENCES compte_comptable (id),
    CONSTRAINT ck_ligne_montant_positif CHECK (montant > 0)
);

CREATE INDEX idx_ligne_ecriture_ecriture ON ligne_ecriture (ecriture_id);
CREATE INDEX idx_ligne_ecriture_compte ON ligne_ecriture (compte_id);
