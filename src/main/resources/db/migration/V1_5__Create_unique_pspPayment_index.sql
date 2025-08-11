alter table payment
    drop constraint unique_ab;

alter table payment
    add constraint unique_pspPayment unique (psp_type, psp_payment_id);