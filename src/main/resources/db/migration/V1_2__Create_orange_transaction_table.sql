create table if not exists orange_transaction
(
    ref                     varchar
        constraint orange_transaction_pk primary key,

    orange_api_raw_response varchar not null
);