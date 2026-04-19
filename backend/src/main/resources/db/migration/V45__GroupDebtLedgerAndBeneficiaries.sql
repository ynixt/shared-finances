CREATE TABLE wallet_event_beneficiary (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    wallet_event_id UUID NOT NULL REFERENCES wallet_event(id) ON DELETE CASCADE,
    beneficiary_user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    benefit_percent NUMERIC(5,2) NOT NULL,
    CONSTRAINT chk_wallet_event_beneficiary_percent
        CHECK (benefit_percent >= 0 AND benefit_percent <= 100)
);

CREATE UNIQUE INDEX uq_wallet_event_beneficiary_event_user
    ON wallet_event_beneficiary(wallet_event_id, beneficiary_user_id);

CREATE INDEX idx_wallet_event_beneficiary_event
    ON wallet_event_beneficiary(wallet_event_id);

CREATE INDEX idx_wallet_event_beneficiary_user
    ON wallet_event_beneficiary(beneficiary_user_id);

CREATE TABLE recurrence_event_beneficiary (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    wallet_event_id UUID NOT NULL REFERENCES recurrence_event(id) ON DELETE CASCADE,
    beneficiary_user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    benefit_percent NUMERIC(5,2) NOT NULL,
    CONSTRAINT chk_recurrence_event_beneficiary_percent
        CHECK (benefit_percent >= 0 AND benefit_percent <= 100)
);

CREATE UNIQUE INDEX uq_recurrence_event_beneficiary_event_user
    ON recurrence_event_beneficiary(wallet_event_id, beneficiary_user_id);

CREATE INDEX idx_recurrence_event_beneficiary_event
    ON recurrence_event_beneficiary(wallet_event_id);

CREATE INDEX idx_recurrence_event_beneficiary_user
    ON recurrence_event_beneficiary(beneficiary_user_id);

CREATE TABLE group_member_debt_monthly (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    group_id UUID NOT NULL REFERENCES "group"(id) ON DELETE CASCADE,
    payer_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    month DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(12,2) NOT NULL,
    CONSTRAINT chk_group_member_debt_monthly_pair CHECK (payer_id <> receiver_id),
    CONSTRAINT uq_group_member_debt_monthly_scope UNIQUE (group_id, payer_id, receiver_id, month, currency)
);

CREATE INDEX idx_group_member_debt_monthly_group_pair
    ON group_member_debt_monthly(group_id, payer_id, receiver_id, currency);

CREATE INDEX idx_group_member_debt_monthly_group_month
    ON group_member_debt_monthly(group_id, month, currency);

CREATE TABLE group_member_debt_movement (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    group_id UUID NOT NULL REFERENCES "group"(id) ON DELETE CASCADE,
    payer_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    month DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    delta_signed NUMERIC(12,2) NOT NULL,
    reason_kind TEXT NOT NULL,
    created_by_user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    note TEXT,
    source_wallet_event_id UUID REFERENCES wallet_event(id) ON DELETE CASCADE,
    source_movement_id UUID REFERENCES group_member_debt_movement(id) ON DELETE SET NULL,
    CONSTRAINT chk_group_member_debt_movement_pair CHECK (payer_id <> receiver_id)
);

CREATE INDEX idx_group_member_debt_movement_group_created
    ON group_member_debt_movement(group_id, created_at DESC, id DESC);

CREATE INDEX idx_group_member_debt_movement_group_pair_month
    ON group_member_debt_movement(group_id, payer_id, receiver_id, month, currency);

CREATE INDEX idx_group_member_debt_movement_source_wallet_event
    ON group_member_debt_movement(source_wallet_event_id)
    WHERE source_wallet_event_id IS NOT NULL;

CREATE INDEX idx_group_member_debt_movement_source_movement
    ON group_member_debt_movement(source_movement_id)
    WHERE source_movement_id IS NOT NULL;
