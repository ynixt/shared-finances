CREATE TABLE wallet_category_concept (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    kind TEXT NOT NULL,
    code TEXT,
    display_name CITEXT,
    CONSTRAINT chk_wallet_category_concept_kind
        CHECK (kind IN ('PREDEFINED', 'CUSTOM')),
    CONSTRAINT chk_wallet_category_concept_payload
        CHECK (
            (kind = 'PREDEFINED' AND code IS NOT NULL AND display_name IS NULL)
            OR
            (kind = 'CUSTOM' AND code IS NULL AND display_name IS NOT NULL AND char_length(display_name) <= 255)
        )
);

CREATE UNIQUE INDEX uq_wallet_category_concept_code
    ON wallet_category_concept(code)
    WHERE code IS NOT NULL;

CREATE INDEX idx_wallet_category_concept_kind
    ON wallet_category_concept(kind);

INSERT INTO wallet_category_concept (id, kind, code, display_name)
SELECT
    gen_random_uuid(),
    'PREDEFINED',
    concept_code,
    NULL
FROM (
    VALUES
        ('BEAUTY_CARE'),
        ('CHARITY'),
        ('CHILDCARE'),
        ('CLOTHING'),
        ('CREDIT_CARD_PAYMENT'),
        ('CRYPTOCURRENCY'),
        ('DEBT_SF'),
        ('DINING_OUT'),
        ('DIVIDENDS'),
        ('DONATIONS'),
        ('EDUCATION'),
        ('ELECTRONICS'),
        ('ENTERTAINMENT'),
        ('FAMILY'),
        ('FEES'),
        ('FINES_PENALTIES'),
        ('FURNITURE'),
        ('GAMES'),
        ('GIFTS_DONATIONS'),
        ('GOVERNMENT_BENEFITS'),
        ('HEALTH'),
        ('HOME'),
        ('HOUSING_RENT'),
        ('INSURANCE'),
        ('INVESTMENTS'),
        ('MAINTENANCE_REPAIRS'),
        ('PERSONAL'),
        ('PETS'),
        ('PHARMACY'),
        ('PROFIT_BONUS'),
        ('PROFIT_SHARING'),
        ('REFUND'),
        ('SAVINGS'),
        ('SERVICES'),
        ('SUBSCRIPTIONS'),
        ('SUPERMARKET'),
        ('TAXES'),
        ('TRANSPORT'),
        ('TRAVEL'),
        ('UTILITIES'),
        ('VEHICLE_FINES_PENALTIES'),
        ('VEHICLE'),
        ('WAGE'),
        ('WELL_BEING')
) AS seeded(concept_code);

ALTER TABLE wallet_entry_category
    ADD COLUMN concept_id UUID,
    ADD CONSTRAINT fk_wallet_entry_category_concept
        FOREIGN KEY (concept_id) REFERENCES wallet_category_concept(id);

ALTER TABLE wallet_entry_category
    ALTER COLUMN concept_id SET NOT NULL;

CREATE INDEX idx_wallet_entry_category_concept_id
    ON wallet_entry_category(concept_id);

CREATE UNIQUE INDEX idx_wallet_entry_category_user_id_concept_id
    ON wallet_entry_category(user_id, concept_id)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX idx_wallet_entry_category_group_id_concept_id
    ON wallet_entry_category(group_id, concept_id)
    WHERE group_id IS NOT NULL;

CREATE INDEX idx_wallet_event_category_id
    ON wallet_event(category_id)
    WHERE category_id IS NOT NULL;

CREATE INDEX idx_recurrence_event_category_id
    ON recurrence_event(category_id)
    WHERE category_id IS NOT NULL;
