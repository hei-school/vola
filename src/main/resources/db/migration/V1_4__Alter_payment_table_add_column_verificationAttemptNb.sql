alter table payment
    add column verification_attempt_nb int not null default 0;