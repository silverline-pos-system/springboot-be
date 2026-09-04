-- V12: per-line cost on sale items, so COGS/margin can be computed from the
-- actual batch a line was sold from (batch-level pricing).

alter table sale_items
    add column unit_cost numeric(10, 2);
