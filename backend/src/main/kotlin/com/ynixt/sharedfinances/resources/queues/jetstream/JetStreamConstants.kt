package com.ynixt.sharedfinances.resources.queues.jetstream

object JetStreamConstants {
    const val ENTRY_RECURRENCE_STREAM = "ENTRY_RECURRENCE_STREAM"
    const val GENERATE_ENTRY_RECURRENCE_QUEUE = "generate-entry-recurrence"

    const val ENTRY_RECURRENCE_DLQ_STREAM = "ENTRY_RECURRENCE_DLQ_STREAM"
    const val GENERATE_ENTRY_RECURRENCE_DLQ_QUEUE = "generate-entry-recurrence-dlq"

    const val SIMULATION_JOB_DISPATCH_STREAM = "SF_JOB_DISPATCH"
    const val SIMULATION_JOB_DISPATCH_SUBJECT = "sf.jobs.dispatch"
    const val SIMULATION_JOB_WORKER_CONSUMER = "sf-job-workers"

    const val IMPORT_JOB_DISPATCH_STREAM = "SF_IMPORT_DISPATCH"
    const val IMPORT_JOB_DISPATCH_SUBJECT = "sf.imports.dispatch"
    const val IMPORT_JOB_WORKER_CONSUMER = "sf-import-workers"

    const val EXPORT_JOB_DISPATCH_STREAM = "SF_EXPORT_DISPATCH"
    const val EXPORT_JOB_DISPATCH_SUBJECT = "sf.exports.dispatch"
    const val EXPORT_JOB_WORKER_CONSUMER = "sf-export-workers"
}
