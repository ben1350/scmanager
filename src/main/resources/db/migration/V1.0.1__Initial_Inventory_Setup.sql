-- 1. UNIT OF MEASURE
INSERT INTO unit_of_measure (id, uom_code, description) VALUES
                                                            (nextval('unit_of_measure_seq'), 'PCS', 'Pieces'),
                                                            (nextval('unit_of_measure_seq'), 'KG', 'Kilograms');

-- 2. ITEM TYPES
INSERT INTO item_type (id, item_type_code) VALUES
                                               (nextval('item_type_seq'), 'RAW_MATERIAL'),
                                               (nextval('item_type_seq'), 'FINISHED_GOOD');

-- 3. ITEMS (Raw Materials)
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id)
SELECT nextval('item_seq'), 'RM001', 'Sugar', it.id, uom.id
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'RAW_MATERIAL' AND uom.uom_code = 'KG';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id)
SELECT nextval('item_seq'), 'RM002', 'Flour', it.id, uom.id
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'RAW_MATERIAL' AND uom.uom_code = 'KG';

-- 4. ITEMS (Finished Goods)
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id)
SELECT nextval('item_seq'), 'FG001', 'Bread', it.id, uom.id
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

-- 5. STOCK OPENING (Setting initial balance for Flour)
INSERT INTO stock_opening (id, stock_date, item_id, opening_qty)
SELECT nextval('stock_opening_seq'), CURRENT_DATE - INTERVAL '1 day', id, 500.00
FROM item WHERE item_code = 'RM002';

-- 6. PRODUCTION BATCH (A sample finished batch)
INSERT INTO production_batch (id, batch_code, production_date, finished_item_id, status)
SELECT nextval('production_batch_seq'), 'PB-00001', CURRENT_DATE, id, 'FINISHED'
FROM item WHERE item_code = 'FG001';

-- 7. PRODUCTION CONSUMPTION (Linking raw materials used in the batch above)
INSERT INTO production_consumption (id, batch_id, raw_item_id, consumed_qty)
SELECT nextval('production_consumption_seq'),
       (SELECT id FROM production_batch WHERE batch_code = 'PB-00001'),
       (SELECT id FROM item WHERE item_code = 'RM002'),
       25.50;

-- 8. THE LEDGER ENTRIES (Critical for your dynamic output qty)
-- Output for Finished Good
INSERT INTO stock_transaction (id, transaction_date, item_id, transaction_type, quantity, reference_no, created_at)
SELECT nextval('stock_transaction_seq'), CURRENT_DATE,
       (SELECT id FROM item WHERE item_code = 'FG001'),
       'PRODUCTION_OUTPUT', 100.00, 'PB-00001', NOW();

-- Consumption for Raw Material
INSERT INTO stock_transaction (id, transaction_date, item_id, transaction_type, quantity, reference_no, created_at)
SELECT nextval('stock_transaction_seq'), CURRENT_DATE,
       (SELECT id FROM item WHERE item_code = 'RM002'),
       'PRODUCTION_CONSUME', -25.50, 'PB-00001', NOW();