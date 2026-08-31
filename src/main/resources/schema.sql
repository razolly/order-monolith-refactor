DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS products;

CREATE TABLE products (
    id     BIGINT       PRIMARY KEY,
    name   VARCHAR(255) NOT NULL,
    price  DECIMAL(12, 2) NOT NULL,
    stock  INT          NOT NULL
);

CREATE TABLE orders (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    customer_email    VARCHAR(255) NOT NULL,
    subtotal          DECIMAL(12, 2) NOT NULL,
    tax               DECIMAL(12, 2) NOT NULL,
    shipping          DECIMAL(12, 2) NOT NULL,
    discount          DECIMAL(12, 2) NOT NULL,
    total             DECIMAL(12, 2) NOT NULL,
    payment_method    VARCHAR(32)  NOT NULL,
    payment_reference VARCHAR(128) NOT NULL,
    status            VARCHAR(32)  NOT NULL,
    created_at        TIMESTAMP    NOT NULL
);

CREATE TABLE order_items (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT       NOT NULL,
    product_id  BIGINT       NOT NULL,
    unit_price  DECIMAL(12, 2) NOT NULL,
    quantity    INT          NOT NULL,
    line_total  DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE TABLE audit_log (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    message     VARCHAR(1024) NOT NULL,
    created_at  TIMESTAMP    NOT NULL
);
