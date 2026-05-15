CREATE OR REPLACE FUNCTION sf_uuid_from_seed(seed TEXT)
RETURNS UUID
LANGUAGE SQL
IMMUTABLE
AS $$
    SELECT (
        SUBSTRING(md5(seed), 1, 8) || '-' ||
        SUBSTRING(md5(seed), 9, 4) || '-' ||
        SUBSTRING(md5(seed), 13, 4) || '-' ||
        SUBSTRING(md5(seed), 17, 4) || '-' ||
        SUBSTRING(md5(seed), 21, 12)
    )::uuid
$$;

DO $$
DECLARE
    movement RECORD;
    open_balance RECORD;
    mapped_fragment RECORD;
    remaining NUMERIC(12, 2);
    allocation NUMERIC(12, 2);
    fragment_index INTEGER;
    fragment_id UUID;
    corrected_month DATE;
    invalid_credit_card_event_id UUID;
    inconsistent_credit_card_event_id UUID;
BEGIN
    SELECT invalid.wallet_event_id
    INTO invalid_credit_card_event_id
    FROM (
        SELECT DISTINCT m.source_wallet_event_id AS wallet_event_id
        FROM group_member_debt_movement m
        JOIN wallet_entry ent
            ON ent.wallet_event_id = m.source_wallet_event_id
        JOIN wallet_item wi
            ON wi.id = ent.wallet_item_id
        LEFT JOIN credit_card_bill ccb
            ON ccb.id = ent.bill_id
        WHERE
            m.reason_kind = 'BENEFICIARY_CHARGE'
            AND m.source_wallet_event_id IS NOT NULL
            AND wi.type = 'CREDIT_CARD'
            AND ccb.id IS NULL
    ) invalid
    LIMIT 1;

    IF invalid_credit_card_event_id IS NOT NULL THEN
        RAISE EXCEPTION 'Failed to rebuild debt charge months, credit card wallet event % has no linked bill',
            invalid_credit_card_event_id;
    END IF;

    SELECT inconsistent.wallet_event_id
    INTO inconsistent_credit_card_event_id
    FROM (
        SELECT m.source_wallet_event_id AS wallet_event_id
        FROM group_member_debt_movement m
        JOIN wallet_entry ent
            ON ent.wallet_event_id = m.source_wallet_event_id
        JOIN wallet_item wi
            ON wi.id = ent.wallet_item_id
        JOIN credit_card_bill ccb
            ON ccb.id = ent.bill_id
        WHERE
            m.reason_kind = 'BENEFICIARY_CHARGE'
            AND m.source_wallet_event_id IS NOT NULL
            AND wi.type = 'CREDIT_CARD'
        GROUP BY m.source_wallet_event_id
        HAVING COUNT(DISTINCT DATE_TRUNC('month', ccb.bill_date)::date) > 1
    ) inconsistent
    LIMIT 1;

    IF inconsistent_credit_card_event_id IS NOT NULL THEN
        RAISE EXCEPTION 'Failed to rebuild debt charge months, credit card wallet event % spans more than one bill month',
            inconsistent_credit_card_event_id;
    END IF;

    CREATE TEMP TABLE tmp_group_debt_event_bill_month ON COMMIT DROP AS
    SELECT
        m.source_wallet_event_id AS wallet_event_id,
        MIN(DATE_TRUNC('month', ccb.bill_date)::date) AS bill_month
    FROM group_member_debt_movement m
    JOIN wallet_entry ent
        ON ent.wallet_event_id = m.source_wallet_event_id
    JOIN wallet_item wi
        ON wi.id = ent.wallet_item_id
    JOIN credit_card_bill ccb
        ON ccb.id = ent.bill_id
    WHERE
        m.reason_kind = 'BENEFICIARY_CHARGE'
        AND m.source_wallet_event_id IS NOT NULL
        AND wi.type = 'CREDIT_CARD'
    GROUP BY m.source_wallet_event_id;

    CREATE TEMP TABLE tmp_group_debt_rebuilt_month_by_source_movement (
        source_movement_id UUID PRIMARY KEY,
        rebuilt_month DATE NOT NULL
    ) ON COMMIT DROP;

    CREATE TEMP TABLE tmp_group_debt_movement_rebuild (
        id UUID PRIMARY KEY,
        created_at TIMESTAMPTZ NOT NULL,
        updated_at TIMESTAMPTZ,
        group_id UUID NOT NULL,
        payer_id UUID NOT NULL,
        receiver_id UUID NOT NULL,
        month DATE NOT NULL,
        currency VARCHAR(3) NOT NULL,
        delta_signed NUMERIC(12, 2) NOT NULL,
        reason_kind TEXT NOT NULL,
        created_by_user_id UUID NOT NULL,
        note TEXT,
        source_wallet_event_id UUID,
        source_movement_id UUID
    ) ON COMMIT DROP;

    CREATE TEMP TABLE tmp_group_debt_monthly_state (
        group_id UUID NOT NULL,
        payer_id UUID NOT NULL,
        receiver_id UUID NOT NULL,
        month DATE NOT NULL,
        currency VARCHAR(3) NOT NULL,
        balance NUMERIC(12, 2) NOT NULL,
        PRIMARY KEY (group_id, payer_id, receiver_id, month, currency)
    ) ON COMMIT DROP;

    CREATE TEMP TABLE tmp_group_debt_settlement_fragment_map (
        original_movement_id UUID NOT NULL,
        fragment_seq INTEGER NOT NULL,
        fragment_movement_id UUID NOT NULL,
        month DATE NOT NULL,
        delta_signed NUMERIC(12, 2) NOT NULL,
        PRIMARY KEY (original_movement_id, fragment_seq)
    ) ON COMMIT DROP;

    FOR movement IN
        SELECT *
        FROM group_member_debt_movement
        ORDER BY created_at ASC NULLS FIRST, id ASC
    LOOP
        corrected_month := movement.month;

        IF movement.reason_kind = 'BENEFICIARY_CHARGE' AND movement.source_wallet_event_id IS NOT NULL THEN
            SELECT mapped.bill_month
            INTO corrected_month
            FROM tmp_group_debt_event_bill_month mapped
            WHERE mapped.wallet_event_id = movement.source_wallet_event_id;

            corrected_month := COALESCE(corrected_month, movement.month);
        ELSIF movement.reason_kind = 'BENEFICIARY_REVERSAL' AND movement.source_movement_id IS NOT NULL THEN
            SELECT mapped.rebuilt_month
            INTO corrected_month
            FROM tmp_group_debt_rebuilt_month_by_source_movement mapped
            WHERE mapped.source_movement_id = movement.source_movement_id;

            corrected_month := COALESCE(corrected_month, movement.month);
        END IF;

        IF movement.reason_kind = 'DEBT_SETTLEMENT' THEN
            remaining := ROUND(ABS(movement.delta_signed), 2);
            fragment_index := 0;

            FOR open_balance IN
                SELECT month, balance
                FROM tmp_group_debt_monthly_state
                WHERE
                    group_id = movement.group_id
                    AND payer_id = movement.payer_id
                    AND receiver_id = movement.receiver_id
                    AND currency = UPPER(movement.currency)
                    AND balance > 0
                ORDER BY month ASC
            LOOP
                EXIT WHEN remaining <= 0;

                allocation := ROUND(LEAST(open_balance.balance, remaining), 2);
                IF allocation <= 0 THEN
                    CONTINUE;
                END IF;

                fragment_index := fragment_index + 1;
                fragment_id :=
                    CASE
                        WHEN fragment_index = 1 THEN movement.id
                        ELSE sf_uuid_from_seed(movement.id::text || ':settlement:' || fragment_index::text)
                    END;

                INSERT INTO tmp_group_debt_movement_rebuild(
                    id,
                    created_at,
                    updated_at,
                    group_id,
                    payer_id,
                    receiver_id,
                    month,
                    currency,
                    delta_signed,
                    reason_kind,
                    created_by_user_id,
                    note,
                    source_wallet_event_id,
                    source_movement_id
                ) VALUES (
                    fragment_id,
                    movement.created_at,
                    movement.updated_at,
                    movement.group_id,
                    movement.payer_id,
                    movement.receiver_id,
                    open_balance.month,
                    UPPER(movement.currency),
                    ROUND(-allocation, 2),
                    movement.reason_kind,
                    movement.created_by_user_id,
                    movement.note,
                    movement.source_wallet_event_id,
                    NULL
                );

                INSERT INTO tmp_group_debt_settlement_fragment_map(
                    original_movement_id,
                    fragment_seq,
                    fragment_movement_id,
                    month,
                    delta_signed
                ) VALUES (
                    movement.id,
                    fragment_index,
                    fragment_id,
                    open_balance.month,
                    ROUND(-allocation, 2)
                );

                UPDATE tmp_group_debt_monthly_state
                SET balance = ROUND(balance - allocation, 2)
                WHERE
                    group_id = movement.group_id
                    AND payer_id = movement.payer_id
                    AND receiver_id = movement.receiver_id
                    AND month = open_balance.month
                    AND currency = UPPER(movement.currency);

                remaining := ROUND(remaining - allocation, 2);
            END LOOP;

            IF remaining > 0 THEN
                RAISE EXCEPTION 'Failed to rebuild debt settlement %, remaining amount % exceeds open debt',
                    movement.id,
                    remaining;
            END IF;
        ELSIF movement.reason_kind = 'DEBT_SETTLEMENT_REVERSAL' THEN
            fragment_index := 0;

            FOR mapped_fragment IN
                SELECT *
                FROM tmp_group_debt_settlement_fragment_map
                WHERE original_movement_id = movement.source_movement_id
                ORDER BY fragment_seq ASC
            LOOP
                fragment_index := fragment_index + 1;
                fragment_id :=
                    CASE
                        WHEN fragment_index = 1 THEN movement.id
                        ELSE sf_uuid_from_seed(movement.id::text || ':reversal:' || fragment_index::text)
                    END;

                INSERT INTO tmp_group_debt_movement_rebuild(
                    id,
                    created_at,
                    updated_at,
                    group_id,
                    payer_id,
                    receiver_id,
                    month,
                    currency,
                    delta_signed,
                    reason_kind,
                    created_by_user_id,
                    note,
                    source_wallet_event_id,
                    source_movement_id
                ) VALUES (
                    fragment_id,
                    movement.created_at,
                    movement.updated_at,
                    movement.group_id,
                    movement.payer_id,
                    movement.receiver_id,
                    mapped_fragment.month,
                    UPPER(movement.currency),
                    ROUND(mapped_fragment.delta_signed * -1, 2),
                    movement.reason_kind,
                    movement.created_by_user_id,
                    movement.note,
                    movement.source_wallet_event_id,
                    mapped_fragment.fragment_movement_id
                );

                INSERT INTO tmp_group_debt_monthly_state(
                    group_id,
                    payer_id,
                    receiver_id,
                    month,
                    currency,
                    balance
                ) VALUES (
                    movement.group_id,
                    movement.payer_id,
                    movement.receiver_id,
                    mapped_fragment.month,
                    UPPER(movement.currency),
                    ROUND(mapped_fragment.delta_signed * -1, 2)
                )
                ON CONFLICT (group_id, payer_id, receiver_id, month, currency)
                DO UPDATE SET
                    balance = ROUND(tmp_group_debt_monthly_state.balance + EXCLUDED.balance, 2);
            END LOOP;

            IF fragment_index = 0 THEN
                RAISE EXCEPTION 'Failed to rebuild debt settlement reversal %, missing original settlement fragments for %',
                    movement.id,
                    movement.source_movement_id;
            END IF;
        ELSE
            INSERT INTO tmp_group_debt_movement_rebuild(
                id,
                created_at,
                updated_at,
                group_id,
                payer_id,
                receiver_id,
                month,
                currency,
                delta_signed,
                reason_kind,
                created_by_user_id,
                note,
                source_wallet_event_id,
                source_movement_id
            ) VALUES (
                movement.id,
                movement.created_at,
                movement.updated_at,
                movement.group_id,
                movement.payer_id,
                movement.receiver_id,
                corrected_month,
                UPPER(movement.currency),
                movement.delta_signed,
                movement.reason_kind,
                movement.created_by_user_id,
                movement.note,
                movement.source_wallet_event_id,
                movement.source_movement_id
            );

            INSERT INTO tmp_group_debt_rebuilt_month_by_source_movement(
                source_movement_id,
                rebuilt_month
            ) VALUES (
                movement.id,
                corrected_month
            )
            ON CONFLICT (source_movement_id)
            DO UPDATE SET
                rebuilt_month = EXCLUDED.rebuilt_month;

            INSERT INTO tmp_group_debt_monthly_state(
                group_id,
                payer_id,
                receiver_id,
                month,
                currency,
                balance
            ) VALUES (
                movement.group_id,
                movement.payer_id,
                movement.receiver_id,
                corrected_month,
                UPPER(movement.currency),
                movement.delta_signed
            )
            ON CONFLICT (group_id, payer_id, receiver_id, month, currency)
            DO UPDATE SET
                balance = ROUND(tmp_group_debt_monthly_state.balance + EXCLUDED.balance, 2);
        END IF;
    END LOOP;

    CREATE TEMP TABLE tmp_group_debt_monthly_expected ON COMMIT DROP AS
    SELECT
        group_id,
        payer_id,
        receiver_id,
        month,
        currency,
        ROUND(SUM(delta_signed), 2) AS balance,
        MIN(created_at) AS created_at,
        MAX(COALESCE(updated_at, created_at)) AS updated_at
    FROM tmp_group_debt_movement_rebuild
    GROUP BY group_id, payer_id, receiver_id, month, currency
    HAVING ROUND(SUM(delta_signed), 2) <> 0;

    IF EXISTS (
        SELECT 1
        FROM (
            SELECT group_id, payer_id, receiver_id, month, currency, balance
            FROM tmp_group_debt_monthly_state
            WHERE balance <> 0
            EXCEPT
            SELECT group_id, payer_id, receiver_id, month, currency, balance
            FROM tmp_group_debt_monthly_expected
        ) mismatch
    ) OR EXISTS (
        SELECT 1
        FROM (
            SELECT group_id, payer_id, receiver_id, month, currency, balance
            FROM tmp_group_debt_monthly_expected
            EXCEPT
            SELECT group_id, payer_id, receiver_id, month, currency, balance
            FROM tmp_group_debt_monthly_state
            WHERE balance <> 0
        ) mismatch
    ) THEN
        RAISE EXCEPTION 'Rebuilt debt monthly snapshot does not reconcile with rebuilt ledger';
    END IF;

    TRUNCATE TABLE group_member_debt_monthly;
    TRUNCATE TABLE group_member_debt_movement;

    INSERT INTO group_member_debt_movement(
        id,
        created_at,
        updated_at,
        group_id,
        payer_id,
        receiver_id,
        month,
        currency,
        delta_signed,
        reason_kind,
        created_by_user_id,
        note,
        source_wallet_event_id,
        source_movement_id
    )
    SELECT
        id,
        created_at,
        updated_at,
        group_id,
        payer_id,
        receiver_id,
        month,
        currency,
        delta_signed,
        reason_kind,
        created_by_user_id,
        note,
        source_wallet_event_id,
        source_movement_id
    FROM tmp_group_debt_movement_rebuild
    ORDER BY created_at ASC NULLS FIRST, id ASC;

    INSERT INTO group_member_debt_monthly(
        id,
        created_at,
        updated_at,
        group_id,
        payer_id,
        receiver_id,
        month,
        currency,
        balance
    )
    SELECT
        sf_uuid_from_seed(
            'group_member_debt_monthly:' ||
            group_id::text || ':' ||
            payer_id::text || ':' ||
            receiver_id::text || ':' ||
            month::text || ':' ||
            currency
        ),
        created_at,
        updated_at,
        group_id,
        payer_id,
        receiver_id,
        month,
        currency,
        balance
    FROM tmp_group_debt_monthly_expected
    ORDER BY group_id, payer_id, receiver_id, month, currency;
END $$;

DROP FUNCTION sf_uuid_from_seed(TEXT);
