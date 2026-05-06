-- ============================================================
-- V12__Rosswood_Sample_Data.sql
-- Sample data seed for Rosswood Inventory System
-- Based on real business data extracted from:
--   - RAW_MATERIALS_YOGHURT.xlsx
--   - YOGHURT_BOTTLES_INVENTORIES_.xlsx
--   - YOGHURT_FINISHED_GOODS__1_.xlsx  (FG1 — closing stock as of 2026-03-01)
--   - YOGHURT_FINISHED_GOODS__2_.xlsx  (FG2 — blank template, SKU reference)
--   - NEW_PRICE_LIST_YOGHURT_02_02_2026.pdf
--   - NEW_PRICE_LIST_CASHEW_02_02_2026.pdf
-- VAT rate: 20% (Ghana standard rate)
-- Yoghurt shelf life: 21 days  |  Cashew/Granola: 180 days
-- Stock opening date: 2026-01-01
-- ============================================================

-- ============================================================
-- 1. UNITS OF MEASURE
-- ============================================================
INSERT INTO unit_of_measure (id, uom_code, description, created_by, created_at) VALUES
                                                                                    (nextval('unit_of_measure_seq'), 'PCS',   'Pieces',       'admin', NOW()),
                                                                                    (nextval('unit_of_measure_seq'), 'KG',    'Kilograms',    'admin', NOW()),
                                                                                    (nextval('unit_of_measure_seq'), 'BAG',   'Bags',         'admin', NOW()),
                                                                                    (nextval('unit_of_measure_seq'), 'LITRE', 'Litres',       'admin', NOW()),
                                                                                    (nextval('unit_of_measure_seq'), 'SACHET','Sachets',      'admin', NOW()),
                                                                                    (nextval('unit_of_measure_seq'), 'CASE',  'Cases',        'admin', NOW()),
                                                                                    (nextval('unit_of_measure_seq'), 'UNIT',  'Units',        'admin', NOW());

-- ============================================================
-- 2. ITEM TYPES
-- ============================================================
INSERT INTO item_type (id, item_type_code, created_by, created_at) VALUES
                                                                       (nextval('item_type_seq'), 'RAW_MATERIAL',  'admin', NOW()),
                                                                       (nextval('item_type_seq'), 'FINISHED_GOOD', 'admin', NOW()),
                                                                       (nextval('item_type_seq'), 'PACKAGING',     'admin', NOW());

-- ============================================================
-- 3. ITEMS — Raw Materials
--    Source: RAW_MATERIALS_YOGHURT.xlsx > Summary sheet
--    UOM: BAG for bagged products, KG for loose, LITRE for milk, SACHET for culture
-- ============================================================

-- WHITE SUGAR 50KG bags
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, shelf_life_days, created_by, created_at)
SELECT nextval('item_seq'), 'RM001', 'White Sugar 50KG', it.id, uom.id, true, NULL, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'RAW_MATERIAL' AND uom.uom_code = 'BAG';

-- MILK SUPER LAC (powdered milk)
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, shelf_life_days, created_by, created_at)
SELECT nextval('item_seq'), 'RM002', 'Milk Super Lac', it.id, uom.id, true, NULL, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'RAW_MATERIAL' AND uom.uom_code = 'BAG';

-- DANO MILK (powdered milk)
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, shelf_life_days, created_by, created_at)
SELECT nextval('item_seq'), 'RM003', 'Dano Milk', it.id, uom.id, true, NULL, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'RAW_MATERIAL' AND uom.uom_code = 'BAG';

-- COW MILK (fresh)
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, shelf_life_days, created_by, created_at)
SELECT nextval('item_seq'), 'RM004', 'Cow Milk', it.id, uom.id, true, 2, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'RAW_MATERIAL' AND uom.uom_code = 'LITRE';

-- WHEAT
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, shelf_life_days, created_by, created_at)
SELECT nextval('item_seq'), 'RM005', 'Wheat', it.id, uom.id, true, NULL, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'RAW_MATERIAL' AND uom.uom_code = 'KG';

-- MILLET
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, shelf_life_days, created_by, created_at)
SELECT nextval('item_seq'), 'RM006', 'Millet', it.id, uom.id, true, NULL, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'RAW_MATERIAL' AND uom.uom_code = 'KG';

