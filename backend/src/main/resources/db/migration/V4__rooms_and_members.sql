CREATE TABLE rooms (
    id                uuid PRIMARY KEY,
    name              varchar(60)  NOT NULL,
    code              varchar(6)   NOT NULL UNIQUE,
    owner_id          uuid         NOT NULL REFERENCES users (id),
    starts_at         timestamptz  NOT NULL,
    ends_at           timestamptz  NOT NULL,
    closed_at         timestamptz,
    visibility_mode   varchar(20)  NOT NULL DEFAULT 'ALL',
    members_see_trails boolean     NOT NULL DEFAULT false,
    start_notified_at timestamptz,
    end_processed_at  timestamptz,
    version           bigint       NOT NULL DEFAULT 0,
    created_at        timestamptz  NOT NULL
);

CREATE INDEX idx_rooms_starts_at ON rooms (starts_at);
CREATE INDEX idx_rooms_ends_at ON rooms (ends_at);

CREATE TABLE room_members (
    id             uuid PRIMARY KEY,
    room_id        uuid         NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    user_id        uuid         NOT NULL REFERENCES users (id),
    role           varchar(20)  NOT NULL,
    status         varchar(20)  NOT NULL,
    geofence_state varchar(20)  NOT NULL DEFAULT 'UNKNOWN',
    joined_at      timestamptz  NOT NULL,
    left_at        timestamptz,
    UNIQUE (room_id, user_id)
);

CREATE INDEX idx_room_members_user_id ON room_members (user_id);
