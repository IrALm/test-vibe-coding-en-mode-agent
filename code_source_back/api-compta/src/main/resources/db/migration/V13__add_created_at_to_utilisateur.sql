ALTER TABLE utilisateur
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();
