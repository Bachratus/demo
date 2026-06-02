CREATE TABLE customer
(
    id           BIGINT PRIMARY KEY,
    public_id    UUID        NOT NULL UNIQUE,
    user_id      UUID,
    email        TEXT        NOT NULL,
    display_name TEXT,
    version      INTEGER     NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,

    CONSTRAINT customer_email_not_blank
        CHECK (LENGTH(TRIM(email)) > 0)
);

CREATE SEQUENCE customer_id_seq
    START WITH 1
    INCREMENT BY 50;

ALTER SEQUENCE customer_id_seq
    OWNED BY customer.id;

CREATE UNIQUE INDEX customer_user_id_unique
    ON customer (user_id)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX customer_email_unique
    ON customer (LOWER(email));

CREATE TABLE customer_address
(
    id               BIGINT PRIMARY KEY,
    public_id        UUID        NOT NULL UNIQUE,
    customer_id      BIGINT      NOT NULL,
    label            TEXT,
    recipient_name   TEXT        NOT NULL,
    street           TEXT        NOT NULL,
    building_number  TEXT        NOT NULL,
    apartment_number TEXT,
    postal_code      TEXT        NOT NULL,
    city             TEXT        NOT NULL,
    country_code     TEXT        NOT NULL,
    phone_number     TEXT,
    is_default       BOOLEAN     NOT NULL DEFAULT FALSE,
    version          INTEGER     NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,

    CONSTRAINT customer_address_customer_fk
        FOREIGN KEY (customer_id)
            REFERENCES customer (id)
            ON DELETE CASCADE,

    CONSTRAINT customer_address_street_not_blank
        CHECK (LENGTH(TRIM(street)) > 0),

    CONSTRAINT customer_address_building_number_not_blank
        CHECK (LENGTH(TRIM(building_number)) > 0),

    CONSTRAINT customer_address_postal_code_not_blank
        CHECK (LENGTH(TRIM(postal_code)) > 0),

    CONSTRAINT customer_address_recipient_not_blank
        CHECK (LENGTH(TRIM(recipient_name)) > 0),

    CONSTRAINT customer_address_city_not_blank
        CHECK (LENGTH(TRIM(city)) > 0),

    CONSTRAINT customer_address_country_code_format
        CHECK (country_code ~ '^[A-Z]{2}$')
);

CREATE SEQUENCE customer_address_id_seq
    START WITH 1
    INCREMENT BY 50;

ALTER SEQUENCE customer_address_id_seq
    OWNED BY customer_address.id;

CREATE INDEX customer_address_customer_id_idx
    ON customer_address (customer_id);

CREATE UNIQUE INDEX customer_address_one_default_per_customer
    ON customer_address (customer_id)
    WHERE is_default = TRUE;