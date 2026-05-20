create table repository_cache (
    cache_object_id uuid not null,
    cache_object_path text not null,
    cache_object_size bigint,
    cache_object_hash text,
    cache_object_mime_type text,
    repository_type text not null,
    repository_name text not null,
    url_path text not null,
    cached boolean not null default false,
    cached_at timestamp,
    constraint pk_repository_cache primary key(cache_object_id),
    constraint uk_repository_cache_01 unique(repository_type, repository_name, url_path)
);

create index ix_repository_cache_01 on repository_cache(repository_type, repository_name, cached_at);

create table repository_cache_lock (
    cache_object_id uuid not null,
    lock_id uuid not null,
    lock_type text not null,
    locked_at timestamp not null,
    constraint pk_repository_cache_lock primary key(cache_object_id, lock_id),
    constraint fk_repository_cache_lock_repository_cache foreign key(cache_object_id) references repository_cache(cache_object_id),
    constraint ck_repository_cache_lock_lock_type check(lock_type in ('READ','WRITE','DELETE'))
);