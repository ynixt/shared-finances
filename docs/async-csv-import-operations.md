# Async CSV import operations

CSV imports are persisted as jobs in `import_batch` and move through `QUEUED`, `RUNNING`, `COMPLETED`, or `FAILED`.
Undo uses the same durable queue and moves a completed batch through `UNDO_QUEUED`, `UNDO_RUNNING`, or `UNDO_FAILED`;
on success, the financial effects and batch are removed atomically. HTTP requests only accept the operation, while JetStream
workers apply its financial effects afterwards.

## Gradual activation

1. Deploy the database migration and backend with `SF_APP_IMPORT_WORKER_ENABLED=false`.
2. Confirm that new imports remain `QUEUED`, `GET /imports/{id}` works, and the `import-reconcile` task is registered.
3. Deploy the compatible frontend so queued/running batches are shown as in progress.
4. Enable one backend instance with `SF_APP_IMPORT_WORKER_ENABLED=true`.
5. Watch queue time, processing duration, retry, recovered-lease, and terminal-result metrics before enabling more workers.

The reconciler is controlled independently by `SF_APP_IMPORT_RECONCILE_CRON_ENABLED` and
`SF_APP_IMPORT_RECONCILE_CRON`. Keep it enabled while workers are active so queued jobs whose initial publication failed
and jobs abandoned after a worker crash are dispatched again.

## Monitoring

The worker exposes these Micrometer metrics and never records the financial payload:

- `imports.queue.time`: time between batch persistence and claim;
- `imports.processing.duration`: time spent by a successful worker attempt;
- `imports.retries`: retry/terminal transition count;
- `imports.leases.recovered`: expired leases recovered by the reconciler;
- `imports.terminal`: completed, undone, and failed results for import or undo.

Logs include only the batch ID, attempt count, duration, and result. Alert on sustained queue-time growth, repeated lease
recovery, a failed-result spike, or batches remaining queued longer than two reconciliation intervals.

Useful database checks:

```sql
SELECT status, COUNT(*) FROM import_batch GROUP BY status ORDER BY status;

SELECT id, user_id, created_at, started_at, lease_expires_at, retries
FROM import_batch
WHERE status IN ('QUEUED', 'RUNNING', 'UNDO_QUEUED', 'UNDO_RUNNING')
ORDER BY created_at;
```

Do not include `request_payload` in operational queries, logs, dashboards, or support tickets.

## Rollback

1. Set `SF_APP_IMPORT_WORKER_ENABLED=false` on every instance and wait for current `RUNNING` or `UNDO_RUNNING` transactions to commit or
   roll back. A worker shutdown stops new pulls; expired leases are recovered by reconciliation.
2. Inspect queued/running import and undo batches with the query above. Process them with the asynchronous release before a full
   application rollback, or explicitly mark them `FAILED` through an approved administrative procedure.
3. Only after no active batch remains, deploy the previous synchronous application version. The additive columns and the
   `COMPLETED` default keep legacy inserts compatible.
4. Keep the migration in place. Do not drop lifecycle columns or indexes during an application rollback.

If JetStream is temporarily unavailable, leave accepted batches in `QUEUED`; do not resubmit payloads manually. Restore
JetStream and run/await reconciliation. Re-delivery is safe because terminal batches are ignored, claims are conditional,
and financial rows plus the `COMPLETED` transition commit atomically.
