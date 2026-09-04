-- V10: Remove the Item Dispatcher feature entirely. GRN (V9) has replaced it.
-- Dropping item_dispatches also drops its idx_dispatch_branch_status index.

drop table if exists dispatch_payment_requests;
drop table if exists item_dispatch_lines;
drop table if exists item_dispatches;

alter table product_serials
    drop column if exists dispatch_id;
