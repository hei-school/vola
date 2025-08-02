create table if not exists application
(
    id      varchar
        constraint application_pk primary key,
    name    varchar        not null,
    api_key varchar unique not null
);

alter table payment
    add column application_id varchar
        constraint application_id_fk references application (id);

create index if not exists application_id_index on payment (application_id);