-- CULTURE (yoghurt starter)
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, shelf_life_days, created_by, created_at)
SELECT nextval('item_seq'), 'RM007', 'Culture', it.id, uom.id, true, 90, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'RAW_MATERIAL' AND uom.uom_code = 'SACHET';

-- BENZOATE (preservative)
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, shelf_life_days, created_by, created_at)
SELECT nextval('item_seq'), 'RM008', 'Benzoate', it.id, uom.id, true, NULL, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'RAW_MATERIAL' AND uom.uom_code = 'KG';

-- VANILLA FLAVOR
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, shelf_life_days, created_by, created_at)
SELECT nextval('item_seq'), 'RM009', 'Vanilla Flavor', it.id, uom.id, true, NULL, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'RAW_MATERIAL' AND uom.uom_code = 'LITRE';

-- STRAWBERRY FLAVOR
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, shelf_life_days, created_by, created_at)
SELECT nextval('item_seq'), 'RM010', 'Strawberry Flavor', it.id, uom.id, true, NULL, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'RAW_MATERIAL' AND uom.uom_code = 'LITRE';

-- ============================================================
-- 4. ITEMS — Packaging Materials
--    Source: YOGHURT_BOTTLES_INVENTORIES_.xlsx > Summary
-- ============================================================

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, created_by, created_at)
SELECT nextval('item_seq'), 'PKG001', '330ml Bottles', it.id, uom.id, true, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'PACKAGING' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, created_by, created_at)
SELECT nextval('item_seq'), 'PKG002', 'GK 750G Cup', it.id, uom.id, true, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'PACKAGING' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, created_by, created_at)
SELECT nextval('item_seq'), 'PKG003', 'GK 500G Cup', it.id, uom.id, true, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'PACKAGING' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, created_by, created_at)
SELECT nextval('item_seq'), 'PKG004', 'GK 250G Cup', it.id, uom.id, true, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'PACKAGING' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, created_by, created_at)
SELECT nextval('item_seq'), 'PKG005', '2L Gallon', it.id, uom.id, true, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'PACKAGING' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, created_by, created_at)
SELECT nextval('item_seq'), 'PKG006', '5L Gallon', it.id, uom.id, true, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'PACKAGING' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, created_by, created_at)
SELECT nextval('item_seq'), 'PKG007', 'Packing Bags', it.id, uom.id, true, 'admin', NOW()
FROM item_type it, unit_of_measure uom
WHERE it.item_type_code = 'PACKAGING' AND uom.uom_code = 'PCS';

-- ============================================================
-- 5. ITEMS — Finished Goods (Yoghurt)
--    Source: YOGHURT_FINISHED_GOODS__1_.xlsx + Price List PDF
--    All yoghurt SKUs: shelf_life_days=21, vat_rate=20.00
--    Price list (VAT ex):
--      330ML case = GHS 138.00 (per case of 12, so unit = 11.50)
--      2L          = GHS 62.00 per unit
--      GK 250G     = GHS 20.00 plain / 22.00 honey
--      GK 500G     = GHS 37.00
--      GK 750G     = GHS 55.00
-- ============================================================

-- 330ML range (price per unit ex-VAT = 138.00/12 = 11.50)
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG001', 'One Yogo Strawberry 330ML', it.id, uom.id, true, 11.50, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG002', 'One Yogo Vanilla 330ML', it.id, uom.id, true, 11.50, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG003', 'One Yogo Vanilla & Wheat 330ML', it.id, uom.id, true, 11.50, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG004', 'One Yogo Strawberry & Wheat 330ML', it.id, uom.id, true, 11.50, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG005', 'One Yogo Sugar Free 330ML', it.id, uom.id, true, 11.50, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG006', 'One Yogo Sugar Free & Wheat 330ML', it.id, uom.id, true, 11.50, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG007', 'One Yogo Brukina 330ML', it.id, uom.id, true, 11.50, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG008', 'One Yogo With Honey 330ML', it.id, uom.id, true, 11.50, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

-- Greek range
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG009', 'OneYogo Greek Plain 250G', it.id, uom.id, true, 20.00, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG010', 'OneYogo Greek Honey 250G', it.id, uom.id, true, 22.00, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG011', 'OneYogo Greek Plain 500G', it.id, uom.id, true, 37.00, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG012', 'OneYogo Greek Spring 500G', it.id, uom.id, true, 37.00, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG013', 'OneYogo Greek 750G', it.id, uom.id, true, 55.00, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

