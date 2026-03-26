select 'issues' as t, id, title, updated_at from issues where title like 'it-compensation-%' order by id desc limit 3;
select 'notes' as t, id, note, updated_at from notes where note like 'it-compensation-%' order by id desc limit 3;
select 'labels' as t, id, title, updated_at from labels where title like 'it-compensation-%' order by id desc limit 3;
