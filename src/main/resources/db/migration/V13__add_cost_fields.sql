-- Add unit cost to stock movements (purchases, opening stock, and COGS on sales)
ALTER TABLE stock_transaction ADD COLUMN unit_cost DECIMAL(12,4);

-- Add weighted average cost to item master (running WAC balance)
ALTER TABLE item ADD COLUMN average_cost DECIMAL(12,4);

-- Add unit cost to opening stock entries (used to seed WAC)
ALTER TABLE stock_opening ADD COLUMN unit_cost DECIMAL(12,4);