-- 2L range (price per unit ex-VAT = GHS 62.00)
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG014', 'One Yogo Strawberry 2L', it.id, uom.id, true, 62.00, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG015', 'One Yogo Vanilla 2L', it.id, uom.id, true, 62.00, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG016', 'One Yogo Strawberry & Wheat 2L', it.id, uom.id, true, 62.00, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG017', 'One Yogo Vanilla & Wheat 2L', it.id, uom.id, true, 62.00, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG018', 'One Yogo Sugar Free & Wheat 2L', it.id, uom.id, true, 62.00, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG019', 'One Yogo Sugar Free 2L', it.id, uom.id, true, 62.00, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG020', 'One Yogo Brukina 2L', it.id, uom.id, true, 62.00, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

-- 5L range (no specific price in list — using proportional estimate)
INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG021', 'One Yogo Sobolo 5L', it.id, uom.id, true, 145.00, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG022', 'One Yogo Asaana 5L', it.id, uom.id, true, 145.00, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG023', 'One Yogo Brukina 5L', it.id, uom.id, true, 145.00, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG024', 'One Yogo Labgen 5L', it.id, uom.id, true, 145.00, 21, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

-- ============================================================
-- 6. ITEMS — Finished Goods (Cashew & Granola)
--    Source: NEW_PRICE_LIST_CASHEW_02_02_2026.pdf
--    shelf_life_days = 180 (6 months), vat_rate = 20.00
-- ============================================================

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG101', 'Cashew Nuts Salted 168G', it.id, uom.id, true, 35.50, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG102', 'Cashew Nuts No Salt 168G', it.id, uom.id, true, 35.50, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG103', 'Cashew Nuts Chocolate 150G', it.id, uom.id, true, 33.50, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG104', 'Cashew Nuts Spicy Chilli 168G', it.id, uom.id, true, 35.50, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG105', 'Cashew Roasted No Salt 25G', it.id, uom.id, true, 5.50, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG106', 'Cashew Roasted Salted 25G', it.id, uom.id, true, 5.50, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG107', 'Cashew Roasted No Salt 40G', it.id, uom.id, true, 8.30, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG108', 'Cashew Roasted Salted 40G', it.id, uom.id, true, 8.30, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG109', 'Cashew Roasted No Salt 250G', it.id, uom.id, true, 52.00, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG110', 'Cashew Roasted Salted 250G', it.id, uom.id, true, 52.00, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG111', 'Raw Cashew Kernel 500G', it.id, uom.id, true, 85.00, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG112', 'Spread Cashew Plain 350G', it.id, uom.id, true, 37.20, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG113', 'Granola PM Chocolate 200G', it.id, uom.id, true, 25.96, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG114', 'Granola PM Coconut 200G', it.id, uom.id, true, 25.96, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG115', 'Granola PM With Cashew 200G', it.id, uom.id, true, 25.96, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG116', 'Granola PM Chocolate 500G', it.id, uom.id, true, 55.00, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG117', 'Granola PM Coconut 500G', it.id, uom.id, true, 55.00, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

INSERT INTO item (id, item_code, item_name, item_type_id, uom_id, active_flag, selling_price_ex_vat, shelf_life_days, vat_rate, created_by, created_at)
SELECT nextval('item_seq'), 'FG118', 'Granola PM With Cashew 500G', it.id, uom.id, true, 55.00, 180, 20.00, 'admin', NOW()
FROM item_type it, unit_of_measure uom WHERE it.item_type_code = 'FINISHED_GOOD' AND uom.uom_code = 'PCS';

-- ============================================================
-- 7. ITEM UOM (base UOM registration for each item)
--    Required by sales_invoice_item.uom_id FK
-- ============================================================
INSERT INTO item_uom (id, item_id, uom_code, is_base, created_by, created_at)
SELECT nextval('item_uom_seq'), i.id, u.uom_code, true, 'admin', NOW()
FROM item i
         JOIN unit_of_measure u ON u.id = i.uom_id
WHERE i.item_code IN (
                      'RM001','RM002','RM003','RM004','RM005','RM006','RM007','RM008','RM009','RM010',
                      'PKG001','PKG002','PKG003','PKG004','PKG005','PKG006','PKG007',
                      'FG001','FG002','FG003','FG004','FG005','FG006','FG007','FG008',
                      'FG009','FG010','FG011','FG012','FG013',
                      'FG014','FG015','FG016','FG017','FG018','FG019','FG020',
                      'FG021','FG022','FG023','FG024',
                      'FG101','FG102','FG103','FG104','FG105','FG106','FG107','FG108',
                      'FG109','FG110','FG111','FG112','FG113','FG114','FG115','FG116','FG117','FG118'
    );

