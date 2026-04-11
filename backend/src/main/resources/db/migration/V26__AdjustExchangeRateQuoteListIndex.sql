DROP INDEX IF EXISTS idx_exchange_rate_quote_list_cursor;
DROP INDEX IF EXISTS idx_exchange_rate_quote_pair_date;

CREATE INDEX idx_exchange_rate_quote_list_cursor
    ON exchange_rate_quote (
        quote_date DESC,
        base_currency ASC,
        quote_currency ASC,
        quoted_at DESC,
        id DESC
    );

CREATE INDEX idx_exchange_rate_quote_pair_list_cursor
    ON exchange_rate_quote (
        base_currency ASC,
        quote_currency ASC,
        quote_date DESC,
        quoted_at DESC,
        id DESC
    );
