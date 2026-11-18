PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS venues (
    id   TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    city TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS events (
    id            TEXT PRIMARY KEY,
    title         TEXT NOT NULL,
    description   TEXT NOT NULL DEFAULT '',
    category      TEXT NOT NULL,
    venue_id      TEXT NOT NULL REFERENCES venues(id),
    starts_at     TEXT NOT NULL,
    sales_open_at TEXT NOT NULL,
    status        TEXT NOT NULL CHECK (status IN ('DRAFT','PUBLISHED','CANCELLED'))
);

CREATE TABLE IF NOT EXISTS sections (
    event_id      TEXT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    name          TEXT NOT NULL,
    tier          TEXT NOT NULL CHECK (tier IN ('STANDARD','PREMIUM','VIP')),
    rows_count    INTEGER NOT NULL CHECK (rows_count > 0),
    seats_per_row INTEGER NOT NULL CHECK (seats_per_row > 0),
    base_price    TEXT NOT NULL,
    PRIMARY KEY (event_id, name)
);

CREATE TABLE IF NOT EXISTS seats (
    id        TEXT PRIMARY KEY,
    event_id  TEXT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    section   TEXT NOT NULL,
    row_label TEXT NOT NULL,
    number    INTEGER NOT NULL,
    tier      TEXT NOT NULL,
    status    TEXT NOT NULL CHECK (status IN ('AVAILABLE','HELD','BOOKED'))
);
CREATE INDEX IF NOT EXISTS idx_seats_event ON seats(event_id);

CREATE TABLE IF NOT EXISTS bookings (
    id             TEXT PRIMARY KEY,
    event_id       TEXT NOT NULL REFERENCES events(id),
    customer_name  TEXT NOT NULL,
    customer_email TEXT NOT NULL,
    customer_phone TEXT,
    status         TEXT NOT NULL CHECK (status IN ('PENDING','HELD','CONFIRMED','CANCELLED','EXPIRED')),
    total_amount   TEXT NOT NULL,
    payment_id     TEXT,
    created_at     TEXT NOT NULL,
    expires_at     TEXT,
    updated_at     TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_bookings_event ON bookings(event_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);

CREATE TABLE IF NOT EXISTS booking_seats (
    booking_id TEXT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    seat_id    TEXT NOT NULL REFERENCES seats(id),
    PRIMARY KEY (booking_id, seat_id)
);

CREATE TABLE IF NOT EXISTS payments (
    id             TEXT PRIMARY KEY,
    booking_id     TEXT NOT NULL REFERENCES bookings(id),
    method         TEXT NOT NULL CHECK (method IN ('CARD','WALLET','BANK_TRANSFER')),
    amount         TEXT NOT NULL,
    status         TEXT NOT NULL CHECK (status IN ('SUCCESS','FAILED')),
    gateway_ref    TEXT,
    failure_reason TEXT,
    processed_at   TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_payments_booking ON payments(booking_id);

CREATE TABLE IF NOT EXISTS notifications (
    id         TEXT PRIMARY KEY,
    booking_id TEXT NOT NULL,
    channel    TEXT NOT NULL CHECK (channel IN ('EMAIL','SMS','IN_APP')),
    recipient  TEXT NOT NULL,
    subject    TEXT NOT NULL,
    body       TEXT NOT NULL,
    sent_at    TEXT NOT NULL,
    delivered  INTEGER NOT NULL CHECK (delivered IN (0,1))
);
