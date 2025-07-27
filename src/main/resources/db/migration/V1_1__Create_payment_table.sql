create table if not exists payment
(
    id                            varchar
        constraint payment_pk primary key,

    user_id                       varchar                  not null
        constraint user_id_fk
            references "user" (id),
    psp_type                      varchar                  not null,
    psp_payment_id                varchar                  not null,
    -- Following unicity constraint is CRITICAL
    constraint unique_ab unique (user_id, psp_type, psp_payment_id),

    psp_creation_instant          timestamp with time zone,
    amount                        integer,

    creation_instant              timestamp with time zone not null,
    last_psp_verification_instant timestamp with time zone
);
create index if not exists user_id_index on payment (user_id);