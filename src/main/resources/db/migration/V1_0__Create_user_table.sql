create table if not exists "user"
(
    id               varchar
        constraint user_pk primary key,
    email            varchar                  not null unique,
    creation_instant timestamp with time zone not null default now()
);
