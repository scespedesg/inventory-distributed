CREATE TABLE stock (
    sku_id VARCHAR(40) PRIMARY KEY,
    on_hand BIGINT NOT NULL,
    reserved BIGINT NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE idempotency (
    id VARCHAR(100) PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(100),
    payload CLOB,
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO stock (sku_id, on_hand, reserved, version) VALUES ('SKU123', 10, 0, 0);
