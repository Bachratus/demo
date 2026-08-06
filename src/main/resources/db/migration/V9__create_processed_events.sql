CREATE TABLE processed_events
(
    id         BIGINT PRIMARY KEY,
    event_type TEXT        NOT NULL,
    event_id   TEXT        NOT NULL,
    version    INTEGER     NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by TEXT,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by TEXT,

    CONSTRAINT processed_events_event_type_not_blank
        CHECK (LENGTH(TRIM(event_type)) > 0),

    CONSTRAINT processed_events_event_id_not_blank
        CHECK (LENGTH(TRIM(event_id)) > 0),

    CONSTRAINT processed_events_event_unique
        UNIQUE (event_type, event_id)
);

CREATE SEQUENCE processed_events_id_seq
    START WITH 1
    INCREMENT BY 50;

ALTER SEQUENCE processed_events_id_seq
    OWNED BY processed_events.id;

ALTER TABLE processed_events
    ALTER COLUMN id SET DEFAULT nextval('processed_events_id_seq');

SELECT setval(
               'processed_events_id_seq',
               COALESCE((SELECT MAX(id) FROM processed_events), 1),
               EXISTS (SELECT 1 FROM processed_events)
       );
