CREATE TABLE geofences (
    id         uuid PRIMARY KEY,
    room_id    uuid                 NOT NULL UNIQUE REFERENCES rooms (id) ON DELETE CASCADE,
    center     geometry(Point,4326) NOT NULL,
    radius_m   integer              NOT NULL CHECK (radius_m BETWEEN 50 AND 5000),
    updated_at timestamptz          NOT NULL
);
