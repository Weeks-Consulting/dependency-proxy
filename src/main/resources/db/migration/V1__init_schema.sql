create table repository_cache (
    repository_type text not null,
    repository_name text not null,
    url_path text not null,
    url_params text,
    mime_type text,
    cache_object_path text not null,
    cache_object_id uuid not null,
    cache_object_size bigint,
    cache_object_hash text,
    is_cached boolean not null default false,
    inserted_at timestamp not null default current_timestamp,
    updated_at timestamp,
    deleted_at timestamp,
    primary key(repository_type, repository_name, url_path, url_params),
    unique(cache_object_id)
);

create index ix_repository_cache_01 on repository_cache(repository_type, repository_name, inserted_at);