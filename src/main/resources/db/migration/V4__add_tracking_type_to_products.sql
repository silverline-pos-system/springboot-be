ALTER TABLE products ADD COLUMN tracking_type VARCHAR(20) DEFAULT 'NORMAL';
UPDATE products SET tracking_type = 'NORMAL' WHERE tracking_type IS NULL;
ALTER TABLE products ALTER COLUMN tracking_type SET NOT NULL;