-- CASE UOM for finished goods (case of 12 for 330ML, case of 6 for 500G granola etc.)
INSERT INTO item_uom (id, item_id, uom_code, is_base, created_by, created_at)
SELECT nextval('item_uom_seq'), id, 'CASE', false, 'admin', NOW()
FROM item WHERE item_code IN (
                              'FG001','FG002','FG003','FG004','FG005','FG006','FG007','FG008',
                              'FG101','FG102','FG103','FG104','FG109','FG110','FG111','FG112'
    );

-- ============================================================
-- 8. ITEM UOM CONVERSION (cases <-> units)
-- ============================================================

-- 330ML: 1 CASE = 12 PCS
INSERT INTO item_uom_conversion (id, item_id, from_uom, to_uom, conversion_factor, created_by, created_at)
SELECT nextval('item_uom_conversion_seq'), id, 'CASE', 'PCS', 12.0000, 'admin', NOW()
FROM item WHERE item_code IN ('FG001','FG002','FG003','FG004','FG005','FG006','FG007','FG008');

-- 168G Cashew: 1 CASE = 24 PCS
INSERT INTO item_uom_conversion (id, item_id, from_uom, to_uom, conversion_factor, created_by, created_at)
SELECT nextval('item_uom_conversion_seq'), id, 'CASE', 'PCS', 24.0000, 'admin', NOW()
FROM item WHERE item_code IN ('FG101','FG102','FG104');

-- 250G Cashew: 1 CASE = 15 PCS
INSERT INTO item_uom_conversion (id, item_id, from_uom, to_uom, conversion_factor, created_by, created_at)
SELECT nextval('item_uom_conversion_seq'), id, 'CASE', 'PCS', 15.0000, 'admin', NOW()
FROM item WHERE item_code IN ('FG109','FG110');

-- 500G Cashew Kernel: 1 CASE = 12 PCS
INSERT INTO item_uom_conversion (id, item_id, from_uom, to_uom, conversion_factor, created_by, created_at)
SELECT nextval('item_uom_conversion_seq'), id, 'CASE', 'PCS', 12.0000, 'admin', NOW()
FROM item WHERE item_code IN ('FG111','FG112');

-- ============================================================
-- 9. CUSTOMERS
--    Source: Daily_Activity_Report_2_Jan_2026.xlsx (AIRPORT MELCOM)
--    Additional representative customers seeded for demo purposes
-- ============================================================
INSERT INTO customer (id, customer_code, name, address, phone, created_by, created_at) VALUES
                                                                                           (nextval('customer_seq'), 'CUST001', 'Airport Melcom',        'Airport City, Accra',        '+233 302 000001', 'admin', NOW()),
                                                                                           (nextval('customer_seq'), 'CUST002', 'Shoprite Ghana',        'Accra Mall, Spintex',        '+233 302 000002', 'admin', NOW()),
                                                                                           (nextval('customer_seq'), 'CUST003', 'Palace Hypermarket',    'Tema, Greater Accra',        '+233 303 000003', 'admin', NOW()),
                                                                                           (nextval('customer_seq'), 'CUST004', 'Max Mart',              'East Legon, Accra',          '+233 302 000004', 'admin', NOW()),
                                                                                           (nextval('customer_seq'), 'CUST005', 'Wan Grocery',           'Osu, Accra',                 '+233 302 000005', 'admin', NOW());

-- ============================================================
-- 10. CUSTOMER BRANCHES
-- ============================================================
INSERT INTO customer_branch (id, customer_id, branch_name, branchAddress, contactPerson, contactPhone, active_flag, created_by, created_at)
SELECT nextval('customer_branch_seq'), id, 'Airport Melcom - Main Branch', 'Airport City, Accra', 'Procurement Manager', '+233 302 000001', true, 'admin', NOW()
FROM customer WHERE customer_code = 'CUST001';

INSERT INTO customer_branch (id, customer_id, branch_name, branchAddress, contactPerson, contactPhone, active_flag, created_by, created_at)
SELECT nextval('customer_branch_seq'), id, 'Shoprite Accra Mall', 'Accra Mall, Spintex Road', 'Grocery Buyer', '+233 302 000002', true, 'admin', NOW()
FROM customer WHERE customer_code = 'CUST002';

