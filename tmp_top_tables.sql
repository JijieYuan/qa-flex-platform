select relname, reltuples::bigint
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where n.nspname='public' and c.relkind='r'
order by reltuples desc nulls last
limit 20;
