begin;

-- Local demo fixtures for review data management.
-- Keep this script idempotent for repeated local execution.

insert into review_records (
    id,
    project_name,
    title,
    module_name,
    review_type,
    review_date,
    review_owner,
    review_scale_pages,
    review_product,
    author_name,
    review_version,
    deleted,
    created_at,
    updated_at
)
values
    (9001, 'CC2026R3', '[Sketch] Arithmetic design review', 'Sketch', 'Design Review', date '2026-02-10', 'Wang Qing', 24, 'Arithmetic design spec', 'Lu Shikun', 'V1.3', false, timestamp '2026-02-10 09:30:00', timestamp '2026-02-10 09:44:35'),
    (9002, 'CC2026R3', '[Sketch] Spline curvature control review', 'Sketch', 'Design Review', date '2026-02-10', 'Wang Qing', 33, 'Spline design spec', 'Ma Chuanchao', 'V2.0', false, timestamp '2026-02-10 09:55:00', timestamp '2026-02-10 10:16:34'),
    (9003, 'CC2026R3', '[Tools] Speed style feature review', 'Tools', 'Design Review', date '2026-02-09', 'Li Li', 38, 'Speed style design spec', 'Zhang Lei', 'V1.0', false, timestamp '2026-02-09 13:10:00', timestamp '2026-02-09 13:47:02'),
    (9004, 'CC2026R3', '[Platform] External reference cache review', 'Platform', 'Requirement Review', date '2026-02-09', 'Xu Jianmin', 19, 'External reference requirement', 'Xu Jianmin', 'V0.9', false, timestamp '2026-02-09 10:40:00', timestamp '2026-02-09 11:24:20')
on conflict (id) do update
set project_name = excluded.project_name,
    title = excluded.title,
    module_name = excluded.module_name,
    review_type = excluded.review_type,
    review_date = excluded.review_date,
    review_owner = excluded.review_owner,
    review_scale_pages = excluded.review_scale_pages,
    review_product = excluded.review_product,
    author_name = excluded.author_name,
    review_version = excluded.review_version,
    deleted = excluded.deleted,
    created_at = excluded.created_at,
    updated_at = excluded.updated_at;

insert into review_record_experts (
    id,
    review_record_id,
    expert_name,
    sort_order,
    deleted,
    created_at,
    updated_at
)
values
    (9101, 9001, 'Zhang Xiaohan', 1, false, timestamp '2026-02-10 09:35:00', timestamp '2026-02-10 09:35:00'),
    (9102, 9001, 'Wang Qing', 2, false, timestamp '2026-02-10 09:35:00', timestamp '2026-02-10 09:35:00'),
    (9103, 9001, 'Yang Yalun', 3, false, timestamp '2026-02-10 09:35:00', timestamp '2026-02-10 09:35:00'),
    (9104, 9002, 'Yang Yalun', 1, false, timestamp '2026-02-10 10:00:00', timestamp '2026-02-10 10:00:00'),
    (9105, 9002, 'Wu Wei', 2, false, timestamp '2026-02-10 10:00:00', timestamp '2026-02-10 10:00:00'),
    (9106, 9003, 'Wu Wei', 1, false, timestamp '2026-02-09 13:15:00', timestamp '2026-02-09 13:15:00'),
    (9107, 9003, 'Cui Xuefeng', 2, false, timestamp '2026-02-09 13:15:00', timestamp '2026-02-09 13:15:00'),
    (9108, 9004, 'Xu Jianmin', 1, false, timestamp '2026-02-09 10:45:00', timestamp '2026-02-09 10:45:00')
on conflict (id) do update
set review_record_id = excluded.review_record_id,
    expert_name = excluded.expert_name,
    sort_order = excluded.sort_order,
    deleted = excluded.deleted,
    created_at = excluded.created_at,
    updated_at = excluded.updated_at;

insert into review_problem_items (
    id,
    review_record_id,
    reviewer_name,
    workload_hours,
    review_category,
    document_position,
    problem_category,
    problem_description,
    suggested_solution,
    owner_name,
    rejection_reason,
    problem_status,
    deleted,
    created_at,
    updated_at
)
values
    (9201, 9001, 'Zhang Xiaohan', 1.0, 'Meeting Review', '3.3.2', 'Doc Standard', 'Flow description is incomplete and key branches lack input/output notes.', 'Add key node notes and unify arrow direction.', 'Lu Shikun', null, 'Fixed', false, timestamp '2026-02-10 09:50:00', timestamp '2026-02-10 17:00:00'),
    (9202, 9001, 'Wang Qing', 1.0, 'Meeting Review', '3.3.2', 'Functionality', 'Constraint linkage scope is unclear after converting the line type.', 'Add a boundary note and one practical example.', 'Lu Shikun', '', 'Fixed', false, timestamp '2026-02-10 10:10:00', timestamp '2026-02-10 17:30:00'),
    (9203, 9002, 'Yang Yalun', 0.2, 'Meeting Review', '3.5 API', 'Doc Standard', 'Naming is inconsistent between local variables and API text.', 'Unify terms and add abbreviation notes.', 'Ma Chuanchao', '', 'Fixed', false, timestamp '2026-02-10 10:20:00', timestamp '2026-02-10 18:10:00'),
    (9204, 9002, 'Wu Wei', 0.2, 'Meeting Review', '4.2 Constraints', 'Completeness', 'Formula mapping is missing, making conclusions hard to trace.', 'Add variable notes and a small derivation table.', 'Ma Chuanchao', '', 'New', false, timestamp '2026-02-10 10:28:00', timestamp '2026-02-10 10:28:00'),
    (9205, 9003, 'Cui Xuefeng', 0.3, 'Meeting Review', '1.5 Animation', 'Completeness', 'Undo chain after adding the control valve is not described.', 'Describe rollback behavior for the exception path.', 'Zhang Lei', '', 'Fixed', false, timestamp '2026-02-09 14:00:00', timestamp '2026-02-09 18:20:00'),
    (9206, 9003, 'Wu Wei', 0.2, 'Independent Review', '1.4 Prerequisite task', 'Functionality', 'Rotation width limit lacks boundary condition examples.', 'Add a boundary table and an extreme-value example.', 'Zhang Lei', 'Confirmed for later follow-up in the next version.', 'Rejected', false, timestamp '2026-02-09 14:12:00', timestamp '2026-02-10 09:00:00'),
    (9207, 9004, 'Xu Jianmin', 0.5, 'Independent Review', '2.1 Scope', 'Functionality', 'Cache invalidation only covers timed cleanup and misses active refresh.', 'Add active refresh and cache rebuild flow.', 'Xu Jianmin', null, 'New', false, timestamp '2026-02-09 11:10:00', timestamp '2026-02-09 11:10:00')
on conflict (id) do update
set review_record_id = excluded.review_record_id,
    reviewer_name = excluded.reviewer_name,
    workload_hours = excluded.workload_hours,
    review_category = excluded.review_category,
    document_position = excluded.document_position,
    problem_category = excluded.problem_category,
    problem_description = excluded.problem_description,
    suggested_solution = excluded.suggested_solution,
    owner_name = excluded.owner_name,
    rejection_reason = excluded.rejection_reason,
    problem_status = excluded.problem_status,
    deleted = excluded.deleted,
    created_at = excluded.created_at,
    updated_at = excluded.updated_at;

select setval('review_records_id_seq', greatest((select max(id) from review_records), 1), true);
select setval('review_record_experts_id_seq', greatest((select max(id) from review_record_experts), 1), true);
select setval('review_problem_items_id_seq', greatest((select max(id) from review_problem_items), 1), true);

commit;
