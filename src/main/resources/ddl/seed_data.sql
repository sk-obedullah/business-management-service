-- =====================================================================
-- seed_data.sql  — Demo Data
-- Executed automatically on first launch via DatabaseConfig.java.
-- =====================================================================

-- Clear in FK-safe order
DELETE FROM order_line;
DELETE FROM "order";
DELETE FROM customer;
DELETE FROM product;

-- ══════════════════════════════════════════════════════════════════════
-- PRODUCTS  (12 realistic jewelry items)
-- ══════════════════════════════════════════════════════════════════════
INSERT INTO product (name, sku, category, metal, purity, weight_grams,
                     cost_price, selling_price, quantity_on_hand, description)
VALUES
-- ── Rings ─────────────────────────────────────────────────────────────
('Classic Solitaire Diamond Ring',  'RING-AU-22K-001', 'Ring',     'Gold',     '22K',  5.20,  28000.00,  42000.00,  8,  'Classic 4-prong solitaire with 0.5 ct diamond, 22K yellow gold band.'),
('Platinum Eternity Band',          'RING-PT-950-001', 'Ring',     'Platinum', '950',  6.80,  55000.00,  78000.00,  4,  'Full eternity band set with round brilliant diamonds, 950 platinum.'),
('Rose Gold Stackable Ring',        'RING-RG-18K-001', 'Ring',     'Gold',     '18K',  2.10,   4500.00,   7200.00, 15,  '18K rose gold minimalist stackable band, engraving available.'),
('Silver Filigree Ring',            'RING-AG-925-001', 'Ring',     'Silver',   '925',  3.40,    800.00,   1800.00, 20,  'Handcrafted 925 silver filigree ring with intricate floral pattern.'),

-- ── Necklaces ─────────────────────────────────────────────────────────
('Gold Chain Necklace 18"',         'NECK-AU-22K-001', 'Necklace', 'Gold',     '22K', 12.50,  65000.00,  92000.00,  6,  '22K gold rope chain necklace, 18 inches, lobster clasp.'),
('Diamond Pendant Necklace',        'NECK-AU-18K-001', 'Necklace', 'Gold',     '18K',  4.30,  32000.00,  52000.00,  5,  '18K white gold pendant with 0.3 ct brilliant diamond, 16" chain.'),
('Pearl Strand Necklace',           'NECK-PE-001',     'Necklace', 'Silver',   '925',  8.90,   5500.00,  11000.00, 10,  '7mm freshwater pearl strand with 925 silver clasp, 18".'),
('Silver Oxidised Choker',          'NECK-AG-925-001', 'Necklace', 'Silver',   '925',  6.20,   1200.00,   2800.00, 12,  'Handcrafted oxidised silver choker with tribal motifs.'),

-- ── Earrings ──────────────────────────────────────────────────────────
('Gold Stud Earrings (Diamond)',     'EARR-AU-18K-001', 'Earring',  'Gold',     '18K',  2.80,  18000.00,  28000.00,  9,  '18K white gold 4-prong diamond stud earrings, 0.2 ct each.'),
('Silver Jhumka Earrings',          'EARR-AG-925-001', 'Earring',  'Silver',   '925',  7.60,   1500.00,   3200.00, 18,  'Traditional Indian jhumka earrings in 925 silver with ghungroos.'),

-- ── Bracelets (low stock for dashboard test) ──────────────────────────
('22K Gold Bangle (Broad)',         'BRAC-AU-22K-001', 'Bracelet', 'Gold',     '22K', 22.40, 120000.00, 175000.00,  2,  'Broad 22K gold bangle with engraved peacock motif, size 2.6.'),
('Tennis Bracelet - Diamond',       'BRAC-PT-950-001', 'Bracelet', 'Platinum', '950', 10.50,  95000.00, 145000.00,  3,  '4 ct total weight diamonds in platinum four-prong channel setting, 7".');


-- ══════════════════════════════════════════════════════════════════════
-- CUSTOMERS  (8 realistic entries)
-- ══════════════════════════════════════════════════════════════════════
INSERT INTO customer (first_name, last_name, email, phone, address, notes)
VALUES
('Priya',    'Sharma',    'priya.sharma@gmail.com',    '+91 98765 43210', '12, Rose Garden, Bandra West, Mumbai 400050',    'VIP customer. Prefers gold jewellery. Anniversary in March.'),
('Rahul',    'Mehta',     'rahul.mehta@outlook.com',   '+91 97654 32109', '7, Shanti Nagar, Koramangala, Bengaluru 560034',  'Buys gifts for wife every anniversary. Platinum preference.'),
('Aisha',    'Khan',      'aisha.khan@yahoo.com',      '+91 96543 21098', '34B, Salt Lake, Sector V, Kolkata 700091',        'Interested in silver jewellery exclusively.'),
('Suresh',   'Iyer',      'suresh.iyer@gmail.com',     '+91 95432 10987', '5, Anna Nagar East, Chennai 600102',              'Bulk buyer - purchases for family functions.'),
('Deepika',  'Patel',     'deepika.patel@gmail.com',   '+91 94321 09876', '22, Navrangpura, Ahmedabad 380009',               'Referred by Priya Sharma. First-time buyer.'),
('Arjun',    'Nair',      'arjun.nair@hotmail.com',    '+91 93210 98765', 'Plot 8, Jubilee Hills, Hyderabad 500033',         'High-value buyer - interested in investment jewellery.'),
('Meena',    'Reddy',     'meena.reddy@gmail.com',     '+91 92109 87654', '101, Aundh, Pune 411007',                         'Prefers traditional designs. Repeat customer.'),
('Vikram',   'Singh',     'vikram.singh@gmail.com',    '+91 91098 76543', '14, Sector 21, Chandigarh 160022',                'Corporate gift buyer - invoices required.');


