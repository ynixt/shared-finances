CREATE TEMP TABLE tmp_manual_adjustment_flatten AS
SELECT
    root.id AS root_id,
    COALESCE(SUM(chain_movement.delta_signed), 0) AS net_delta,
    COALESCE(
        (
            SELECT NULLIF(BTRIM(note_movement.note), '')
            FROM group_member_debt_movement note_movement
            WHERE
                (note_movement.id = root.id OR note_movement.source_movement_id = root.id)
                AND NULLIF(BTRIM(note_movement.note), '') IS NOT NULL
            ORDER BY note_movement.created_at DESC, note_movement.id DESC
            LIMIT 1
        ),
        NULLIF(BTRIM(root.note), '')
    ) AS resolved_note
FROM group_member_debt_movement root
JOIN group_member_debt_movement chain_movement
    ON chain_movement.id = root.id OR chain_movement.source_movement_id = root.id
WHERE root.reason_kind = 'MANUAL_ADJUSTMENT'
GROUP BY root.id, root.note;

DELETE FROM group_member_debt_movement root
USING tmp_manual_adjustment_flatten flattened
WHERE
    root.id = flattened.root_id
    AND flattened.net_delta = 0;

UPDATE group_member_debt_movement root
SET
    delta_signed = flattened.net_delta,
    note = flattened.resolved_note,
    updated_at = CURRENT_TIMESTAMP
FROM tmp_manual_adjustment_flatten flattened
WHERE
    root.id = flattened.root_id
    AND flattened.net_delta <> 0;

DELETE FROM group_member_debt_movement
WHERE reason_kind = 'MANUAL_ADJUSTMENT_COMPENSATION';

TRUNCATE TABLE group_member_debt_monthly;

INSERT INTO group_member_debt_monthly (
    id,
    group_id,
    payer_id,
    receiver_id,
    month,
    currency,
    balance
)
SELECT
    gen_random_uuid(),
    group_id,
    payer_id,
    receiver_id,
    month,
    currency,
    SUM(delta_signed) AS balance
FROM group_member_debt_movement
GROUP BY group_id, payer_id, receiver_id, month, currency
HAVING SUM(delta_signed) <> 0;

DROP TABLE tmp_manual_adjustment_flatten;
