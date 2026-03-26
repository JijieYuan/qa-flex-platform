select count(*) as table_count
from information_schema.tables
where table_schema='public' and table_type='BASE TABLE';