-- ══════════════════════════════════════════════════════════════════════
-- ORDERS  (6 orders in various states)
-- ══════════════════════════════════════════════════════════════════════
INSERT INTO "order" (customer_id, order_date, status, total_amount, discount, notes, created_at, updated_at)
VALUES (1, '2026-01-15 10:30:00', 'COMPLETED', 49200.00, 2000.00,
        'Giftwrapped. Delivered to Bandra address.', '2026-01-15 10:30:00', '2026-01-15 14:00:00');

INSERT INTO order_line (order_id, product_id, quantity, unit_price, cost_price) VALUES
(1, 3, 2, 7200.00, 4500.00),
(1, 7, 1, 11000.00, 5500.00);

INSERT INTO "order" (customer_id, order_date, status, total_amount, discount, notes, created_at, updated_at)
VALUES (2, '2026-01-28 15:00:00', 'COMPLETED', 145000.00, 5000.00,
        'Anniversary gift. Gift message included.', '2026-01-28 15:00:00', '2026-01-28 17:00:00');

INSERT INTO order_line (order_id, product_id, quantity, unit_price, cost_price) VALUES
(2, 12, 1, 145000.00, 95000.00);

INSERT INTO "order" (customer_id, order_date, status, total_amount, discount, notes, created_at, updated_at)
VALUES (4, '2026-02-05 11:00:00', 'COMPLETED', 22400.00, 0.00,
        'Family function purchase. Multiple items.', '2026-02-05 11:00:00', '2026-02-05 13:30:00');

INSERT INTO order_line (order_id, product_id, quantity, unit_price, cost_price) VALUES
(3, 4,  3, 1800.00, 800.00),
(3, 8,  2, 2800.00, 1200.00),
(3, 10, 3, 3200.00, 1500.00);

INSERT INTO "order" (customer_id, order_date, status, total_amount, discount, notes, created_at, updated_at)
VALUES (6, '2026-02-18 16:45:00', 'PROCESSING', 92000.00, 3000.00,
        'Investment purchase. Require hallmark certificate.', '2026-02-18 16:45:00', '2026-02-19 09:00:00');

INSERT INTO order_line (order_id, product_id, quantity, unit_price, cost_price) VALUES
(4, 5, 1, 92000.00, 65000.00);

INSERT INTO "order" (customer_id, order_date, status, total_amount, discount, notes, created_at, updated_at)
VALUES (5, '2026-02-25 09:15:00', 'PENDING', 49200.00, 0.00,
        'First-time customer. Called to confirm.', '2026-02-25 09:15:00', '2026-02-25 09:15:00');

INSERT INTO order_line (order_id, product_id, quantity, unit_price, cost_price) VALUES
(5, 1,  1, 42000.00, 28000.00),
(5, 10, 1,  3200.00,  1500.00);

INSERT INTO "order" (customer_id, order_date, status, total_amount, discount, notes, created_at, updated_at)
VALUES (7, '2026-02-20 14:00:00', 'CANCELLED', 28000.00, 0.00,
        'Customer cancelled - budget constraints.', '2026-02-20 14:00:00', '2026-02-20 16:00:00');

INSERT INTO order_line (order_id, product_id, quantity, unit_price, cost_price) VALUES
(6, 9, 1, 28000.00, 18000.00);

-- ══════════════════════════════════════════════════════════════════════
-- Adjust stock to reflect completed orders
-- ══════════════════════════════════════════════════════════════════════
UPDATE product SET quantity_on_hand =  13 WHERE id = 3;
UPDATE product SET quantity_on_hand =   9 WHERE id = 7;
UPDATE product SET quantity_on_hand =   2 WHERE id = 12;
UPDATE product SET quantity_on_hand =  17 WHERE id = 4;
UPDATE product SET quantity_on_hand =  10 WHERE id = 8;
UPDATE product SET quantity_on_hand =  15 WHERE id = 10;