INSERT INTO customer_branch (id, customer_id, branch_name, branchAddress, contactPerson, contactPhone, active_flag, created_by, created_at)
SELECT nextval('customer_branch_seq'), id, 'Palace Tema', 'Community 25, Tema', 'Supermarket Lead', '+233 303 000003', true, 'admin', NOW()
FROM customer WHERE customer_code = 'CUST003';

INSERT INTO customer_branch (id, customer_id, branch_name, branchAddress, contactPerson, contactPhone, active_flag, created_by, created_at)
SELECT nextval('customer_branch_seq'), id, 'Max Mart East Legon', 'East Legon, Accra', 'Store Manager', '+233 302 000004', true, 'admin', NOW()
FROM customer WHERE customer_code = 'CUST004';

INSERT INTO customer_branch (id, customer_id, branch_name, branchAddress, contactPerson, contactPhone, active_flag, created_by, created_at)
SELECT nextval('customer_branch_seq'), id, 'Wan Grocery Osu', 'Oxford St, Osu, Accra', 'Owner', '+233 302 000005', true, 'admin', NOW()
FROM customer WHERE customer_code = 'CUST005';

-- ============================================================
-- 11. STOCK OPENING
--    Date: 2026-01-01 (start of year)
--    Source: RAW_MATERIALS_YOGHURT.xlsx Summary (opening qty column)
--    Finished goods opening from YOGHURT_FINISHED_GOODS__1_.xlsx Summary
-- ============================================================

-- Raw materials
INSERT INTO stock_opening (id, stock_date, item_id, opening_qty, created_by, created_at)
SELECT nextval('stock_opening_seq'), '2026-01-01', id, 17.00, 'admin', NOW()
FROM item WHERE item_code = 'RM001'; -- White Sugar: 17 bags

INSERT INTO stock_opening (id, stock_date, item_id, opening_qty, created_by, created_at)
SELECT nextval('stock_opening_seq'), '2026-01-01', id, 16.00, 'admin', NOW()
FROM item WHERE item_code = 'RM002'; -- Milk Super Lac: 16 bags

INSERT INTO stock_opening (id, stock_date, item_id, opening_qty, created_by, created_at)
SELECT nextval('stock_opening_seq'), '2026-01-01', id, 8.00, 'admin', NOW()
FROM item WHERE item_code = 'RM003'; -- Dano Milk: 8 bags

INSERT INTO stock_opening (id, stock_date, item_id, opening_qty, created_by, created_at)
SELECT nextval('stock_opening_seq'), '2026-01-01', id, 44.00, 'admin', NOW()
FROM item WHERE item_code = 'RM004'; -- Cow Milk: 44 litres

-- Finished goods (opening qtys from YOGHURT_FINISHED_GOODS__1_.xlsx Summary)
INSERT INTO stock_opening (id, stock_date, item_id, opening_qty, created_by, created_at)
SELECT nextval('stock_opening_seq'), '2026-01-01', id, 9.00, 'admin', NOW()
FROM item WHERE item_code = 'FG001'; -- S 330ML

INSERT INTO stock_opening (id, stock_date, item_id, opening_qty, created_by, created_at)
SELECT nextval('stock_opening_seq'), '2026-01-01', id, 33.00, 'admin', NOW()
FROM item WHERE item_code = 'FG002'; -- V 330ML

INSERT INTO stock_opening (id, stock_date, item_id, opening_qty, created_by, created_at)
SELECT nextval('stock_opening_seq'), '2026-01-01', id, 36.00, 'admin', NOW()
FROM item WHERE item_code = 'FG003'; -- VW 330ML

INSERT INTO stock_opening (id, stock_date, item_id, opening_qty, created_by, created_at)
SELECT nextval('stock_opening_seq'), '2026-01-01', id, 30.00, 'admin', NOW()
FROM item WHERE item_code = 'FG004'; -- SW 330ML

INSERT INTO stock_opening (id, stock_date, item_id, opening_qty, created_by, created_at)
SELECT nextval('stock_opening_seq'), '2026-01-01', id, 32.00, 'admin', NOW()
FROM item WHERE item_code = 'FG005'; -- SF 330ML

