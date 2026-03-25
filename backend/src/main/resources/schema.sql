create table if not exists gitlab_sync_configs (
    id bigserial primary key,
    name varchar(128) not null default 'default',
    enabled boolean not null default true,
    auto_sync_enabled boolean not null default true,
    source_mode varchar(32) not null default 'DOCKER',
    whitelist_mode varchar(32) not null default 'RECOMMENDED',
    whitelist_tables text,
    db_host varchar(255) not null default 'localhost',
    db_port integer not null default 5432,
    db_name varchar(255) not null,
    db_username varchar(255) not null,
    db_password varchar(255) not null,
    docker_container_name varchar(255),
    webhook_secret varchar(255),
    webhook_project_id bigint,
    compensation_interval_minutes integer not null default 10,
    last_full_sync_at timestamp,
    last_incremental_sync_at timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table if not exists gitlab_sync_logs (
    id bigserial primary key,
    config_id bigint not null references gitlab_sync_configs(id) on delete cascade,
    sync_type varchar(32) not null,
    status varchar(32) not null,
    message text,
    whitelist_snapshot text,
    table_count integer not null default 0,
    record_count integer not null default 0,
    started_at timestamp not null default current_timestamp,
    finished_at timestamp
);

create table if not exists gitlab_webhook_events (
    id bigserial primary key,
    config_id bigint references gitlab_sync_configs(id) on delete set null,
    event_type varchar(128),
    project_id bigint,
    object_kind varchar(128),
    payload text not null,
    received_at timestamp not null default current_timestamp,
    processed boolean not null default false
);

create table if not exists gitlab_mirror_records (
    id bigserial primary key,
    config_id bigint not null references gitlab_sync_configs(id) on delete cascade,
    table_name varchar(128) not null,
    record_key varchar(512) not null,
    updated_at_source timestamp,
    row_data jsonb not null,
    synced_at timestamp not null default current_timestamp,
    created_at timestamp not null default current_timestamp,
    unique (config_id, table_name, record_key)
);

create index if not exists idx_gitlab_mirror_records_table on gitlab_mirror_records(config_id, table_name);
create index if not exists idx_gitlab_sync_logs_config on gitlab_sync_logs(config_id, started_at desc);

alter table gitlab_sync_configs add column if not exists source_mode varchar(32) not null default 'DOCKER';
alter table gitlab_sync_configs add column if not exists docker_container_name varchar(255);
