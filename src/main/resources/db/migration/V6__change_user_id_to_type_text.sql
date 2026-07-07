DROP INDEX customer_user_id_unique;

ALTER TABLE customer
    ALTER COLUMN user_id TYPE TEXT
        USING user_id::text;

ALTER TABLE customer
    ADD CONSTRAINT customer_user_id_not_blank
        CHECK (LENGTH(TRIM(user_id)) > 0);

CREATE UNIQUE INDEX customer_user_id_unique
    ON customer (user_id);