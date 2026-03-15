-- ============================================================
-- V9__Initial_Inventory_Setup.sql
-- Updated to match current entity structure:
--   - stock_transaction: direction, batch_code, expiry_date,
--                        uom_code, production_batch_id columns
--   - production_batch:  expiry_date, status columns
--   - quantity always positive (direction handles sign)
--   - audit columns: created_by, updated_by, updated_at
-- ============================================================

-- 1. UNIT OF MEASURE
INSERT INTO unit_of_measure (id, uom_code, description, created_at) VALUES
                                                                        (nextval('unit_of_measure_seq'), 'PCS', 'Pieces',   NOW()),
                                                                        (nextval('unit_of_measure_seq'), 'KG',  'Kilograms', NOW());

-- 2. ITEM TYPES
INSERT INTO item_type (id, item_type_code, created_at) VALUES
                                                           (nextval('item_type_seq'), 'RAW_MATERIAL',  NOW()),
                                                           (nextval('item_type_seq'), 'FINISHED_GOOD', NOW());

-- 3. ITEMS — Raw Materials
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, created_at)
SELECT nextval('item_seq'), 'RM001', 'Sugar', it.id, uom.id, true, NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'RAW_MATERIAL' AND uom.uom_code = 'KG';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, created_at)
SELECT nextval('item_seq'), 'RM002', 'Flour', it.id, uom.id, true, NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'RAW_MATERIAL' AND uom.uom_code = 'KG';

-- 4. ITEMS — Finished Goods
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, created_at)
SELECT nextval('item_seq'), 'FG001', 'Bread', it.id, uom.id, true, NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

-- 5. STOCK OPENING — initial flour balance
INSERT INTO stock_opening (id, stock_date, item_id, opening_qty, created_at)
SELECT nextval('stock_opening_seq'), CURRENT_DATE - INTERVAL '1 day', id, 500.00, NOW()
FROM item WHERE item_code = 'RM002';

-- 6. PRODUCTION BATCH — sample finished batch with expiry date
INSERT INTO production_batch (id, batch_code, production_date, finished_item_id, output_qty, status, expiry_date, created_at)
SELECT nextval('production_batch_seq'),
       'PB-00001',
       CURRENT_DATE,
       id,
       100.00,
       'FINISHED',
       CURRENT_DATE + INTERVAL '21 days',   -- 21 day shelf life for bread
    NOW()
FROM item WHERE item_code = 'FG001';

-- 7. PRODUCTION CONSUMPTION — raw materials consumed in batch
INSERT INTO production_consumption (id, batch_id, raw_item_id, consumed_qty, created_at)
SELECT nextval('production_consumption_seq'),
       (SELECT id FROM production_batch WHERE batch_code = 'PB-00001'),
       (SELECT id FROM item WHERE item_code = 'RM002'),
       25.50,
       NOW();

-- 8. STOCK TRANSACTIONS — ledger entries
-- 8a. Opening stock IN for Flour
INSERT INTO stock_transaction (
    id, transaction_date, item_id,
    direction, transaction_type,
    quantity, uom_code,
    batch_code, expiry_date,
    production_batch_id, sales_invoice_id,
    reference_no, remarks, created_at
)
SELECT
    nextval('stock_transaction_seq'),
    CURRENT_DATE - INTERVAL '1 day',
    (SELECT id FROM item WHERE item_code = 'RM002'),
    'IN',
    'OPENING_STOCK',
    500.00,
    'KG',
    NULL, NULL, NULL, NULL,
    'OPENING-' || (CURRENT_DATE - INTERVAL '1 day'),
    'Opening stock entry',
    NOW();

-- 8b. Finished good output IN for Bread — links back to batch
INSERT INTO stock_transaction (
    id, transaction_date, item_id,
    direction, transaction_type,
    quantity, uom_code,
    batch_code, expiry_date,
    production_batch_id, sales_invoice_id,
    reference_no, remarks, created_at
)
SELECT
    nextval('stock_transaction_seq'),
    CURRENT_DATE,
    (SELECT id FROM item WHERE item_code = 'FG001'),
    'IN',
    'PRODUCTION_OUTPUT',
    100.00,
    'PCS',
    'PB-00001',
    CURRENT_DATE + INTERVAL '21 days',
    (SELECT id FROM production_batch WHERE batch_code = 'PB-00001'),
    NULL,
    'PB-00001',
    NULL,
    NOW();

-- 8c. Raw material consumption OUT for Flour — always positive, direction=OUT
INSERT INTO stock_transaction (
    id, transaction_date, item_id,
    direction, transaction_type,
    quantity, uom_code,
    batch_code, expiry_date,
    production_batch_id, sales_invoice_id,
    reference_no, remarks, created_at
)
SELECT
    nextval('stock_transaction_seq'),
    CURRENT_DATE,
    (SELECT id FROM item WHERE item_code = 'RM002'),
    'OUT',
    'PRODUCTION_CONSUME',
    25.50,               -- positive! direction=OUT handles the subtraction
    'KG',
    'PB-00001',
    NULL,
    (SELECT id FROM production_batch WHERE batch_code = 'PB-00001'),
    NULL,
    'PB-00001',
    NULL,
    NOW();