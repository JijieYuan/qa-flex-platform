begin;

-- Local demo fixtures for review data management.
-- Keep this script idempotent for repeated local execution.

delete from review_problem_items where id between 9201 and 9207;
delete from review_record_experts where id between 9101 and 9108;
delete from review_records where id between 9001 and 9004;

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
    (9001, 'CC2026R3', '[草图模块] 算数功能设计说明书评审', '草图', '设计说明书评审', date '2026-02-10', '王强', 24, '算数功能设计说明书', '路士坤', 'V1.3', false, timestamp '2026-02-10 09:30:00', timestamp '2026-02-10 09:44:35'),
    (9002, 'CC2026R3', '[草图模块] 样条曲线-曲率控制功能设计评审', '草图', '设计说明书评审', date '2026-02-10', '王强', 33, '样条曲线功能设计说明书', '马传超', 'V2.0', false, timestamp '2026-02-10 09:55:00', timestamp '2026-02-10 10:16:34'),
    (9003, 'CC2026R3', '[工具模块] 速度样式功能设计说明书评审', '工具', '设计说明书评审', date '2026-02-09', '李利', 38, '速度样式功能设计说明书', '张磊', 'V1.0', false, timestamp '2026-02-09 13:10:00', timestamp '2026-02-09 13:47:02'),
    (9004, 'CC2026R3', '[平台模块] 外部参考改进（服务端缓存）评审', '平台', '需求说明书评审', date '2026-02-09', '徐建民', 19, '外部参考改进需求说明书', '徐建民', 'V0.9', false, timestamp '2026-02-09 10:40:00', timestamp '2026-02-09 11:24:20')
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
    (9101, 9001, '张晓涵', 1, false, timestamp '2026-02-10 09:35:00', timestamp '2026-02-10 09:35:00'),
    (9102, 9001, '王强', 2, false, timestamp '2026-02-10 09:35:00', timestamp '2026-02-10 09:35:00'),
    (9103, 9001, '杨亚伦', 3, false, timestamp '2026-02-10 09:35:00', timestamp '2026-02-10 09:35:00'),
    (9104, 9002, '杨亚伦', 1, false, timestamp '2026-02-10 10:00:00', timestamp '2026-02-10 10:00:00'),
    (9105, 9002, '武伟', 2, false, timestamp '2026-02-10 10:00:00', timestamp '2026-02-10 10:00:00'),
    (9106, 9003, '武伟', 1, false, timestamp '2026-02-09 13:15:00', timestamp '2026-02-09 13:15:00'),
    (9107, 9003, '崔雪峰', 2, false, timestamp '2026-02-09 13:15:00', timestamp '2026-02-09 13:15:00'),
    (9108, 9004, '徐建民', 1, false, timestamp '2026-02-09 10:45:00', timestamp '2026-02-09 10:45:00')
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
    (9201, 9001, '张晓涵', 1.0, '会议评审', '3.3.2', '文档规范', '样条曲线重新插点的流程图描述不完整，关键分支缺少输入输出说明。', '补充流程图中的关键节点说明，并统一箭头方向和命名方式。', '路士坤', null, '已修复', false, timestamp '2026-02-10 09:50:00', timestamp '2026-02-10 17:00:00'),
    (9202, 9001, '王强', 1.0, '会议评审', '3.3.2', '功能性', '单段线从非参数线变为参考线后，尺寸约束联动范围描述不明确。', '补充“尺寸保留/清除”的边界说明，并增加一段典型例子。', '路士坤', '', '已修复', false, timestamp '2026-02-10 10:10:00', timestamp '2026-02-10 17:30:00'),
    (9203, 9002, '杨亚伦', 0.2, '会议评审', '3.5 算法接口', '文档规范', '命名不规范，局部变量与接口描述中的名词不一致。', '统一接口名和文档描述中的术语，补充缩写说明。', '马传超', '', '已修复', false, timestamp '2026-02-10 10:20:00', timestamp '2026-02-10 18:10:00'),
    (9204, 9002, '武伟', 0.2, '会议评审', '4.2 约束设计', '完整性', '转述结构与原公式之间缺少映射关系，读者难以定位结论来源。', '在公式旁补充变量说明，并给出对应的推导简表。', '马传超', '', '新提出', false, timestamp '2026-02-10 10:28:00', timestamp '2026-02-10 10:28:00'),
    (9205, 9003, '崔雪峰', 0.3, '会议评审', '1.5 动画问题', '完整性', '加上控制阀后无法一步撤销，异常流处理没有说明。', '补充撤销链路说明，并明确异常流下的回退策略。', '张磊', '', '已修复', false, timestamp '2026-02-09 14:00:00', timestamp '2026-02-09 18:20:00'),
    (9206, 9003, '武伟', 0.2, '独立评审', '1.4 前后置任务', '功能性', '旋转显示元宽限制描述不够，边界条件缺少示意。', '增加边界条件表格，并补一张极值示意图。', '张磊', '已确认先按现有实现交付，后续版本再补充极值示意。', '已拒绝', false, timestamp '2026-02-09 14:12:00', timestamp '2026-02-10 09:00:00'),
    (9207, 9004, '徐建民', 0.5, '独立评审', '2.1 范围定义', '功能性', '缓存失效策略只描述了定时清理，没有覆盖主动刷新场景。', '补充主动刷新与失效重建流程，明确缓存穿透的处理策略。', '徐建民', null, '新提出', false, timestamp '2026-02-09 11:10:00', timestamp '2026-02-09 11:10:00')
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
