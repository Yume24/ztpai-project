CREATE TABLE location_points (
    id          bigserial PRIMARY KEY,
    member_id   uuid                 NOT NULL REFERENCES room_members (id) ON DELETE CASCADE,
    position    geometry(Point,4326) NOT NULL,
    accuracy_m  real                 NOT NULL,
    recorded_at timestamptz          NOT NULL,
    received_at timestamptz          NOT NULL
);

CREATE INDEX idx_location_points_member_recorded ON location_points (member_id, recorded_at);