INSERT INTO stock_opening (id, stock_date, item_id, opening_qty, created_by, created_at)
SELECT nextval('stock_opening_seq'), '2026-01-01', id, 16.00, 'admin', NOW()
FROM item WHERE item_code = 'FG006'; -- SFW 330ML

INSERT INTO stock_opening (id, stock_date, item_id, opening_qty, created_by, created_at)
SELECT nextval('stock_opening_seq'), '2026-01-01', id, 44.00, 'admin', NOW()
FROM item WHERE item_code = 'FG013'; -- GK 750G

INSERT INTO stock_opening (id, stock_date, item_id, opening_qty, created_by, created_at)
SELECT nextval('stock_opening_seq'), '2026-01-01', id, 186.00, 'admin', NOW()
FROM item WHERE item_code = 'FG011'; -- GK 500G

INSERT INTO stock_opening (id, stock_date, item_id, opening_qty, created_by, created_at)
SELECT nextval('stock_opening_seq'), '2026-01-01', id, 142.00, 'admin', NOW()
FROM item WHERE item_code = 'FG009'; -- GK 250G Plain

INSERT INTO stock_opening (id, stock_date, item_id, opening_qty, created_by, created_at)
SELECT nextval('stock_opening_seq'), '2026-01-01', id, 23.00, 'admin', NOW()
FROM item WHERE item_code = 'FG018'; -- SFW 2L

INSERT INTO stock_opening (id, stock_date, item_id, opening_qty, created_by, created_at)
SELECT nextval('stock_opening_seq'), '2026-01-01', id, 16.00, 'admin', NOW()
FROM item WHERE item_code = 'FG019'; -- SF 2L

-- ============================================================
-- 12. STOCK TRANSACTIONS — Opening stock IN entries
--    Mirrors stock_opening records as ledger entries
-- ============================================================

INSERT INTO stock_transaction (
    id, transaction_date, item_id, direction, transaction_type,
    quantity, uom_code, batch_code, expiry_date,
    production_batch_id, sales_invoice_id, reference_no, remarks, created_by, created_at
)
SELECT nextval('stock_transaction_seq'),
       '2026-01-01', id, 'IN', 'OPENING_STOCK',
       17.00, 'BAG', NULL, NULL, NULL, NULL,
       'OPEN-2026-01-01', 'Opening stock', 'admin', NOW()
FROM item WHERE item_code = 'RM001';

INSERT INTO stock_transaction (
    id, transaction_date, item_id, direction, transaction_type,
    quantity, uom_code, batch_code, expiry_date,
    production_batch_id, sales_invoice_id, reference_no, remarks, created_by, created_at
)
SELECT nextval('stock_transaction_seq'),
       '2026-01-01', id, 'IN', 'OPENING_STOCK',
       16.00, 'BAG', NULL, NULL, NULL, NULL,
       'OPEN-2026-01-01', 'Opening stock', 'admin', NOW()
FROM item WHERE item_code = 'RM002';

INSERT INTO stock_transaction (
    id, transaction_date, item_id, direction, transaction_type,
    quantity, uom_code, batch_code, expiry_date,
    production_batch_id, sales_invoice_id, reference_no, remarks, created_by, created_at
)
SELECT nextval('stock_transaction_seq'),
       '2026-01-01', id, 'IN', 'OPENING_STOCK',
       8.00, 'BAG', NULL, NULL, NULL, NULL,
       'OPEN-2026-01-01', 'Opening stock', 'admin', NOW()
FROM item WHERE item_code = 'RM003';

INSERT INTO stock_transaction (
    id, transaction_date, item_id, direction, transaction_type,
    quantity, uom_code, batch_code, expiry_date,
    production_batch_id, sales_invoice_id, reference_no, remarks, created_by, created_at
)
SELECT nextval('stock_transaction_seq'),
       '2026-01-01', id, 'IN', 'OPENING_STOCK',
       44.00, 'LITRE', NULL, NULL, NULL, NULL,
       'OPEN-2026-01-01', 'Opening stock', 'admin', NOW()
FROM item WHERE item_code = 'RM004';

-- Finished goods opening stock transactions
INSERT INTO stock_transaction (
    id, transaction_date, item_id, direction, transaction_type,
    quantity, uom_code, batch_code, expiry_date,
    production_batch_id, sales_invoice_id, reference_no, remarks, created_by, created_at
)
SELECT nextval('stock_transaction_seq'),
       '2026-01-01', i.id, 'IN', 'OPENING_STOCK',
       so.opening_qty, 'PCS', NULL, NULL, NULL, NULL,
       'OPEN-2026-01-01', 'Opening stock', 'admin', NOW()
