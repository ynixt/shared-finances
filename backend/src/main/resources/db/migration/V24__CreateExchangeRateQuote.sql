CREATE TABLE exchange_rate_quote (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    source TEXT NOT NULL,
    base_currency TEXT NOT NULL,
    quote_currency TEXT NOT NULL,
    quote_date DATE NOT NULL,
    rate NUMERIC(20, 8) NOT NULL CHECK (rate > 0),
    quoted_at TIMESTAMPTZ NOT NULL,
    fetched_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uq_exchange_rate_quote_pair_day
    ON exchange_rate_quote (base_currency, quote_currency, quote_date);

CREATE INDEX idx_exchange_rate_quote_pair_date
    ON exchange_rate_quote (base_currency, quote_currency, quote_date DESC);

CREATE INDEX idx_exchange_rate_quote_fetched_at
    ON exchange_rate_quote (fetched_at DESC);
