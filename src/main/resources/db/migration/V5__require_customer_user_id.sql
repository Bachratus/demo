ALTER TABLE customer
    ALTER COLUMN user_id SET NOT NULL;

DROP INDEX customer_user_id_unique;

CREATE UNIQUE INDEX customer_user_id_unique
    ON customer (user_id);