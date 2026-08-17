ALTER TABLE user_items
    ADD COLUMN purchase_order_number VARCHAR(100) NULL AFTER purchase_price,
    ADD COLUMN purchase_place VARCHAR(200) NULL AFTER purchase_order_number;
