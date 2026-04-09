ALTER TABLE recurrence_event
    ADD COLUMN series_id UUID;

ALTER TABLE recurrence_event
    ADD COLUMN series_installment_total INT;

ALTER TABLE recurrence_event
    ADD COLUMN series_installment_offset INT;

UPDATE recurrence_event
SET series_id = id
WHERE series_id IS NULL;

UPDATE recurrence_event
SET series_installment_total = qty_limit
WHERE payment_type = 'INSTALLMENTS'
  AND series_installment_total IS NULL;

UPDATE recurrence_event
SET series_installment_offset = 0
WHERE series_installment_offset IS NULL;

ALTER TABLE recurrence_event
    ALTER COLUMN series_id SET NOT NULL;

ALTER TABLE recurrence_event
    ALTER COLUMN series_installment_offset SET NOT NULL;

ALTER TABLE recurrence_event
    ALTER COLUMN series_installment_offset SET DEFAULT 0;

CREATE INDEX idx_recurrence_event_series_id ON recurrence_event(series_id);
