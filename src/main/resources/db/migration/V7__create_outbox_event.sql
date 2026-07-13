CREATE TABLE outbox_event
(
    id                 UUID PRIMARY KEY,
    version            BIGINT      NOT NULL DEFAULT 0,
    topic_key          TEXT        NOT NULL,
    topic_name         TEXT        NOT NULL,
    aggregate_type     TEXT        NOT NULL,
    aggregate_id       TEXT        NOT NULL,
    event_type         TEXT        NOT NULL,
    payload            JSONB       NOT NULL,
    headers            JSONB       NOT NULL DEFAULT '{}'::jsonb,
    status             TEXT        NOT NULL DEFAULT 'PENDING',
    retry_count        INTEGER     NOT NULL DEFAULT 0,
    next_attempt_at    TIMESTAMPTZ NOT NULL,
    occurred_at        TIMESTAMPTZ NOT NULL,
    published_at       TIMESTAMPTZ,
    dead_lettered_at   TIMESTAMPTZ,
    last_error         TEXT,
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,

    CONSTRAINT outbox_event_topic_key_not_blank
        CHECK (LENGTH(TRIM(topic_key)) > 0),

    CONSTRAINT outbox_event_topic_name_not_blank
        CHECK (LENGTH(TRIM(topic_name)) > 0),

    CONSTRAINT outbox_event_aggregate_type_not_blank
        CHECK (LENGTH(TRIM(aggregate_type)) > 0),

    CONSTRAINT outbox_event_aggregate_id_not_blank
        CHECK (LENGTH(TRIM(aggregate_id)) > 0),

    CONSTRAINT outbox_event_event_type_not_blank
        CHECK (LENGTH(TRIM(event_type)) > 0),

    CONSTRAINT outbox_event_retry_count_not_negative
        CHECK (retry_count >= 0),

    CONSTRAINT outbox_event_status_supported
        CHECK (status IN ('PENDING', 'FAILED', 'DLT_PENDING', 'PUBLISHED', 'DEAD_LETTERED'))
);

CREATE INDEX outbox_event_publishable_idx
    ON outbox_event (next_attempt_at, occurred_at, id)
    WHERE status IN ('PENDING', 'FAILED', 'DLT_PENDING');

CREATE INDEX outbox_event_status_idx
    ON outbox_event (status);

CREATE INDEX outbox_event_aggregate_idx
    ON outbox_event (aggregate_type, aggregate_id);

CREATE INDEX outbox_event_topic_idx
    ON outbox_event (topic_key, topic_name);
