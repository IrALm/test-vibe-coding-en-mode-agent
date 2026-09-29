CREATE TABLE user_session
(
    id                       VARCHAR(36)  PRIMARY KEY,
    utilisateur_id           VARCHAR(36)  NOT NULL,
    session_token_hash       VARCHAR(255) NOT NULL,
    access_token_chiffre     TEXT         NOT NULL,
    access_token_expires_at  TIMESTAMP    NOT NULL,
    refresh_token_chiffre    TEXT         NOT NULL,
    refresh_token_expires_at TIMESTAMP    NOT NULL,
    revoked_at               TIMESTAMP,
    created_at               TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_session_utilisateur FOREIGN KEY (utilisateur_id)
        REFERENCES utilisateur (id),
    CONSTRAINT uk_user_session_token_hash UNIQUE (session_token_hash)
);

CREATE INDEX idx_user_session_utilisateur_revoked ON user_session (utilisateur_id, revoked_at);
