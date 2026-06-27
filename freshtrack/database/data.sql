-- =====================================================================
-- FreshTrack — sample seed data (MySQL)
-- NOTE: The application also seeds equivalent data automatically on first
-- run via DataInitializer. Use this file only for manual DB bootstrapping.
-- Passwords below are BCrypt hashes of the documented demo passwords.
--   admin  -> admin123
--   hubdel -> hub123
--   hubmum -> hub123
-- =====================================================================
USE freshtrack;

INSERT INTO warehouses (warehouse_code, name, location, created_at) VALUES
  ('WH-DEL-01', 'Delhi North Hub',     'Delhi, IN',     NOW(6)),
  ('WH-MUM-01', 'Mumbai West Hub',     'Mumbai, IN',    NOW(6)),
  ('WH-BLR-01', 'Bengaluru South Hub', 'Bengaluru, IN', NOW(6));

INSERT INTO users (username, email, password, full_name, role, enabled, created_at) VALUES
  ('admin',  'admin@freshtrack.io',  '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQDqYpQ0c2qkbE4cTtY3xVQ0qWxYxhq', 'Central Administrator', 'CENTRAL_ADMIN', b'1', NOW(6)),
  ('hubdel', 'hubdel@freshtrack.io', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQDqYpQ0c2qkbE4cTtY3xVQ0qWxYxhq', 'Delhi Hub Operator',    'HUB_USER',      b'1', NOW(6)),
  ('hubmum', 'hubmum@freshtrack.io', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQDqYpQ0c2qkbE4cTtY3xVQ0qWxYxhq', 'Mumbai Hub Operator',   'HUB_USER',      b'1', NOW(6));

-- Map hub users to warehouses
INSERT INTO user_warehouses (user_id, warehouse_id)
SELECT u.id, w.id FROM users u, warehouses w
WHERE u.username = 'hubdel' AND w.warehouse_code = 'WH-DEL-01';

INSERT INTO user_warehouses (user_id, warehouse_id)
SELECT u.id, w.id FROM users u, warehouses w
WHERE u.username = 'hubmum' AND w.warehouse_code IN ('WH-MUM-01', 'WH-BLR-01');

-- Demo invoice + lines
INSERT INTO invoices (invoice_business_id, vendor_name, warehouse_id, status, uploaded_by, created_at)
SELECT 'INV-1001', 'FreshFarms Pvt Ltd', w.id, 'PENDING', 'admin', NOW(6)
FROM warehouses w WHERE w.warehouse_code = 'WH-DEL-01';

INSERT INTO invoice_lines (invoice_id, item_sku, item_name, expected_quantity, received_quantity, version, updated_at)
SELECT i.id, 'SKU-APPLE-001',  'Royal Gala Apple',  50, 0, 0, NOW(6) FROM invoices i WHERE i.invoice_business_id = 'INV-1001'
UNION ALL
SELECT i.id, 'SKU-BANANA-002', 'Cavendish Banana',  80, 0, 0, NOW(6) FROM invoices i WHERE i.invoice_business_id = 'INV-1001'
UNION ALL
SELECT i.id, 'SKU-TOMATO-003', 'Roma Tomato',       40, 0, 0, NOW(6) FROM invoices i WHERE i.invoice_business_id = 'INV-1001';
