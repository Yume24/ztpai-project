CREATE TABLE alerts (
    id              uuid PRIMARY KEY,
    room_id         uuid         NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    subject_user_id uuid         NOT NULL REFERENCES users (id),
    type            varchar(20)  NOT NULL,
    created_at      timestamptz  NOT NULL
);

CREATE INDEX idx_alerts_room_id_created_at ON alerts (room_id, created_at);
