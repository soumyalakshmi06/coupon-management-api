-- 🛒 Sample Products
INSERT INTO products (id, name, price) VALUES (1, 'Laptop', 50.0);
INSERT INTO products (id, name, price) VALUES (2, 'Mouse', 30.0);
INSERT INTO products (id, name, price) VALUES (3, 'Keyboard', 25.0);

-- 🎟️ Sample Coupons (optional, if you already have coupon table)
INSERT INTO coupons (coupon_code, type, discount, is_active, expiry_date, threshold, product_id)
VALUES
('CART10', 'CART_WISE', 10.0, TRUE, '2030-12-31', 100.0, NULL),
('PROD20', 'PRODUCT_WISE', 20.0, TRUE, '2030-12-31', NULL, 1);
