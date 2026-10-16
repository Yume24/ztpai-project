CREATE TABLE push_tokens (
    id         uuid PRIMARY KEY,
    user_id    uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      varchar(255) NOT NULL UNIQUE,
    updated_at timestamptz  NOT NULL
);

CREATE INDEX idx_push_tokens_user_id ON push_tokens (user_id);
