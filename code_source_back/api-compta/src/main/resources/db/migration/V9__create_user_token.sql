CREATE TABLE user_token
(
    id             VARCHAR(36)  PRIMARY KEY,
    utilisateur_id VARCHAR(36)  NOT NULL,
    token_hash     VARCHAR(255) NOT NULL,
    type           VARCHAR(30)  NOT NULL,
    expires_at     TIMESTAMP    NOT NULL,
    used_at        TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_token_utilisateur FOREIGN KEY (utilisateur_id)
        REFERENCES utilisateur (id),
    CONSTRAINT uk_user_token_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_user_token_utilisateur_type ON user_token (utilisateur_id, type);
