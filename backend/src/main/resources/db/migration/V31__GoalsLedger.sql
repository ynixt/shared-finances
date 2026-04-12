CREATE TABLE financial_goal (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    name CITEXT NOT NULL CHECK (char_length(name) <= 255),
    description TEXT,
    user_id UUID REFERENCES "users"(id) ON DELETE CASCADE,
    group_id UUID REFERENCES "group"(id) ON DELETE CASCADE,
    deadline DATE,
    CONSTRAINT chk_financial_goal_scope CHECK (
        (user_id IS NOT NULL AND group_id IS NULL)
        OR (user_id IS NULL AND group_id IS NOT NULL)
    )
);

CREATE INDEX idx_financial_goal_user_id ON financial_goal(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_financial_goal_group_id ON financial_goal(group_id) WHERE group_id IS NOT NULL;

CREATE TABLE financial_goal_target (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    financial_goal_id UUID NOT NULL REFERENCES financial_goal(id) ON DELETE CASCADE,
    currency VARCHAR(3) NOT NULL,
    target_amount NUMERIC(12,2) NOT NULL,
    CONSTRAINT uq_financial_goal_target_goal_currency UNIQUE (financial_goal_id, currency)
);

CREATE INDEX idx_financial_goal_target_goal_id ON financial_goal_target(financial_goal_id);

CREATE TABLE financial_goal_contribution_schedule (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    financial_goal_id UUID NOT NULL REFERENCES financial_goal(id) ON DELETE CASCADE,
    wallet_item_id UUID NOT NULL REFERENCES wallet_item(id) ON DELETE CASCADE,
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    periodicity TEXT NOT NULL,
    qty_executed INT NOT NULL,
    qty_limit INT,
    last_execution DATE,
    next_execution DATE,
    end_execution DATE
);

CREATE INDEX idx_fin_goal_sched_goal ON financial_goal_contribution_schedule(financial_goal_id);
CREATE INDEX idx_fin_goal_sched_wallet ON financial_goal_contribution_schedule(wallet_item_id);
CREATE INDEX idx_fin_goal_sched_next_exec ON financial_goal_contribution_schedule(next_execution)
    WHERE next_execution IS NOT NULL;

CREATE TABLE financial_goal_ledger_movement (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    financial_goal_id UUID NOT NULL REFERENCES financial_goal(id) ON DELETE CASCADE,
    wallet_item_id UUID NOT NULL REFERENCES wallet_item(id) ON DELETE CASCADE,
    signed_amount NUMERIC(12,2) NOT NULL,
    note TEXT,
    movement_kind TEXT NOT NULL,
    schedule_id UUID REFERENCES financial_goal_contribution_schedule(id) ON DELETE SET NULL
);

CREATE INDEX idx_fin_goal_ledger_goal ON financial_goal_ledger_movement(financial_goal_id);
CREATE INDEX idx_fin_goal_ledger_wallet ON financial_goal_ledger_movement(wallet_item_id);
CREATE INDEX idx_fin_goal_ledger_goal_wallet ON financial_goal_ledger_movement(financial_goal_id, wallet_item_id);
CREATE INDEX idx_fin_goal_ledger_schedule ON financial_goal_ledger_movement(schedule_id)
    WHERE schedule_id IS NOT NULL;
