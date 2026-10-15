CREATE TABLE users (
    id            uuid PRIMARY KEY,
    email         varchar(254) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    display_name  varchar(40)  NOT NULL,
    role          varchar(20)  NOT NULL,
    blocked_at    timestamptz,
    created_at    timestamptz  NOT NULL
);

CREATE TABLE refresh_tokens (
    id         uuid PRIMARY KEY,
    user_id    uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash varchar(64)  NOT NULL UNIQUE,
    expires_at timestamptz  NOT NULL,
    created_at timestamptz  NOT NULL
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
