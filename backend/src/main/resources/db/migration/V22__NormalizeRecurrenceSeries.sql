CREATE TABLE recurrence_series (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    qty_total INT
);

INSERT INTO recurrence_series (id, qty_total)
SELECT
    re.series_id,
    COALESCE(
        MAX(
            CASE
                WHEN re.payment_type = 'INSTALLMENTS'
                    THEN COALESCE(
                        re.series_installment_total,
                        re.series_installment_offset + COALESCE(re.qty_limit, re.qty_executed)
                    )
            END
        ),
        MAX(
            CASE
                WHEN re.payment_type = 'RECURRING'
                    AND re.qty_limit IS NOT NULL
                    THEN re.series_installment_offset + re.qty_limit
            END
        ),
        MAX(
            CASE
                WHEN re.payment_type = 'RECURRING'
                    AND re.qty_limit IS NULL
                    THEN re.series_installment_offset + re.qty_executed
            END
        )
    ) AS qty_total
FROM recurrence_event re
GROUP BY re.series_id;

ALTER TABLE recurrence_event
    RENAME COLUMN series_installment_offset TO series_offset;

ALTER TABLE recurrence_event
    DROP COLUMN series_installment_total;

ALTER TABLE recurrence_event
    ADD CONSTRAINT fk_recurrence_event_series
        FOREIGN KEY (series_id) REFERENCES recurrence_series(id) ON DELETE CASCADE;

ALTER TABLE wallet_event
    DROP CONSTRAINT fk_w_entry_recurrence;

ALTER TABLE wallet_event
    ADD CONSTRAINT fk_w_entry_recurrence
        FOREIGN KEY (recurrence_event_id) REFERENCES recurrence_event(id) ON DELETE CASCADE;
