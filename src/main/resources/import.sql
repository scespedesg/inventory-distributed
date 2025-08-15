-- Initial data for the inventory database
-- The schema is managed by Hibernate; this script inserts seed data only

INSERT INTO stock (sku_id, on_hand, reserved, version)
VALUES ('SKU123', 10, 0, 0);
