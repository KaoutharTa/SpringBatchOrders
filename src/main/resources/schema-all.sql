DROP TABLE orders IF EXISTS;

CREATE TABLE orders (
                        order_id BIGINT PRIMARY KEY,
                        customer_name VARCHAR(255),
                        amount DOUBLE
);