FROM stock_opening so
         JOIN item i ON i.id = so.item_id
WHERE so.stock_date = '2026-01-01'
  AND i.item_code IN ('FG001','FG002','FG003','FG004','FG005','FG006',
                      'FG009','FG011','FG013','FG018','FG019');

-- ============================================================
-- 13. PRODUCTION BATCH — sample batch for 2026-01-02
--    Finished item: One Yogo Vanilla 330ML (FG002)
--    Output: 200 units
-- ============================================================
INSERT INTO production_batch (
    id, batch_code, production_date, finished_item_id,
    output_qty, status, expiry_date, created_by, created_at
)
SELECT nextval('production_batch_seq'),
       'PB-20260102-001',
       '2026-01-02',
       id,
       200.00,
       'FINISHED',
       '2026-01-23',   -- 21-day shelf life
       'admin', NOW()
FROM item WHERE item_code = 'FG002';

-- ============================================================
-- 14. PRODUCTION CONSUMPTION — materials consumed in PB-20260102-001
-- ============================================================
INSERT INTO production_consumption (id, batch_id, raw_item_id, consumed_qty, created_by, created_at)
SELECT nextval('production_consumption_seq'),
       (SELECT id FROM production_batch WHERE batch_code = 'PB-20260102-001'),
       id, 2.00, 'admin', NOW()
FROM item WHERE item_code = 'RM001'; -- 2 bags White Sugar

INSERT INTO production_consumption (id, batch_id, raw_item_id, consumed_qty, created_by, created_at)
SELECT nextval('production_consumption_seq'),
       (SELECT id FROM production_batch WHERE batch_code = 'PB-20260102-001'),
       id, 80.00, 'admin', NOW()
FROM item WHERE item_code = 'RM004'; -- 80 litres Cow Milk

INSERT INTO production_consumption (id, batch_id, raw_item_id, consumed_qty, created_by, created_at)
SELECT nextval('production_consumption_seq'),
       (SELECT id FROM production_batch WHERE batch_code = 'PB-20260102-001'),
       id, 1.00, 'admin', NOW()
FROM item WHERE item_code = 'RM009'; -- 1 litre Vanilla Flavor

INSERT INTO production_consumption (id, batch_id, raw_item_id, consumed_qty, created_by, created_at)
SELECT nextval('production_consumption_seq'),
       (SELECT id FROM production_batch WHERE batch_code = 'PB-20260102-001'),
       id, 2.00, 'admin', NOW()
FROM item WHERE item_code = 'RM007'; -- 2 sachets Culture

-- ============================================================
-- 15. STOCK TRANSACTIONS — Production batch ledger
-- ============================================================

-- 15a. Finished goods IN from production
INSERT INTO stock_transaction (
    id, transaction_date, item_id, direction, transaction_type,
    quantity, uom_code, batch_code, expiry_date,
    production_batch_id, sales_invoice_id, reference_no, remarks, created_by, created_at
)
SELECT nextval('stock_transaction_seq'),
       '2026-01-02',
       (SELECT id FROM item WHERE item_code = 'FG002'),
       'IN', 'PRODUCTION_OUTPUT',
       200.00, 'PCS',
       'PB-20260102-001', '2026-01-23',
       (SELECT id FROM production_batch WHERE batch_code = 'PB-20260102-001'),
       NULL, 'PB-20260102-001', NULL, 'admin', NOW();

-- 15b. Raw material consumption OUT
INSERT INTO stock_transaction (
    id, transaction_date, item_id, direction, transaction_type,
    quantity, uom_code, batch_code, expiry_date,
    production_batch_id, sales_invoice_id, reference_no, remarks, created_by, created_at
)
SELECT nextval('stock_transaction_seq'),
       '2026-01-02',
       (SELECT id FROM item WHERE item_code = 'RM001'),
       'OUT', 'PRODUCTION_CONSUME',
       2.00, 'BAG',
       'PB-20260102-001', NULL,
       (SELECT id FROM production_batch WHERE batch_code = 'PB-20260102-001'),
       NULL, 'PB-20260102-001', NULL, 'admin', NOW();

