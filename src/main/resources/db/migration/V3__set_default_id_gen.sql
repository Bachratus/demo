ALTER TABLE customer
    ALTER COLUMN id SET DEFAULT nextval('customer_id_seq');

ALTER TABLE customer_address
    ALTER COLUMN id SET DEFAULT nextval('customer_address_id_seq');


SELECT setval(
               'customer_id_seq',
               COALESCE((SELECT MAX(id) FROM customer), 1),
               EXISTS (SELECT 1 FROM customer)
       );

SELECT setval(
               'customer_address_id_seq',
               COALESCE((SELECT MAX(id) FROM customer_address), 1),
               EXISTS (SELECT 1 FROM customer_address)
       );