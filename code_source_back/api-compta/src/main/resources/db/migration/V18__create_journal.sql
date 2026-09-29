CREATE TABLE journal
(
    id             VARCHAR(36) PRIMARY KEY,
    libelle        VARCHAR(255) NOT NULL,
    entite_id      VARCHAR(36)  NOT NULL,
    dernier_numero INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT fk_journal_entite FOREIGN KEY (entite_id)
        REFERENCES entite (id),
    CONSTRAINT uk_journal_entite UNIQUE (entite_id)
);