INSERT INTO stock_transaction (
    id, transaction_date, item_id, direction, transaction_type,
    quantity, uom_code, batch_code, expiry_date,
    production_batch_id, sales_invoice_id, reference_no, remarks, created_by, created_at
)
SELECT nextval('stock_transaction_seq'),
       '2026-01-02',
       (SELECT id FROM item WHERE item_code = 'RM004'),
       'OUT', 'PRODUCTION_CONSUME',
       80.00, 'LITRE',
       'PB-20260102-001', NULL,
       (SELECT id FROM production_batch WHERE batch_code = 'PB-20260102-001'),
       NULL, 'PB-20260102-001', NULL, 'admin', NOW();

INSERT INTO stock_transaction (
    id, transaction_date, item_id, direction, transaction_type,
    quantity, uom_code, batch_code, expiry_date,
    production_batch_id, sales_invoice_id, reference_no, remarks, created_by, created_at
)
SELECT nextval('stock_transaction_seq'),
       '2026-01-02',
       (SELECT id FROM item WHERE item_code = 'RM009'),
       'OUT', 'PRODUCTION_CONSUME',
       1.00, 'LITRE',
       'PB-20260102-001', NULL,
       (SELECT id FROM production_batch WHERE batch_code = 'PB-20260102-001'),
       NULL, 'PB-20260102-001', NULL, 'admin', NOW();

-- ============================================================
-- 16. SALES INVOICE — Airport Melcom, 2026-01-02
--    Source: Daily_Activity_Report_2_Jan_2026.xlsx
--    Invoice total: GHS 765.60 (VAT inc)
--    = 638.00 ex-VAT + 127.60 VAT (20%)
-- ============================================================
INSERT INTO sales_invoice (
    id, rosswood_invoice_no, vat_invoice_no,
    customer_branch_id, invoice_date, delivery_date,
    delivery_note_no, status,
    total_amount, total_amount_ex_vat, vat_amount,
    remarks, created_by, created_at
)
SELECT nextval('sales_invoice_seq'),
       'RW-INV-20260102-001',
       'VAT-20260102-001',
       (SELECT cb.id FROM customer_branch cb JOIN customer c ON c.id = cb.customer_id WHERE c.customer_code = 'CUST001' LIMIT 1),
       '2026-01-02',
       '2026-01-02',
       'DN-20260102-001',
       'DELIVERED',
       765.60,    -- total inc VAT
       638.00,    -- ex VAT
       127.60,    -- VAT amount
       'Airport Melcom delivery — corrected invoice',
       'admin', NOW();

-- ============================================================
-- 17. SALES INVOICE ITEMS
--    Vanilla 330ML: 55 units @ GHS 11.50 ex-VAT = GHS 632.50, VAT = GHS 126.50
-- ============================================================
INSERT INTO sales_invoice_item (
    id, invoice_id, item_id, uom_id,
    quantity, unit_price_ex_vat,
    line_total_ex_vat, vat_rate, vat_amount, line_total_inc_vat,
    batch_code, created_by, created_at
)
SELECT nextval('sales_invoice_item_seq'),
       (SELECT id FROM sales_invoice WHERE rosswood_invoice_no = 'RW-INV-20260102-001'),
       i.id,
       iu.id,
       55.00,
       11.50,
       632.50,
       20.00,
       126.50,
       759.00,
       'PB-20260102-001',
       'admin', NOW()
FROM item i
         JOIN item_uom iu ON iu.item_id = i.id AND iu.is_base = true
WHERE i.item_code = 'FG002';

-- ============================================================
-- 18. STOCK TRANSACTION — Sales OUT for the invoice above
-- ============================================================
INSERT INTO stock_transaction (
    id, transaction_date, item_id, direction, transaction_type,
    quantity, uom_code, batch_code, expiry_date,
    production_batch_id, sales_invoice_id, reference_no, remarks, created_by, created_at
)
SELECT nextval('stock_transaction_seq'),
       '2026-01-02',
       (SELECT id FROM item WHERE item_code = 'FG002'),
       'OUT', 'SALE',
       55.00, 'PCS',
       'PB-20260102-001', '2026-01-23',
       (SELECT id FROM production_batch WHERE batch_code = 'PB-20260102-001'),
       (SELECT id FROM sales_invoice WHERE rosswood_invoice_no = 'RW-INV-20260102-001'),
       'RW-INV-20260102-001', NULL, 'admin', NOW();