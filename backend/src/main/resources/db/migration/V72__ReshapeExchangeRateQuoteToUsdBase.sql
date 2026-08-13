DROP TABLE IF EXISTS exchange_rate_quote;

CREATE TABLE exchange_rate_quote (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    source TEXT NOT NULL,
    currency TEXT NOT NULL,
    quote_date DATE NOT NULL,
    rate NUMERIC(38, 18) NOT NULL CHECK (rate > 0),
    fetched_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uq_exchange_rate_quote_currency_day
    ON exchange_rate_quote (currency, quote_date);
