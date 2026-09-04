-- V11: Remove the STORE_KEEPER role. Former store keepers become cashiers, who
-- now have full inventory access.

update user_profiles
set role = 'CASHIER'
where role = 'STORE_KEEPER';

-- Any active secondary-role assignments to the removed role fall back to cashier.
update secondary_role_assignments
set secondary_role = 'CASHIER'
where secondary_role = 'STORE_KEEPER';

-- Rebuild the role CHECK constraint without STORE_KEEPER. The V1 constraint was
-- created inline, so PostgreSQL named it user_profiles_role_check by default.
alter table user_profiles
    drop constraint if exists user_profiles_role_check;

alter table user_profiles
    add constraint user_profiles_role_check
        check (role in
               ('SUPER_ADMIN', 'MANAGER', 'SUPERVISOR', 'CASHIER',
                'DTV_TECHNICIAN', 'MOBILE_TECHNICIAN'));
