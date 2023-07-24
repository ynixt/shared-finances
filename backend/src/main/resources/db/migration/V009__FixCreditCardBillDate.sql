alter table credit_card_bill_date
    drop column bill_date;

alter table "credit_card_bill_date"
    add column bill_date date not null;