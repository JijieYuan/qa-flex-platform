select count(*) as table_count,
       coalesce(sum(case when c.reltuples > 0 then c.reltuples else 0 end)::bigint,0) as estimated_rows,
       count(*) filter (where c.reltuples > 0) as non_empty_estimate
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where n.nspname='public' and c.relkind='r';
