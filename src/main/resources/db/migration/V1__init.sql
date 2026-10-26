CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(160) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(120) NOT NULL,
    department      VARCHAR(80)  NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE rooms (
    id              UUID PRIMARY KEY,
    code            VARCHAR(16)  NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL,
    floor           SMALLINT     NOT NULL,
    location        VARCHAR(160) NOT NULL,
    capacity        INTEGER      NOT NULL CHECK (capacity > 0 AND capacity <= 200),
    description     VARCHAR(500),
    status          VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE room_amenities (
    room_id     UUID        NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    amenity     VARCHAR(40) NOT NULL,
    PRIMARY KEY (room_id, amenity)
);

CREATE TABLE bookings (
    id              UUID PRIMARY KEY,
    room_id         UUID         NOT NULL REFERENCES rooms (id),
    organizer_id    UUID         NOT NULL REFERENCES users (id),
    title           VARCHAR(160) NOT NULL,
    purpose         VARCHAR(400),
    start_at        TIMESTAMPTZ  NOT NULL,
    end_at          TIMESTAMPTZ  NOT NULL,
    attendee_count  INTEGER      NOT NULL CHECK (attendee_count > 0),
    status          VARCHAR(20)  NOT NULL,
    cancel_reason   VARCHAR(400),
    cancelled_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT bookings_time_order CHECK (end_at > start_at)
);

CREATE TABLE booking_attendees (
    booking_id  UUID NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users (id),
    PRIMARY KEY (booking_id, user_id)
);

-- Prevents two CONFIRMED bookings from occupying the same room at overlapping times.
-- This is the last line of defence against double-booking under concurrency.
ALTER TABLE bookings
    ADD CONSTRAINT bookings_no_overlap
    EXCLUDE USING gist (
        room_id WITH =,
        tstzrange(start_at, end_at, '[)') WITH &&
    )
    WHERE (status = 'CONFIRMED');

CREATE INDEX idx_bookings_room_time ON bookings (room_id, start_at, end_at);
CREATE INDEX idx_bookings_organizer ON bookings (organizer_id, start_at);
CREATE INDEX idx_bookings_status_time ON bookings (status, start_at);
CREATE INDEX idx_rooms_status_capacity ON rooms (status, capacity);
