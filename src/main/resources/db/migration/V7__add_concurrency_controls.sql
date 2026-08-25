-- Concurrency controls for POS at multi-cashier load.
--
-- 1) Optimistic locking on stock rows. Two cashiers deducting the same product row concurrently
--    would otherwise read the same quantity and lose one update (silent oversell). With a version
--    column, the second commit fails and its sale transaction rolls back instead of overselling.
ALTER TABLE stock ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 2) Atomic per-day invoice sequence. The previous generator used max(invoice_no)+1, which lets two
--    concurrent sales pick the same number and collide on the unique index AFTER payment is taken.
--    A counter row per date prefix, incremented atomically, guarantees a unique number per sale.
CREATE TABLE IF NOT EXISTS invoice_counters (
    date_prefix VARCHAR(20) PRIMARY KEY,
    last_seq    INTEGER NOT NULL
);
