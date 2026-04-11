CREATE INDEX idx_exchange_rate_quote_list_cursor
    ON exchange_rate_quote (quote_date DESC, quoted_at DESC, id DESC);
